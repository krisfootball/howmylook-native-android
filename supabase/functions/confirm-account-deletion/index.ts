import { createClient } from "npm:@supabase/supabase-js@2.49.1";
import { deleteUserAccount } from "../_shared/delete-user-data.ts";
import { hashToken } from "../_shared/account-deletion-token.ts";

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

    if (!supabaseUrl || !serviceRoleKey) {
      return htmlResponse("Account deletion is not configured.", "Missing Supabase service configuration.", 500);
    }

    const url = new URL(request.url);
    let token = url.searchParams.get("token")?.trim() ?? "";

    if (!token && request.method === "POST") {
      const body = await request.json().catch(() => ({})) as { token?: string };
      token = body.token?.trim() ?? "";
    }

    if (!token) {
      return htmlResponse("Invalid link", "This account deletion link is missing or invalid.", 400);
    }

    const tokenHash = await hashToken(token);
    const admin = createClient(supabaseUrl, serviceRoleKey);

    const { data: requestRow, error: requestError } = await admin
      .from("account_deletion_requests")
      .select("user_id, expires_at")
      .eq("token_hash", tokenHash)
      .maybeSingle();

    if (requestError) {
      return htmlResponse("Unable to confirm deletion", requestError.message, 500);
    }

    if (!requestRow) {
      return htmlResponse(
        "Link expired or already used",
        "This account deletion link is invalid, expired, or was already used.",
        400,
      );
    }

    if (new Date(requestRow.expires_at).getTime() < Date.now()) {
      await admin.from("account_deletion_requests").delete().eq("user_id", requestRow.user_id);
      return htmlResponse(
        "Link expired",
        "This account deletion link has expired. Open the app and request a new confirmation email.",
        400,
      );
    }

    await deleteUserAccount(admin, requestRow.user_id);

    return htmlResponse(
      "Account deleted",
      "Your HowMyLook account and related data have been permanently deleted.",
      200,
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return htmlResponse("Unable to delete account", message, 500);
  }
});

function htmlResponse(title: string, message: string, status: number) {
  const html = `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${escapeHtml(title)}</title>
    <style>
      body { font-family: Arial, sans-serif; background: #fff7fb; color: #111827; margin: 0; padding: 32px 16px; }
      .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 24px; padding: 28px; box-shadow: 0 8px 30px rgba(15, 23, 42, 0.08); }
      h1 { margin: 0 0 12px; font-size: 28px; }
      p { margin: 0; line-height: 1.6; color: #4b5563; }
    </style>
  </head>
  <body>
    <div class="card">
      <h1>${escapeHtml(title)}</h1>
      <p>${escapeHtml(message)}</p>
    </div>
  </body>
</html>`;

  return new Response(html, {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "text/html; charset=utf-8",
    },
  });
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
