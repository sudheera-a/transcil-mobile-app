# Manrope + Mesh Background Theme Design

**Date:** 2026-07-16  
**App:** TranscilMobileApp (native Android, Kotlin + XML)  
**Status:** Approved for planning after user review

## Goal

Apply brand typography and day/night mesh backgrounds consistently across the splash screen and every existing page.

## Requirements

1. **Font:** Manrope app-wide (Bold for splash/titles; Regular for body via theme inheritance).
2. **Dark mode background:** User-provided dark wireframe mesh image.
3. **Light mode background:** User-provided light wireframe mesh image (Option B — separate Figma asset).
4. **Scope of screens:** Splash, Onboarding 1–4, Welcome, Verify OTP.
5. **Splash text:** Centered “Transcil”, Manrope Bold, ~27sp (Figma), with mode-aware text color (white on dark mesh, near-black on light mesh).

## Approach

**Theme-driven resources (chosen):**

- Ship light and dark mesh PNGs as drawables.
- Expose a single `@drawable/screen_background` via resource aliases in `values` / `values-night` (light → `bg_mesh_light`, dark → `bg_mesh_dark`). Also set theme `android:windowBackground` to the same drawable so the window does not flash a flat color before layout inflate.
- Install Manrope under `res/font/` and set `fontFamily` on `Theme.TranscilMobileApp`.
- Replace flat `@color/splash_background` on layouts with `@drawable/screen_background`.
- Introduce theme color tokens for primary text, secondary text, and brand green so hardcoding does not break night mode.

Rationale: one maintenance point, automatic DayNight switching, XML-preview friendly, idiomatic Material3.

## Architecture

```
values/                         values-night/
  colors.xml                      colors.xml (text night variants)
  themes.xml                      themes.xml (if needed)
  drawables.xml:
    screen_background             screen_background
      → @drawable/bg_mesh_light     → @drawable/bg_mesh_dark

res/drawable/
  bg_mesh_light.png
  bg_mesh_dark.png

res/font/
  manrope_regular.ttf
  manrope_bold.ttf

Layouts (7):
  activity_main, activity_onboarding1–4, activity_welcome, activity_verify_otp
  → android:background="@drawable/screen_background"
```

### Color tokens

| Token | Light | Dark |
|-------|-------|------|
| Screen background drawable | `bg_mesh_light` | `bg_mesh_dark` |
| `text_primary` | `#1A1A1A` | `#FFFFFF` |
| `text_secondary` | `#666666` | `#B3B3B3` |
| `brand_primary` | `#8BC34A` | `#8BC34A` (keep unless contrast fails on dark mesh) |

Layouts that currently hardcode `#8BC34A`, `#666666`, `#999999` should use these tokens when touched.

### Splash behavior

- Keep existing ~2s delay in `MainActivity` → `Onboarding1Activity`.
- Visual only change for this pass (mesh + Manrope + mode-aware text).
- Do **not** migrate to Android 12 SplashScreen API in this pass.

## Small polish included

- Fix string typo: `GET START` → `GET STARTED`.
- Where layouts are edited for background/colors, replace hardcoded greys/green with theme colors.

## Out of scope

- Home / post-OTP destination screen
- Wiring Skip on Onboarding 2/3
- Android 12+ SplashScreen compat library
- New onboarding illustrations (3/4 still reuse scooter art)
- Onboarding-seen persistence
- Package rename from `com.example.transcilmobileapp`

## Source assets (to import)

| Mode | Cursor asset (source) | App destination |
|------|----------------------|-----------------|
| Dark | `/Users/sudheer/.cursor/projects/Users-sudheer-AndroidStudioProjects-TranscilMobileApp/assets/SplashScreen-cae2a970-a140-4be9-8f1e-03f096dff4e1.png` | `app/src/main/res/drawable/bg_mesh_dark.png` |
| Light | `/Users/sudheer/.cursor/projects/Users-sudheer-AndroidStudioProjects-TranscilMobileApp/assets/SplashScreen-bg-46ee53db-2ece-45ea-9c72-30394bbecfce.png` | `app/src/main/res/drawable/bg_mesh_light.png` |

Manrope: download official TTF/OTF (Google Fonts) and place under `res/font/` with Android-legal filenames (`manrope_regular`, `manrope_bold`).

## Verification

1. Light mode: light mesh visible; dark text readable on splash and all pages.
2. Dark mode (system night): dark mesh visible; white/light text readable; green CTAs still visible.
3. Manrope applied on splash title and inherited body text.
4. No layout regressions (onboarding image, dots, buttons, OTP fields still laid out correctly).

## Success criteria

- Switching system light/dark updates mesh + text colors without code changes per screen.
- Splash matches Figma intent (Manrope Bold, centered brand name, mesh background).
- All seven existing screens share the same background mechanism.
