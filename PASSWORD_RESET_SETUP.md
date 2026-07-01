# Password reset email setup

Forgot password in the app sends a reset email through **Resend** (same provider as account deletion), because Supabase's built-in auth email often does not deliver without custom SMTP.

## Flow

1. In the app: Sign in → enter email → **Forgot password?**
2. Email arrives with a **Reset password** button
3. Open the link in your browser, choose a new password
4. Return to the app and sign in with the new password

## 1. Deploy edge functions

From the project root (with [Supabase CLI](https://supabase.com/docs/guides/cli) linked to your project):

```bash
supabase functions deploy request-password-reset
supabase functions deploy complete-password-reset
```

## 2. Resend secrets

If you already set up account deletion email, you can reuse the same secrets.

In Supabase → **Project Settings → Edge Functions → Secrets**, add:

| Secret | Example |
|--------|---------|
| `RESEND_API_KEY` | `re_...` |
| `PASSWORD_RESET_FROM_EMAIL` | `HowMyLook <noreply@yourdomain.com>` |

Optional: if `PASSWORD_RESET_FROM_EMAIL` is not set, the function falls back to `ACCOUNT_DELETION_FROM_EMAIL`, then Resend's test sender.

## 3. Allow the reset redirect URL

In Supabase → **Authentication → URL Configuration → Redirect URLs**, add:

```
https://YOUR_PROJECT_REF.supabase.co/functions/v1/complete-password-reset
```

Replace `YOUR_PROJECT_REF` with your project ref (for example `kivvtomgajzzgcjcuyqd`).

## 4. Test

1. Rebuild and reinstall the app
2. Sign in screen → enter a registered email → **Forgot password?**
3. You should see: *If an account exists for that email, password reset instructions were sent...*
4. Check inbox and spam for **Reset your HowMyLook password**
5. Open the link, set a new password, then sign in on the app

## Notes

- The app always shows a generic success message (it does not reveal whether the email exists)
- Reset links expire after a short time for security
- If the app shows a server configuration error, deploy the functions and add `RESEND_API_KEY` first
