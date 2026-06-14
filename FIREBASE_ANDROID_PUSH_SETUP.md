# HowMyLook native Android push setup

This is the real Android push path for followed-account notifications.

## What the native app now does
- requests Android notification permission on Android 13+
- registers Firebase Cloud Messaging (FCM)
- stores the device token in Supabase table `android_push_devices`
- keeps the existing follow-level `Notify me` toggle on `follows.notifications_enabled`
- prepares notification open intents back into the app

## Still required on backend
Run this SQL in Supabase:
- `howmylook/SUPABASE_MIGRATION_ANDROID_PUSH.sql`

## Firebase / Google setup
1. Create or reuse Firebase project for package `com.howmylook.app`
2. Keep `google-services.json` in repo root of `howmylook-native-android/`
3. In Firebase console, enable Cloud Messaging
4. Get a server credential strategy for sending FCM messages from backend

## Backend send path needed
The current web route `/api/notify-post` only sends browser Web Push.
To finish Android delivery, add a backend sender that:
- finds followers where `follows.notifications_enabled = true`
- loads matching rows from `android_push_devices`
- sends FCM messages with `title`, `body`, `postId`, and `profileId`
- removes invalid tokens when FCM reports them dead

## Suggested payload
```json
{
  "title": "Someone you follow posted a new look ✨",
  "body": "Tap to see the fit.",
  "postId": "<uuid>",
  "profileId": "<uuid>"
}
```

## Testing checklist
1. Install Android build with valid Firebase config
2. Sign in
3. Follow a person
4. Tap `Notify me`
5. Confirm a row appears in `android_push_devices`
6. Create a post from followed account
7. Verify backend sends FCM
8. Verify notification appears and opens the app
