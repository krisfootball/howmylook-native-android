const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (request.method !== "GET") {
    return htmlResponse("Method not allowed", "Open the password reset link from your email.", 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");

  if (!supabaseUrl || !anonKey) {
    return htmlResponse(
      "Password reset unavailable",
      "Password reset is not configured on the server yet.",
      500,
    );
  }

  return new Response(resetPasswordHtml(supabaseUrl, anonKey), {
    status: 200,
    headers: {
      ...corsHeaders,
      "Content-Type": "text/html; charset=utf-8",
    },
  });
});

function resetPasswordHtml(supabaseUrl: string, anonKey: string) {
  return `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Reset your HowMyLook password</title>
    <style>
      body { font-family: Arial, sans-serif; background: #fff7fb; color: #111827; margin: 0; padding: 32px 16px; }
      .card { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 24px; padding: 28px; box-shadow: 0 8px 30px rgba(15, 23, 42, 0.08); }
      h1 { margin: 0 0 12px; font-size: 28px; }
      p { margin: 0 0 12px; line-height: 1.6; color: #4b5563; }
      label { display: block; margin: 16px 0 6px; font-weight: 600; color: #111827; }
      input { width: 100%; box-sizing: border-box; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 14px; font-size: 16px; }
      button { margin-top: 18px; width: 100%; border: 0; border-radius: 999px; padding: 14px 18px; font-size: 16px; font-weight: 600; background: #111827; color: #ffffff; cursor: pointer; }
      button:disabled { opacity: 0.6; cursor: wait; }
      .error { color: #b91c1c; }
      .success { color: #166534; }
      .hidden { display: none; }
    </style>
  </head>
  <body>
    <div class="card">
      <h1>Reset password</h1>
      <p id="intro">Choose a new password for your HowMyLook account.</p>
      <p id="status" class="error hidden"></p>
      <form id="reset-form" class="hidden">
        <label for="password">New password</label>
        <input id="password" type="password" minlength="6" autocomplete="new-password" required />
        <label for="confirm-password">Confirm password</label>
        <input id="confirm-password" type="password" minlength="6" autocomplete="new-password" required />
        <button id="submit-button" type="submit">Save new password</button>
      </form>
    </div>
    <script type="module">
      import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

      const supabase = createClient(${JSON.stringify(supabaseUrl)}, ${JSON.stringify(anonKey)});
      const statusEl = document.getElementById("status");
      const formEl = document.getElementById("reset-form");
      const introEl = document.getElementById("intro");
      const submitButton = document.getElementById("submit-button");

      function showStatus(message, kind = "error") {
        statusEl.textContent = message;
        statusEl.className = kind;
        statusEl.classList.remove("hidden");
      }

      async function prepareSession() {
        const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ""));
        const accessToken = hashParams.get("access_token");
        const refreshToken = hashParams.get("refresh_token");
        const type = hashParams.get("type");

        if (!accessToken || !refreshToken || type !== "recovery") {
          showStatus("This password reset link is invalid or has expired. Request a new one from the app.");
          introEl.classList.add("hidden");
          return;
        }

        const { error } = await supabase.auth.setSession({
          access_token: accessToken,
          refresh_token: refreshToken,
        });

        if (error) {
          showStatus(error.message);
          introEl.classList.add("hidden");
          return;
        }

        formEl.classList.remove("hidden");
      }

      formEl.addEventListener("submit", async (event) => {
        event.preventDefault();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirm-password").value;

        if (password.length < 6) {
          showStatus("Password must be at least 6 characters.");
          return;
        }
        if (password !== confirmPassword) {
          showStatus("Passwords do not match.");
          return;
        }

        submitButton.disabled = true;
        showStatus("Saving your new password...", "success");

        const { error } = await supabase.auth.updateUser({ password });
        if (error) {
          submitButton.disabled = false;
          showStatus(error.message);
          return;
        }

        formEl.classList.add("hidden");
        introEl.textContent = "Your password was updated. Close this page and sign in on the HowMyLook app.";
        showStatus("Password saved successfully.", "success");
      });

      prepareSession();
    </script>
  </body>
</html>`;
}

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
