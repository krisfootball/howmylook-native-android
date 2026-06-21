# Play Store icon assets

| File | Use |
|------|-----|
| `ic_launcher_512.png` | Upload as the **App icon** in Google Play Console (512×512) |
| `ic_launcher_foreground_source.png` | Transparent polaroid artwork (no background square) |

The icon background color is baked into every PNG layer (`#8A5368`), not only the adaptive background XML. That guarantees the dusty-rose background is visible behind the white polaroid frames on all Android launchers.

Launcher PNGs live under `app/src/main/res/mipmap-*`.
