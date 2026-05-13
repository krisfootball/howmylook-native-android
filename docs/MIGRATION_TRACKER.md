# HowMyLook Native Migration Tracker

## Goal
Build a real Android app in Kotlin + Jetpack Compose that preserves the existing HowMyLook product rules and Supabase backend behavior.

## Milestone 1: vertical slice
- [x] Create standalone native Android project scaffold
- [x] Set package id to `com.howmylook.app`
- [x] Add Compose app shell and navigation
- [x] Add placeholder auth -> username -> home flow structure
- [x] Add home voting screen scaffold with unlock progress
- [x] Add real Supabase configuration strategy
- [~] Add real auth session restore (bootstrap layer started)
- [~] Add real username/profile bootstrap (profile read + save flow started)
- [~] Add real home queue fetch (repository + viewmodel + UI path started)
- [~] Submit a real yes/no vote through backend (RPC path started)
- [~] Update unlock counter from backend state (viewmodel path started)

## Screen mapping
- Auth [scaffolded]
- Username onboarding [scaffolded]
- Home / rating gate [partially wired]
- Search [scaffolded + initial data path]
- Upload [scaffolded]
- Profile [scaffolded + initial data path]
- Post detail
- People profile
- Followers / following

## Backend domains to wire
- auth
- profiles
- posts
- post_images
- votes
- follows
- cast_vote RPC

## Notes
- Existing `howmylook/android/` is the old Capacitor wrapper and is not the native foundation.
- New native work lives in `howmylook-native-android/`.
- Keep the current Next.js app as product reference and backend contract source.
