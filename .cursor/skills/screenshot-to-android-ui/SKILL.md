---
name: screenshot-to-android-ui
description: >-
  Builds Android screens from Figma/screenshots for TranscilMobileApp using
  Kotlin, XML Views, ViewBinding, and MVVM. Use when implementing UI from
  screenshots, Figma exports, or when the user asks for screenshot-to-code /
  design-to-Android work in this repo.
---

# Screenshot → Android UI (Transcil)

## Stack (do not invent another)

- Native Android: **Kotlin + XML Views + ViewBinding/DataBinding**
- Package today: `com.example.transcilmobileapp`
- MVVM: `BaseActivity` + `BaseViewModel` (LiveData); Activities observe and navigate via `Intent`
- Theme: Material3 DayNight, **Manrope**, `@drawable/screen_background` (mesh)
- **Not** Jetpack Compose unless the user explicitly migrates the project

## Before coding

1. Read latest design specs under `docs/superpowers/specs/` if present
2. Match the **closest existing screen** (e.g. Welcome form, onboarding CTA, OTP)
3. Prefer fewer files: shared drawables/dimens over one-off styles

## Architecture rules

- Hoist selection/validation/navigation decisions to **ViewModel**
- Prefer enums / sealed types for journey, gender, UI events
- User-facing strings → `strings.xml` only
- Keep Activities thin: bind views, forward clicks, observe state
- Do not add API/persistence unless the task asks for it

## Design system rules (non-negotiable)

- Spacing / corner radii → `res/values/dimens.xml` tokens (4dp scale); never scatter magic `12dp` / `16dp` in many layouts without tokens
- Colors → `colors.xml` / `values-night/colors.xml` + theme; **no hex in layouts**
- Reuse: `@drawable/screen_background`, `brand_primary`, `text_primary` / `text_secondary` / `text_muted`, `text_on_brand`
- Shared state drawables for cards/inputs/chips (default vs selected/focused)
- File-specific one-off sizes → private dimens in that feature area or a clearly named dimens entry — only promote to shared tokens when reused

## Screenshot interpretation

- Match layout hierarchy, spacing rhythm, and component roles
- Snap to tokens over pixel-perfect chasing
- Support **light + dark** via theme even if screenshot shows one mode
- Fix obvious design typos in copy (e.g. “Renta” → “Rent”) unless user insists otherwise

## Implementation workflow

1. Identify screen type: onboarding | form | selection | dialog
2. Extend nearest existing Activity/layout pattern
3. Models + ViewModel state first
4. Tokens + shared drawables
5. Layout + Activity wiring + Manifest
6. Minimal diff — no unrelated cleanups

## Explicitly do NOT

- Hardcode colors, spacing, or corner radii in layouts
- Put business validation only in click listeners
- Introduce Compose for a single screen
- Duplicate card/button XML when an include or shared drawable works
- Mix refactors unrelated to the screenshot

## Per-screen checklist (fill before coding)

```text
- Screen name / Activity:
- Entry point (from which screen):
- Interactions + navigation targets:
- States: idle | selected | validation error | stub next
- Dark + light: yes (theme)
- Closest existing screen to copy:
```

## Deliverable

- XML + Kotlin that matches the screenshot within the token system
- List files touched and assumptions
- If screenshot conflicts with tokens, snap to tokens and note the delta
