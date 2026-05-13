# HowMyLook Native Android

Real native Android app scaffold for HowMyLook.

## What this is
- Kotlin + Jetpack Compose project skeleton
- separate from the old Capacitor wrapper in `howmylook/android/`
- intended to grow into the main mobile app

## Current status
The native app now includes:
- Android app module
- Compose theme
- navigation shell
- real Compose auth and username forms
- Supabase Android dependencies and provider setup
- native auth bootstrap that checks for an existing Supabase session
- profile read/save flow
- rating queue loading + `cast_vote` submission
- Home screen with unlock progress and queue state
- Search with real people/look loading
- Upload using Android photo picker + Supabase Storage + pending post creation
- people profile + follow/unfollow flow
- post detail screen
- followers/following list screens
- first-pass tester-facing error handling for upload/follow failures

## Notes
- `assembleDebug` works on this machine and the debug APK is buildable.
- Supabase config supports `local.properties` overrides, with current web-project values still available as fallbacks.
- The biggest remaining gap is runtime/device verification against the live Supabase project, not basic compile wiring.

## Next step
Run a real device/internal smoke test against the live Supabase backend, then tighten whatever breaks in real usage.
