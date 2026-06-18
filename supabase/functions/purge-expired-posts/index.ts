import { createClient } from "npm:@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

type ExpiredPostRow = {
  id: string;
  user_id: string;
  image_url: string | null;
  compare_left_image_url: string | null;
  compare_right_image_url: string | null;
};

type PostImageRow = {
  image_url: string | null;
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }

    const authHeader = request.headers.get("Authorization") ?? "";
    if (!authHeader.includes(serviceRoleKey)) {
      return jsonResponse({ error: "Unauthorized." }, 401);
    }

    const admin = createClient(supabaseUrl, serviceRoleKey);
    const nowIso = new Date().toISOString();

    const { data: expiredPosts, error: fetchError } = await admin
      .from("posts")
      .select("id, user_id, image_url, compare_left_image_url, compare_right_image_url")
      .eq("is_active", true)
      .eq("keep_forever", false)
      .lt("expires_at", nowIso)
      .limit(100);

    if (fetchError) {
      return jsonResponse({ error: fetchError.message }, 500);
    }

    const posts = (expiredPosts ?? []) as ExpiredPostRow[];
    if (posts.length === 0) {
      return jsonResponse({ purged: 0 });
    }

    let purged = 0;
    const bucket = admin.storage.from("post-images");

    for (const post of posts) {
      const { data: postImages, error: imagesError } = await admin
        .from("post_images")
        .select("image_url")
        .eq("post_id", post.id);

      if (imagesError) {
        continue;
      }

      const storagePaths = collectStoragePaths([
        ...(postImages ?? []).map((row: PostImageRow) => row.image_url),
        post.image_url,
        post.compare_left_image_url,
        post.compare_right_image_url,
      ]);

      await admin.from("votes").delete().eq("post_id", post.id);
      await admin.from("post_images").delete().eq("post_id", post.id);
      await admin.from("posts").delete().eq("id", post.id);

      if (storagePaths.length > 0) {
        await bucket.remove(storagePaths);
      }

      purged += 1;
    }

    return jsonResponse({ purged });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return jsonResponse({ error: message }, 500);
  }
});

function collectStoragePaths(urls: Array<string | null | undefined>): string[] {
  const paths = new Set<string>();
  for (const url of urls) {
    if (!url) continue;
    const marker = "/post-images/";
    const index = url.indexOf(marker);
    if (index === -1) continue;
    const path = url.substring(index + marker.length);
    if (path) {
      paths.add(path);
    }
  }
  return Array.from(paths);
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
