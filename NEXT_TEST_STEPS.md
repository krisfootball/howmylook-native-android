# HowMyLook Native Android - Next Test Steps

## Current APK
- Debug APK path: `howmylook-native-android/app/build/outputs/apk/debug/app-debug.apk`

## What is now proven
- Gradle wrapper works
- Java 17 works
- Android SDK is configured
- `assembleDebug` succeeds

## Before first real device smoke test
1. Install the debug APK on a real Android device
2. Confirm Supabase bucket + RLS policies are in place
3. Use at least two test accounts for follow/profile verification
4. Keep an eye on upload and follow failures for policy-related messages

## First smoke-test goals
- App installs
- App opens without crashing
- Auth works end to end
- Username flow behaves correctly
- Unlock/rating flow behaves correctly
- Search and people profiles open
- Follow/unfollow works
- Followers/following lists open
- Post detail opens from Home and Search
- Upload creates pending posts and post_images rows

## Known limitations before smoke test
- Runtime/device verification is still the biggest unknown
- Multi-photo detail/gallery polish is still minimal
- More end-to-end validation is still needed for Supabase auth/upload/follow behavior
