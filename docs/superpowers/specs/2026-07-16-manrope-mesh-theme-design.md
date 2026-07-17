# Manrope + Mesh Background Theme Design

**Date:** 2026-07-16  
**App:** TranscilMobileApp (native Android, Kotlin + XML)  
**Status:** Approved — self-critiqued; ready for implementation

## Goal

Apply brand typography and day/night mesh backgrounds consistently across the splash screen and every existing page.

## Requirements

1. **Font:** Manrope app-wide via a `font` family XML (Regular 400 + Bold 700) so `android:textStyle="bold"` and theme `fontFamily` resolve correctly.
2. **Dark mode background:** User-provided dark wireframe mesh image.
3. **Light mode background:** User-provided light wireframe mesh image (Option B — separate Figma asset).
4. **Scope of screens:** Splash, Onboarding 1–4, Welcome, Verify OTP.
5. **Splash text:** Centered “Transcil”, Manrope Bold, **27sp** (Figma), line spacing ~32sp, with mode-aware `text_primary`.

## Approach

**Theme-driven resources (chosen):**

- Ship mesh bitmaps under `drawable-nodpi/` (avoid unintended density scaling).
- Expose `@drawable/screen_background` as a `<bitmap>` wrapper with `android:gravity="fill"` so the mesh covers the screen; alias light/dark via `values` / `values-night`.
- Set theme `android:windowBackground` to `@drawable/screen_background` to avoid a white flash before inflate.
- Install Manrope under `res/font/` + `manrope_family.xml`; set theme `fontFamily` / `android:fontFamily` to that family.
- Replace `@color/splash_background` on layouts with `@drawable/screen_background`.
- Color tokens for text and brand; migrate layouts off hex literals and off non-night-aware `brand_dark`.
- Day/night status bar: light icons on dark mesh (`windowLightStatusBar=false` night), dark icons on light mesh (`true` day).

Rationale: one maintenance point, automatic DayNight switching, XML-preview friendly, idiomatic Material3.

## Self-critique (resolved in this revision)

| Issue | Resolution |
|-------|------------|
| Theme `fontFamily` alone does not wire Bold weights | Use `res/font/manrope_family.xml` with Regular + Bold files |
| Spec invented `text_primary` while layouts use `brand_dark` | Make `brand_dark` an alias of `text_primary` **or** replace all `@color/brand_dark` with `@color/text_primary` (prefer replace) |
| `#999999` mentioned but no token | Add `text_muted` (`#999999` light / `#8A8A8A` dark) |
| Raw PNG as `android:background` may letterbox | Wrap in `<bitmap gravity="fill">` |
| Dark source file is JPEG despite `.png` name | Convert to real PNG on import |
| Low-res assets (~390×844 / 473×1024) | Accept for v1; `fill` gravity; optional later upscale from Figma |
| System bar icon contrast ignored | Theme `windowLightStatusBar` day/night |
| Splash size ambiguous (~27 vs current 36sp) | Lock to **27sp** |

## Architecture

```
values/                         values-night/
  colors.xml                      colors.xml
  themes.xml                      themes.xml (status bar + any overrides)
  drawables.xml:
    screen_background_src         screen_background_src
      → @drawable/bg_mesh_light     → @drawable/bg_mesh_dark

res/drawable-nodpi/
  bg_mesh_light.png
  bg_mesh_dark.png

res/drawable/
  screen_background.xml   → <bitmap src="@drawable/screen_background_src" gravity="fill" />

res/font/
  manrope_regular.ttf
  manrope_bold.ttf
  manrope_family.xml      → font family 400/700

Layouts (7):
  → android:background="@drawable/screen_background"
  → text colors use @color/text_* / @color/brand_primary / @color/text_on_brand
```

### Color tokens

| Token | Light | Dark |
|-------|-------|------|
| `screen_background_src` | `bg_mesh_light` | `bg_mesh_dark` |
| `text_primary` | `#1A1A1A` | `#FFFFFF` |
| `text_secondary` | `#666666` | `#B3B3B3` |
| `text_muted` | `#999999` | `#8A8A8A` |
| `brand_primary` | `#8BC34A` | `#8BC34A` |
| `text_on_brand` | `#FFFFFF` | `#FFFFFF` |

Deprecate unused `splash_background` after layouts migrate (or leave unused — prefer delete if nothing references it).

### Splash behavior

- Keep existing ~2s delay in `MainActivity` → `Onboarding1Activity`.
- Visual only: mesh + Manrope Bold 27sp + `text_primary`.
- Do **not** migrate to Android 12 SplashScreen API in this pass.

## Small polish included

- Fix string typo: `GET START` → `GET STARTED`.
- On touched layouts: replace `#8BC34A` → `@color/brand_primary`, `#666666` → `@color/text_secondary`, `#999999` → `@color/text_muted`, button white → `@color/text_on_brand`.
- Progress dots may keep current drawables this pass (inactive `#C4C4C4` is acceptable for v1).

## Out of scope

- Home / post-OTP destination screen
- Wiring Skip on Onboarding 2/3
- Android 12+ SplashScreen compat library
- New onboarding illustrations (3/4 still reuse scooter art)
- Onboarding-seen persistence
- Package rename from `com.example.transcilmobileapp`
- Higher-resolution mesh exports
- Restyling stock `@android:drawable/edit_text` inputs

## Source assets (to import)

| Mode | Cursor asset (source) | App destination | Notes |
|------|----------------------|-----------------|-------|
| Dark | `.../assets/SplashScreen-cae2a970-a140-4be9-8f1e-03f096dff4e1.png` | `app/src/main/res/drawable-nodpi/bg_mesh_dark.png` | Source is JPEG; convert with `sips -s format png` |
| Light | `.../assets/SplashScreen-bg-46ee53db-2ece-45ea-9c72-30394bbecfce.png` | `app/src/main/res/drawable-nodpi/bg_mesh_light.png` | Already PNG |

Manrope: download **Regular** and **Bold** TTF from Google Fonts (SIL Open Font License) into `res/font/` as `manrope_regular.ttf` and `manrope_bold.ttf`.

## Verification

1. Light mode: light mesh fills screen; dark text readable on splash and all pages; dark status-bar icons.
2. Dark mode: dark mesh fills screen; white/light text readable; green CTAs visible; light status-bar icons.
3. Splash: Manrope Bold, 27sp, centered “Transcil”.
4. Body/titles inherit Manrope; bold titles render Bold weight.
5. No layout regressions (onboarding image, dots, buttons, OTP fields).

## Success criteria

- System light/dark switches mesh + text colors without per-screen code.
- Splash matches Figma intent (Manrope Bold, 27sp, mesh background).
- All seven existing screens share `@drawable/screen_background`.
