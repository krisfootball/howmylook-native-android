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
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

    if (!supabaseUrl || !serviceRoleKey || !anonKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }

    const body = request.method === "POST"
      ? await request.json().catch(() => ({})) as { confirmationToken?: string }
      : {};
    const confirmationToken = body.confirmationToken?.trim() ?? "";

    if (!confirmationToken) {
      return jsonResponse({
        error: "Account deletion now requires email confirmation. Request a confirmation email from Edit profile in the app.",
      }, 400);
    }

    const tokenHash = await hashToken(confirmationToken);
    const admin = createClient(supabaseUrl, serviceRoleKey);

    const { data: requestRow, error: requestError } = await admin
      .from("account_deletion_requests")
      .select("user_id, expires_at")
      .eq("token_hash", tokenHash)
      .maybeSingle();

    if (requestError) {
      return jsonResponse({ error: requestError.message }, 500);
    }

    if (!requestRow) {
      return jsonResponse({ error: "Invalid or expired account deletion confirmation." }, 400);
    }

    if (new Date(requestRow.expires_at).getTime() < Date.now()) {
      await admin.from("account_deletion_requests").delete().eq("user_id", requestRow.user_id);
      return jsonResponse({ error: "This account deletion link has expired." }, 400);
    }

    await deleteUserAccount(admin, requestRow.user_id);

    return jsonResponse({ deleted: true });
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
