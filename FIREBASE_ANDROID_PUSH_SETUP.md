# HowMyLook native Android push setup

This is the Android push path for followed-account notifications.

## What the native app does
- requests Android notification permission on Android 13+
- registers Firebase Cloud Messaging (FCM)
- stores the device token in Supabase table `android_push_devices`
- keeps the follow-level **Notify me** toggle on `follows.notifications_enabled`
- opens the post detail screen when a notification is tapped
- calls the `notify-post-followers` edge function after a successful upload (for auto-approved posts)

## One-time Supabase setup

### 1. Run the SQL migration
In Supabase SQL editor, run:
- `supabase/migrations/20250615000000_android_push_notify.sql`

This creates `android_push_devices` with row-level security so each user can only manage their own tokens.

### 2. Deploy the edge function
From the repo root (with [Supabase CLI](https://supabase.com/docs/guides/cli) linked to your project):

```bash
supabase functions deploy notify-post-followers
```

### 3. Add the Firebase server key secret
In Supabase **Project Settings → Edge Functions → Secrets**, add:

| Name | Value |
|------|--------|
| `FCM_SERVER_KEY` | Firebase Console → Project settings → Cloud Messaging → **Server key** (legacy) |

`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are provided automatically to edge functions.

### 4. Notify followers after a new post
Posts publish as **approved** immediately. After upload, the app calls `notify-post-followers` when the edge function is deployed.

Optional: add a **Database Webhook** on `posts` **Insert** if you want server-side notification only (instead of or in addition to the app call).

## Firebase / Google setup
1. Create or reuse a Firebase project for package `com.howmylook.app`
2. Keep `google-services.json` in the Android app module
3. In Firebase console, enable Cloud Messaging
4. Use the legacy server key for `FCM_SERVER_KEY` above (FCM HTTP v1 with a service account is a future upgrade)

## FCM payload shape
The edge function sends a **data** message (not notification payload) so Android always receives it in `HowMyLookFirebaseMessagingService`:

```json
{
  "title": "Display Name posted a new look",
  "body": "Occasion text",
  "postId": "<uuid>",
  "profileId": "<uuid>"
}
```

## Testing checklist
1. Install an Android build with valid Firebase config
2. Sign in on the follower device
3. Follow someone and tap **Notify me**
4. Confirm a row appears in `android_push_devices` for the follower
5. From the followed account, publish a post
6. Confirm the follower receives a push notification
7. Tap the notification and confirm the post detail screen opens

## Troubleshooting
- **No row in `android_push_devices`:** sign in again; grant notification permission; toggle **Notify me** on a profile
- **No push after post:** confirm `FCM_SERVER_KEY` secret, edge function deploy, and post `moderation_status = approved`
- **Push arrives but tap does nothing:** rebuild with the latest app (MainActivity passes `postId` into navigation)
