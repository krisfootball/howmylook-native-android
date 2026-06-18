# Expiry, moderation, and reports setup

HowMyLook hides expired posts in the app and can **hard-delete** them on the server.

## How expiry works

- New posts get `expires_at` = 30 days from upload.
- **Kept** posts (`keep_forever = true`) stay until the user deletes them (max 10 kept).
- The app only loads posts that are kept **or** not expired yet.
- A server job removes expired, non-kept posts completely (database rows, votes, and storage files).

## One-time Supabase setup

### 1. Run the SQL migration

In Supabase **SQL Editor**, run:

`supabase/migrations/20250620000000_reports_and_expiry.sql`

This creates the `content_reports` table for in-app reports.

### 2. Deploy the purge function

In your project folder terminal:

```powershell
supabase functions deploy purge-expired-posts
```

### 3. Schedule daily purge (recommended)

Use Supabase **Edge Functions → Cron** (or an external cron) to call:

`POST https://kivvtomgajzzgcjcuyqd.supabase.co/functions/v1/purge-expired-posts`

**Header:**

`Authorization: Bearer YOUR_SERVICE_ROLE_KEY`

(Service role key: **Project Settings → API → service_role** — keep secret.)

Run once per day (e.g. 03:00 UTC).

You can test manually with the same POST; the response looks like `{ "purged": 3 }`.

## Moderation

- New uploads publish as **`approved` immediately** — everyone sees them in Home, Search, and profiles right away.
- Each new post also lands in your **Admin** queue (`admin_reviewed = false`) until you tap **Approve** or **Delete**.
- **Approve** only clears the post from the admin grid (it stays live for everyone).
- **Delete** removes the post completely.
- Push notifications fire right after upload when push is set up.

### Admin setup

1. Run `supabase/migrations/20250621000000_admin_moderation.sql`
2. Run `supabase/migrations/20250621000001_post_admin_reviewed.sql`
3. Grant yourself admin:

```sql
update public.profiles set is_admin = true where username = 'yourusername';
```

4. Sign out and sign back in — the **Admin** tab appears in the bottom bar.

## In-app features added

| Feature | Where |
|--------|--------|
| **Report post** | Post detail → Report (not your own post) |
| **Report profile** | Someone else’s profile → Report profile |
| **Forgot password** | Sign in screen → Forgot password? |
| **Expiry** | Automatic in all feeds; hard delete via purge function |

Reports are stored in `content_reports` for you to review in Supabase.

## Password reset note

Supabase sends the reset email. In **Authentication → URL Configuration**, set your site URL and redirect URLs so the email link works on mobile/web.
