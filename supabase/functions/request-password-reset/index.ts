import { createClient } from "npm:@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const GENERIC_SUCCESS_MESSAGE =
  "If an account exists for that email, password reset instructions were sent. Check your inbox and spam folder.";

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
    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    const fromEmail =
      Deno.env.get("PASSWORD_RESET_FROM_EMAIL") ??
      Deno.env.get("ACCOUNT_DELETION_FROM_EMAIL") ??
      "HowMyLook <onboarding@resend.dev>";

    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ error: "Missing Supabase service configuration." }, 500);
    }
    if (!resendApiKey) {
      return jsonResponse({
        error: "Password reset email is not configured yet. Add RESEND_API_KEY to Supabase edge function secrets.",
      }, 500);
    }

    const body = await request.json().catch(() => ({})) as { email?: string };
    const email = body.email?.trim().toLowerCase() ?? "";
    if (!email || !email.includes("@")) {
      return jsonResponse({ error: "Enter a valid email address." }, 400);
    }

    const redirectTo = `${supabaseUrl}/functions/v1/complete-password-reset`;
    const admin = createClient(supabaseUrl, serviceRoleKey);
    const { data, error } = await admin.auth.admin.generateLink({
      type: "recovery",
      email,
      options: { redirectTo },
    });

    if (error || !data?.properties?.action_link) {
      console.warn("Password reset link not generated:", error?.message ?? "missing action_link");
      return jsonResponse({
        sent: true,
        message: GENERIC_SUCCESS_MESSAGE,
      });
    }

    const emailResult = await sendPasswordResetEmail({
      apiKey: resendApiKey,
      fromEmail,
      toEmail: email,
      resetUrl: data.properties.action_link,
    });

    if (!emailResult.ok) {
      return jsonResponse({ error: emailResult.error ?? "Unable to send password reset email." }, 500);
    }

    return jsonResponse({
      sent: true,
      message: GENERIC_SUCCESS_MESSAGE,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    return jsonResponse({ error: message }, 500);
  }
});

async function sendPasswordResetEmail(args: {
  apiKey: string;
  fromEmail: string;
  toEmail: string;
  resetUrl: string;
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
      subject: "Reset your HowMyLook password",
      html: `
        <div style="font-family: Arial, sans-serif; line-height: 1.5; color: #111827;">
          <h2>Reset your password</h2>
          <p>We received a request to reset the password for your HowMyLook account.</p>
          <p>If you did not request this, you can ignore this email.</p>
          <p style="margin: 24px 0;">
            <a href="${args.resetUrl}" style="background:#111827;color:#ffffff;padding:12px 18px;border-radius:999px;text-decoration:none;display:inline-block;">
              Reset password
            </a>
          </p>
          <p style="color:#6B7280;font-size:14px;">This link expires after a short time for security.</p>
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
