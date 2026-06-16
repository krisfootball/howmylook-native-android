import { createClient } from "npm:@supabase/supabase-js@2.49.1";

type NotifyRequest = {
  postId?: string;
};

type PushDeviceRow = {
  token: string;
  user_id: string;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const fcmServerKey = Deno.env.get("FCM_SERVER_KEY");

    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }
    if (!fcmServerKey) {
      return jsonResponse({ error: "Missing FCM_SERVER_KEY secret." }, 500);
    }

    const body = (await request.json()) as NotifyRequest;
    const postId = body.postId?.trim();
    if (!postId) {
      return jsonResponse({ error: "postId is required." }, 400);
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const { data: post, error: postError } = await supabase
      .from("posts")
      .select("id, user_id, caption, moderation_status, is_active")
      .eq("id", postId)
      .maybeSingle();

    if (postError) {
      return jsonResponse({ error: postError.message }, 500);
    }
    if (!post) {
      return jsonResponse({ error: "Post not found." }, 404);
    }
    if (!post.is_active || post.moderation_status !== "approved") {
      return jsonResponse({ sent: 0, skipped: "Post is not approved yet." });
    }

    const { data: author, error: authorError } = await supabase
      .from("profiles")
      .select("display_name, username")
      .eq("id", post.user_id)
      .maybeSingle();

    if (authorError) {
      return jsonResponse({ error: authorError.message }, 500);
    }

    const authorName = author?.display_name || author?.username || "Someone you follow";
    const occasion = post.caption?.trim() || "Tap to see the fit.";

    const { data: follows, error: followsError } = await supabase
      .from("follows")
      .select("follower_id")
      .eq("following_id", post.user_id)
      .eq("notifications_enabled", true);

    if (followsError) {
      return jsonResponse({ error: followsError.message }, 500);
    }

    const followerIds = (follows ?? []).map((row) => row.follower_id).filter(Boolean);
    if (followerIds.length === 0) {
      return jsonResponse({ sent: 0, skipped: "No followers with notifications enabled." });
    }

    const { data: devices, error: devicesError } = await supabase
      .from("android_push_devices")
      .select("token, user_id")
      .in("user_id", followerIds);

    if (devicesError) {
      return jsonResponse({ error: devicesError.message }, 500);
    }

    const tokens = Array.from(
      new Set((devices ?? []).map((row: PushDeviceRow) => row.token).filter(Boolean)),
    );

    if (tokens.length === 0) {
      return jsonResponse({ sent: 0, skipped: "No Android devices registered." });
    }

    let sent = 0;
    const deadTokens: string[] = [];

    for (const token of tokens) {
      const response = await fetch("https://fcm.googleapis.com/fcm/send", {
        method: "POST",
        headers: {
          Authorization: `key=${fcmServerKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          to: token,
          priority: "high",
          data: {
            title: `${authorName} posted a new look`,
            body: occasion,
            postId: post.id,
            profileId: post.user_id,
          },
        }),
      });

      const payload = await response.json();
      if (!response.ok) {
        continue;
      }

      if (payload.failure === 1) {
        const result = payload.results?.[0];
        const errorCode = result?.error;
        if (errorCode === "NotRegistered" || errorCode === "InvalidRegistration") {
          deadTokens.push(token);
        }
        continue;
      }

      sent += 1;
    }

    if (deadTokens.length > 0) {
      await supabase.from("android_push_devices").delete().in("token", deadTokens);
    }

    return jsonResponse({ sent });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return jsonResponse({ error: message }, 500);
  }
});

function jsonResponse(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}
