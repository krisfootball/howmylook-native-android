import { createClient } from "npm:@supabase/supabase-js@2.49.1";

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
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

    if (!supabaseUrl || !serviceRoleKey || !anonKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }

    const authHeader = request.headers.get("Authorization");
    if (!authHeader) {
      return jsonResponse({ error: "You need to be signed in to delete your account." }, 401);
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const {
      data: { user },
      error: userError,
    } = await userClient.auth.getUser();

    if (userError || !user) {
      return jsonResponse({ error: "Unable to verify your session." }, 401);
    }

    const admin = createClient(supabaseUrl, serviceRoleKey);
    const userId = user.id;

    const { data: posts, error: postsError } = await admin
      .from("posts")
      .select("id")
      .eq("user_id", userId);

    if (postsError) {
      return jsonResponse({ error: postsError.message }, 500);
    }

    const postIds = (posts ?? []).map((row) => row.id).filter(Boolean);

    if (postIds.length > 0) {
      const { error: postVotesError } = await admin.from("votes").delete().in("post_id", postIds);
      if (postVotesError) {
        return jsonResponse({ error: postVotesError.message }, 500);
      }

      const { error: postImagesError } = await admin.from("post_images").delete().in("post_id", postIds);
      if (postImagesError) {
        return jsonResponse({ error: postImagesError.message }, 500);
      }

      const { error: deletePostsError } = await admin.from("posts").delete().eq("user_id", userId);
      if (deletePostsError) {
        return jsonResponse({ error: deletePostsError.message }, 500);
      }
    }

    const { error: ownVotesError } = await admin.from("votes").delete().eq("user_id", userId);
    if (ownVotesError) {
      return jsonResponse({ error: ownVotesError.message }, 500);
    }

    const { error: followerRowsError } = await admin.from("follows").delete().eq("follower_id", userId);
    if (followerRowsError) {
      return jsonResponse({ error: followerRowsError.message }, 500);
    }

    const { error: followingRowsError } = await admin.from("follows").delete().eq("following_id", userId);
    if (followingRowsError) {
      return jsonResponse({ error: followingRowsError.message }, 500);
    }

    const { error: pushDevicesError } = await admin.from("android_push_devices").delete().eq("user_id", userId);
    if (pushDevicesError) {
      return jsonResponse({ error: pushDevicesError.message }, 500);
    }

    const { error: notificationsError } = await admin.from("user_notifications").delete().eq("user_id", userId);
    if (notificationsError) {
      return jsonResponse({ error: notificationsError.message }, 500);
    }

    await removeStoragePrefix(admin, "post-images", userId);
    await removeStoragePrefix(admin, "profile-avatars", userId);

    const { error: profileError } = await admin.from("profiles").delete().eq("id", userId);
    if (profileError) {
      return jsonResponse({ error: profileError.message }, 500);
    }

    const { error: deleteAuthError } = await admin.auth.admin.deleteUser(userId);
    if (deleteAuthError) {
      return jsonResponse({ error: deleteAuthError.message }, 500);
    }

    return jsonResponse({ deleted: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return jsonResponse({ error: message }, 500);
  }
});

async function removeStoragePrefix(
  admin: ReturnType<typeof createClient>,
  bucket: string,
  prefix: string,
) {
  const { data, error } = await admin.storage.from(bucket).list(prefix, { limit: 1000 });
  if (error || !data?.length) {
    return;
  }

  const paths = data
    .map((item) => item.name)
    .filter(Boolean)
    .map((name) => `${prefix}/${name}`);

  if (paths.length > 0) {
    await admin.storage.from(bucket).remove(paths);
  }
}

function jsonResponse(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}
