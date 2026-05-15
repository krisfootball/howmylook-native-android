# Native next pass

Focus order from Kristaps feedback:
1. persistent bottom nav with icons across main app pages
2. full-screen / edge-to-edge photo treatment for Home and post detail
3. wire broken actions/buttons so primary flows work
4. closer parity with web version layout and feel

Observations:
- Current bottom nav is screen-local (`BottomShortcutRow`) instead of shared scaffold shell.
- Search / Upload / Profile currently use back-pill flows rather than persistent main-tab navigation.
- Home photo is inside a 460dp card, so it can never feel full-screen.
- Post detail shows only first image and still uses constrained rounded card treatment.
- AppRoute enum is missing PostDetail / FollowList, so route handling is ad hoc strings.

Implementation idea:
- move nav to a shared Scaffold in `AppNavigation`
- define primary tab routes Home/Search/Upload/Profile
- hide bottom bar on Splash/Auth/Username and maybe on full-screen PostDetail
- make Home surface image-first, edge-to-edge, with floating top/bottom overlays
- make PostDetail full-screen black immersive layout with pager-ready image area
- keep a small safe back affordance on detail/follow list screens
