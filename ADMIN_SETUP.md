# Admin moderation setup

The Android app includes an **Admin** tab (shield icon) for accounts with `is_admin = true` on their profile.

## What admins see

- A photo grid of **new posts you have not reviewed yet**
- **Approve** — removes the post from the admin grid (it stays live for everyone)
- **Delete** — removes the post completely

Posts are **live for everyone as soon as they are uploaded**. The admin grid is only your review queue.

## One-time setup

### 1. Run the SQL migrations

In Supabase **SQL Editor**, run in order:

1. `supabase/migrations/20250621000000_admin_moderation.sql`
2. `supabase/migrations/20250621000001_post_admin_reviewed.sql`

### 2. Make your account admin

```sql
update public.profiles
set is_admin = true
where username = 'yourusername';
```

### 3. Refresh the app

Sign out and sign back in. The **Admin** tab appears between **Post** and **Activity**.

## How posting works

| Stage | Everyone (Home / Search) | Admin grid |
|-------|--------------------------|------------|
| Just uploaded | Visible immediately | Shows Approve / Delete |
| Approved (cleared) | Still visible | Gone |
| Deleted | Gone | Gone |

## Troubleshooting

- **No Admin tab:** confirm `is_admin = true` and sign in again.
- **Empty grid:** only unreviewed posts appear (`admin_reviewed = false`).
- **Approve / Delete fails:** run both SQL migrations so `admin_reviewed` and RLS policies exist.
