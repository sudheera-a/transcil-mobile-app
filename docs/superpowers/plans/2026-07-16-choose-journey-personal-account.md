# Choose Journey + Personal Account Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After OTP, show Choose Your Journey; Rent EV opens Create Personal Account; 3PL stubs for later.

**Architecture:** Two Activities + ViewModels on existing `BaseActivity`/`BaseViewModel`, shared XML tokens/drawables, Intent navigation. No Compose, no API.

**Tech Stack:** Kotlin, XML Views, ViewBinding, Material3, LiveData.

**Spec:** `docs/superpowers/specs/2026-07-16-choose-journey-personal-account-design.md`  
**Skill:** `.cursor/skills/screenshot-to-android-ui/SKILL.md`

## Global Constraints

- Rent EV → Personal Account; 3PL → coming-soon stub only
- Strings in `strings.xml`; no hex/magic dp in layouts — use dimens/color tokens
- Mesh `@drawable/screen_background` + Manrope theme
- LiveData (match existing); validation in ViewModels
- Copy: “Rent an EV” not “Renta”

---

### Task 1: Tokens, drawables, strings

**Files:**
- Create: `app/src/main/res/values/dimens.xml`
- Modify: `values/colors.xml`, `values-night/colors.xml`, `values/strings.xml`
- Create: drawables `bg_card_default|selected`, `bg_input_default|focused`, `bg_chip_default|selected`, `bg_icon_tile`, `ic_check_circle`, `ic_scooter`, `ic_package`, `ic_person`, `ic_email`, `ic_calendar`, `ic_bullet`

- [ ] Add colors: `brand_primary_soft`, `stroke_default`, `surface_card`, `icon_on_brand`
- [ ] Add dimens: screen padding, card radius 16dp, input radius 12dp, chip radius 12dp, button radius 28dp, icon tile 40dp, spacing 8/12/16/24
- [ ] Add all journey + personal-account strings + validation + `coming_soon_3pl`
- [ ] Create shape drawables (stroke + fill) and simple vector icons
- [ ] Commit: `Add tokens and drawables for journey/account screens.`

---

### Task 2: Models + Choose Journey + OTP wire

**Files:**
- Create: `OnboardingModels.kt` (`JourneyType`, `Gender`)
- Create: `ChooseJourneyViewModel.kt`, `ChooseJourneyActivity.kt`, `activity_choose_journey.xml`
- Modify: `VerifyOtpActivity.kt`, `AndroidManifest.xml`

- [ ] VM: `selectedJourney`, `continueEnabled`, `navigateToPersonalAccount`, `showComingSoon`; `onJourneySelected`, `onContinue`
- [ ] Layout: title, subtitle, two selectable cards, Continue button
- [ ] Activity: toggle card selected backgrounds/check visibility; navigate or Toast stub
- [ ] OTP success → `ChooseJourneyActivity` (finish optional)
- [ ] `./gradlew :app:assembleDebug`
- [ ] Commit: `Add Choose Your Journey screen after OTP.`

---

### Task 3: Create Personal Account

**Files:**
- Create: `CreatePersonalAccountViewModel.kt`, `CreatePersonalAccountActivity.kt`, `activity_create_personal_account.xml`

- [ ] VM: name/email/dob/gender LiveData; `onContinue` validates; `navigateNext` stub event; `showError`
- [ ] Layout: back + Personal Details, title, 3 labeled inputs with icons, gender chips, Continue
- [ ] Focus selector on EditTexts; DOB opens `MaterialDatePicker`; format `DD - MM - YYYY`
- [ ] Wire Journey Rent EV → this Activity
- [ ] AssembleDebug + unit tests
- [ ] Commit: `Add Create Personal Account screen for Rent EV.`

---

## Spec coverage

| Spec item | Task |
|-----------|------|
| Tokens/drawables/strings | 1 |
| Journey + OTP nav | 2 |
| Personal Account + Rent EV branch | 3 |
| 3PL stub | 2 |
