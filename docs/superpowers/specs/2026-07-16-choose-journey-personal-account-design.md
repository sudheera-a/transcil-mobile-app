# Choose Journey + Create Personal Account Design

**Date:** 2026-07-16  
**App:** TranscilMobileApp (Kotlin + XML Views, MVVM)  
**Status:** Approved for planning after user review  
**Branch context:** Implement on a dedicated feature branch from current work (e.g. `feature/choose-journey-personal-account`), or continue on `feature/manrope-mesh-theme` if still open — prefer a dedicated branch if mesh theme is already merge-ready.

## Goal

Add production-lean post-OTP onboarding: journey selection, then personal-account form **only for Rent EV**. Match provided screenshots using existing mesh/Manrope theme and shared tokens — minimal new code, no Compose.

## Confirmed flow

```
… → Welcome → Verify OTP
  → Choose Your Journey
       ├─ Rent an EV + Continue → Create Personal Account → stub next (Toast / later Home)
       └─ 3PL + Continue → stub only (“Coming soon”) — no 3PL form this pass
```

## Screens

### 1. Choose Your Journey

- Title + subtitle from screenshot
- Two exclusive cards: **Rent an EV** (fix typo from “Renta”), **3PL**
- Selected: soft green fill, green stroke, checkmark; unselected: white/surface + grey stroke
- Icon tiles (scooter / package) — vector drawables
- Bullet feature lists (4 each)
- Continue enabled only when a selection exists
- Mesh `@drawable/screen_background`; Manrope via theme

### 2. Create Personal Account (Rent EV only)

- Toolbar: back + “Personal Details”
- Title: “Create Personal Account”
- Fields: Full Name, Email, Date of Birth (`DD - MM - YYYY` via date picker), Gender (Male / Female / Other)
- Focused field + selected gender: soft green fill + green stroke
- Continue: ViewModel validates (name non-blank, basic email, DOB set, gender set) then stub next
- Back → finish to Journey

## Architecture (lean)

| Layer | Approach |
|-------|----------|
| UI | Activities + ViewBinding layouts (existing `BaseActivity`) |
| State | ViewModels extending `BaseViewModel`; **LiveData** for selection / form fields / one-shot navigation (match existing screens; no StateFlow migration this pass) |
| Model | `JourneyType` enum (`RENT_EV`, `THREE_PL`); `Gender` enum |
| Nav | Explicit `Intent`s; OTP → Journey; Journey → Personal Account when `RENT_EV` |
| Resources | `strings.xml`, `dimens.xml` tokens, shared drawables — **no hex / magic dp in layouts** |
| Theme | Existing day/night colors + Manrope + mesh background |

### Efficiency rules (non-negotiable)

- Prefer include/reuse over duplicate XML blocks where it stays readable
- Selection and validation **only** in ViewModels
- One set of card/input/chip drawables shared by both screens
- Snap spacing/radii to dimens tokens (4dp scale), not pixel-perfect chase
- No Compose, no unrelated refactors, no API layer yet

## Files to add / change

**Add**
- `JourneyType.kt`, `Gender.kt` (or single `OnboardingModels.kt` if smaller)
- `ChooseJourneyActivity.kt`, `ChooseJourneyViewModel.kt`, `activity_choose_journey.xml`
- `CreatePersonalAccountActivity.kt`, `CreatePersonalAccountViewModel.kt`, `activity_create_personal_account.xml`
- Drawables: `bg_card_default`, `bg_card_selected`, `bg_input_default`, `bg_input_focused`, `bg_chip_default`, `bg_chip_selected`, icon vectors, bullet
- `values/dimens.xml` (and night only if needed)
- Color tokens: `brand_primary_soft`, `stroke_default`, `stroke_selected` (day + night)

**Change**
- `VerifyOtpActivity.kt` — navigate to `ChooseJourneyActivity` on success
- `AndroidManifest.xml` — register activities
- `strings.xml` — all copy
- `colors.xml` / `values-night/colors.xml` — soft/stroke tokens

## Copy (canonical)

- Journey title/subtitle; card titles/subtitles/bullets; Continue
- Personal Details; Create Personal Account; field labels/hints; Male/Female/Other; Continue
- Errors: invalid email, required fields (short, in strings)
- 3PL stub message

## Out of scope

- 3PL registration form
- Home / dashboard after Personal Account Continue
- Network APIs, persistence of profile
- Compose migration
- Pixel-perfect Figma deltas that fight tokens

## Verification

1. OTP success opens Journey (not Toast-only dead end).
2. Continue disabled until a journey is selected; Rent EV opens Personal Account; 3PL shows stub.
3. Personal Account back returns to Journey; validation blocks bad Continue; happy path stubs next.
4. Light + dark: mesh, readable text, green CTAs/selection still clear.
5. No hardcoded UI strings or layout hex colors.

## Success criteria

- Visual match to screenshots within token system
- Branching logic correct with minimal surface area (2 activities + 2 VMs + shared drawables/tokens)
- Readable, production-lean code consistent with existing BaseActivity/VM patterns
