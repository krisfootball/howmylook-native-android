# Account deletion email confirmation

Account deletion is a two-step flow:

1. In the app: Edit profile → **Delete account forever** → confirm twice → **Send confirmation email**
2. In email: open the confirmation link → account is permanently deleted

Nothing is deleted until the user opens the email link.

## 1. Run the SQL migration

In Supabase → **SQL Editor**, run:

`supabase/migrations/20250622000000_account_deletion_requests.sql`

## 2. Deploy edge functions

Deploy these functions to your Supabase project:

- `request-account-deletion`
- `confirm-account-deletion`
- `delete-account` (updated; no longer deletes without email confirmation)

Using Supabase CLI from the project root:

```bash
supabase functions deploy request-account-deletion
supabase functions deploy confirm-account-deletion
supabase functions deploy delete-account
```

## 3. Add Resend for confirmation emails

The app uses [Resend](https://resend.com) to send the confirmation email.

1. Create a Resend account and API key
2. Verify a sending domain (or use Resend's test sender while testing)
3. In Supabase → **Project Settings → Edge Functions → Secrets**, add:

| Secret | Example |
|--------|---------|
| `RESEND_API_KEY` | `re_...` |
| `ACCOUNT_DELETION_FROM_EMAIL` | `HowMyLook <noreply@yourdomain.com>` |

`SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, and `SUPABASE_ANON_KEY` are already provided to edge functions by Supabase.

## 4. Test the flow

1. Sign in on the app
2. Profile → Edit profile → Delete account forever
3. Confirm twice and tap **Send confirmation email**
4. Open the email and tap **Confirm delete forever**
5. You should see an "Account deleted" page in the browser
6. The app session will no longer work for that account

## Notes

- Confirmation links expire after **24 hours**
- Requesting a new email replaces the previous pending link
- Until the email link is opened, the account stays fully active
