# HowMyLook Native Android - Internal Test Readiness

## Current readiness

Approximate status: **~90% toward internal testing**

The app now includes:
- auth + session bootstrap
- username flow
- rating queue + vote submission
- upload flow using Android photo picker + Supabase Storage + post creation
- search people + looks
- people profile + follow/unfollow
- post detail
- followers/following lists
- first pass of tester-focused error handling

## Biggest remaining risk

The biggest remaining gap is **runtime verification on a real device against the live Supabase project**.

Compile success is no longer the main question. The main question is whether the app behaves correctly in real use.

## What to verify first

### Critical path
1. Sign up with a fresh account
2. Sign in
3. Confirm username selection is required when fallback username exists
4. Rate looks until unlock (or low-queue bypass triggers)
5. Open Search
6. Open another person profile
7. Follow / unfollow
8. Open followers / following lists
9. Open a post detail from Home and Search
10. Create a post with 1 photo
11. Create a post with multiple photos
12. Confirm upload lands in `post-images` bucket
13. Confirm post is created as `pending`
14. Confirm `post_images` rows are created

### Error-path checks
- Try upload when storage bucket/policies are missing or broken
- Try follow when follows RLS is missing or broken
- Try weak connection / intermittent network
- Verify auth restore after app restart
- Verify behavior when no rateable posts are available

## Supabase dependencies to double-check
- `post-images` bucket exists and is writable
- storage policies allow authenticated upload
- `posts` insert policy works
- `post_images` insert policy works
- `follows` policies work for insert/delete/select
- `cast_vote` RPC is available and current
- `login_rating_votes_completed` schema + flow are current

## Suggested immediate next milestone

**Milestone: first real device verification pass**

Success means:
- sign in works
- unlock flow works
- follow works
- upload works on an actual device
- created post reaches Supabase correctly

If that pass is clean enough, the app is ready for a very small internal tester group.
