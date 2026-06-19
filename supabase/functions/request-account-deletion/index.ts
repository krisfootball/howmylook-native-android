import { createClient } from "npm:@supabase/supabase-js@2.49.1";
import { createAccountDeletionToken } from "../_shared/account-deletion-token.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const TOKEN_TTL_HOURS = 24;

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    const fromEmail = Deno.env.get("ACCOUNT_DELETION_FROM_EMAIL") ?? "HowMyLook <onboarding@resend.dev>";

    if (!supabaseUrl || !serviceRoleKey || !anonKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }
    if (!resendApiKey) {
      return jsonResponse({
        error: "Account deletion email is not configured yet. Add RESEND_API_KEY to Supabase edge function secrets.",
      }, 500);
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

    const email = user.email?.trim();
    if (!email) {
      return jsonResponse({ error: "Your account does not have an email address on file." }, 400);
    }

    const { token, tokenHashPromise } = createAccountDeletionToken();
    const tokenHash = await tokenHashPromise;
    const expiresAt = new Date(Date.now() + TOKEN_TTL_HOURS * 60 * 60 * 1000).toISOString();

    const admin = createClient(supabaseUrl, serviceRoleKey);
    const { error: upsertError } = await admin.from("account_deletion_requests").upsert({
      user_id: user.id,
      token_hash: tokenHash,
      expires_at: expiresAt,
      created_at: new Date().toISOString(),
    });

    if (upsertError) {
      return jsonResponse({ error: upsertError.message }, 500);
    }

    const confirmUrl = `${supabaseUrl}/functions/v1/confirm-account-deletion?token=${encodeURIComponent(token)}`;
    const emailResult = await sendDeletionEmail({
      apiKey: resendApiKey,
      fromEmail,
      toEmail: email,
      confirmUrl,
    });

    if (!emailResult.ok) {
      return jsonResponse({ error: emailResult.error ?? "Unable to send confirmation email." }, 500);
    }

    return jsonResponse({
      sent: true,
      message: `Confirmation email sent to ${email}. Open the link in that email to permanently delete your account.`,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return jsonResponse({ error: message }, 500);
  }
});

async function sendDeletionEmail(args: {
  apiKey: string;
  fromEmail: string;
  toEmail: string;
  confirmUrl: string;
}) {
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${args.apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: args.fromEmail,
      to: [args.toEmail],
      subject: "Confirm deletion of your HowMyLook account",
      html: `
        <div style="font-family: Arial, sans-serif; line-height: 1.5; color: #111827;">
          <h2>Confirm account deletion</h2>
          <p>You asked to permanently delete your HowMyLook account.</p>
          <p>This removes your profile, photos, votes, follows, and sign-in access. This cannot be undone.</p>
          <p>If you did not request this, ignore this email and your account will stay active.</p>
          <p style="margin: 24px 0;">
            <a href="${args.confirmUrl}" style="background:#111827;color:#ffffff;padding:12px 18px;border-radius:999px;text-decoration:none;display:inline-block;">
              Confirm delete forever
            </a>
          </p>
          <p style="color:#6B7280;font-size:14px;">This link expires in 24 hours.</p>
        </div>
      `,
    }),
  });

  if (!response.ok) {
    const body = await response.text();
    return { ok: false, error: body || `Resend returned ${response.status}` };
  }

  return { ok: true };
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
