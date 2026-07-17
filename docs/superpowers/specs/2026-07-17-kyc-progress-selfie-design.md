# KYC Progress Hub + Selfie Design

**Date:** 2026-07-17  
**App:** TranscilMobileApp (Kotlin + XML Views, MVVM)  
**Status:** Phase 1+2 accordion hub in progress (Figma match; CTAs reuse form Activities)  
**Branch context:** Prefer a dedicated feature branch (e.g. `feature/kyc-progress-selfie`) from current mainline work.

## Goal

Add a **KYC Progress hub** (post-step checkpoint) and **Selfie Verification** screens from Figma PDFs, while **keeping existing form Activities** (Personal, Address, Aadhaar, PAN, Bank). Wire journey-specific step lists for Rent EV and 3PL. No Compose, no API layer this pass.

## Confirmed decisions

| Decision | Choice |
|----------|--------|
| Hub vs linear forms | **Accordion Phase 1+2** — expandable Progress cards; CTAs open existing form Activities |
| Navigation model | Hub is primary KYC surface; Edit/Submit/DIGIO/Capture → reuse Activities |
| 3PL | Unlock from “Coming soon”; use 3PL step list on Progress |
| Accordion PAN in hub | Visual expand of in-progress row only; PAN still opens `PanVerificationActivity` |
| Camera | Stub placeholder this pass (no CameraX / face detect) |
| Copy fix | “Rents” → **Rent** Verification Progress |

## Current app (baseline)

```
Splash → Onboarding → Welcome → OTP → Choose Journey
  ├─ Rent EV → Personal → Address → Aadhaar → PAN → Bank → Pending/Approved
  └─ 3PL → Toast “Coming soon”
```

Forms use `layout_step_progress` (4 segments). Status end: `KycNavigator.openAfterSubmission` → Pending stub.

## Target flow

```
Choose Journey
  ├─ Rent EV
  │     → Create Personal Account (existing)
  │     → after each successful form Continue/Verify → KycProgressActivity
  │     → tap current/allowed step → open that Activity (or Selfie)
  │     → Selfie Continue → KycNavigator (Pending stub)
  └─ 3PL
        → Create Personal Account (same form for now)
        → same checkpoint pattern with 3PL step list
        → after Bank → Progress → Selfie → KycNavigator
```

**Back:** Form `ivBack` / system back → `finish()` to previous (usually Progress if launched from hub). Progress back → finish (prior screen). Skip on Progress → stub (Toast or jump to Pending — default Toast “Skip coming soon” unless product asks otherwise).

## Screens

### 1. KYC Progress (`KycProgressActivity`)

**From Figma:** `KYC - Renta an EV.pdf`, `KYC -3PL.pdf`

- Toolbar: back + title “KYC Progress” + Skip (green)
- Summary card:
  - Title: Rent → “Rent Verification Progress”; 3PL → “Rider Verification Progress”
  - Badge: “• {completed}/{total} COMPLETE” (orange stroke)
  - Overall Progress label + percent + gradient bar (cyan→green tokens)
  - Helper: “Complete all steps to finish your KYC verification.”
- Section: “VERIFICATION STEPS”
- Step rows (RecyclerView or vertical LinearLayout of includes):
  - States: **Completed** (green check + timestamp stub), **In progress** (orange ring + “In progress…” + expanded chrome), **Pending** (grey clock + “Pending”)
  - Vertical connector line behind icons
  - Chevron affordance (collapsed/expanded visual); only in-progress row shows expanded highlight border
- Help card: “Need Help?” + body + Contact Support (existing stub Toast)

**Journey step lists**

| Order | Rent EV (6) | 3PL (8) |
|------|-------------|--------|
| 1 | Personal Details | Personal Details |
| 2 | Aadhaar Verification | Aadhaar Verification |
| 3 | Address Verification | Address Verification |
| 4 | Reference | Reference |
| 5 | Other Docs | Other Docs |
| 6 | Selfie Verification | PAN Verification |
| 7 | — | Bank Verification |
| 8 | — | Selfie Verification |

Percent = `round(100f * completed / total)` (Figma shows 50% as example; derive live from store).

### 2. Selfie Verification (`SelfieVerificationActivity`)

**From Figma:** `Selfie Verification-1.pdf`, `Selfie Verification-2.pdf`

**State A — Capture**
- Toolbar: back + “Selfie”
- Guidelines card (green border): title + 4 checklist lines with check icons
- Face frame (rounded, green stroke) with placeholder image
- Helper: “Position your face in the frame” + identity copy
- CTA: “Capture Photo” → switch to State B (no real camera)

**State B — Review**
- Same guidelines + frame (show “captured” placeholder)
- “Photo Captured” label
- Primary: Continue → mark Selfie completed → `KycNavigator.openAfterSubmission`
- Secondary: Retake Photo → back to State A

## Step → Activity mapping

| Step | Opens | Notes |
|------|--------|--------|
| Personal Details | `CreatePersonalAccountActivity` | Existing |
| Aadhaar Verification | `AadhaarVerificationActivity` | Existing (+ OTP child) |
| Address Verification | `AddressDetailsActivity` | Existing |
| Reference | Stub Toast | No form PDF this pass |
| Other Docs | Rent: `PanVerificationActivity`; 3PL: stub Toast | Figma folds PAN under Other Docs for Rent |
| PAN Verification | `PanVerificationActivity` | 3PL only as dedicated step |
| Bank Verification | `BankDetailsActivity` | 3PL only in Progress list; Rent EV has no Bank step in Figma |
| Selfie Verification | `SelfieVerificationActivity` | New |

**Rent EV and Bank:** Figma Rent list has no Bank. Current app still has Bank after PAN. For this pass:

- After PAN success (Rent) → Progress (Other Docs / PAN marked done) with Selfie as next  
- **Do not require Bank for Rent EV Progress completion**  
- Optional: leave `BankDetailsActivity` reachable only if somehow launched; default Rent path skips Bank  

If product later wants Bank for Rent, add it to the Rent step list in a follow-up.

## Navigation changes (existing screens)

| From | Today | After |
|------|--------|--------|
| `ChooseJourneyViewModel` 3PL | Coming soon Toast | Navigate to `CreatePersonalAccountActivity` with `JourneyType.THREE_PL` (same as Rent entry) |
| `CreatePersonalAccountActivity` success | → Address | → `KycProgressActivity` (mark Personal completed; Aadhaar in progress) |
| `AddressDetailsActivity` success | → Aadhaar | → Progress |
| `AadhaarOtpActivity` / Aadhaar success | → PAN | → Progress |
| `PanVerificationActivity` success/skip | → Bank | → Progress (Rent: next Selfie; 3PL: next Bank) |
| `BankDetailsActivity` success/skip | → `KycNavigator` | → Progress (next Selfie) — primarily 3PL |
| Selfie Continue | — | → `KycNavigator.openAfterSubmission` |

**Launching a step from Progress:** `startActivity` with journey extra; on return, Progress reloads from store. Prefer marking a step **completed** in the form ViewModel right before navigating to Progress (not only on Progress open).

**First open after Personal:** Progress shows Personal completed, next step (Aadhaar per Figma order) in progress — even though current app historically did Address before Aadhaar. **Follow Figma order for Progress list and “next” targets:** Personal → Aadhaar → Address → …

When user taps Address before Aadhaar is done: either block (Toast) or allow — **default: only allow completed + current in-progress step** (strict sequential).

## Architecture

| Layer | Approach |
|-------|----------|
| UI | Activities + ViewBinding; `BaseActivity` |
| State | `KycProgressViewModel` + shared `KycProgressRepository` (in-memory singleton / object) holding journey, step statuses, stub timestamps |
| Models | `KycStep` enum (all steps); `KycStepStatus` (COMPLETED / IN_PROGRESS / PENDING); journey-specific ordered lists |
| Nav | Explicit Intents; `NavExtras` / extras for `JourneyType`; reuse `KycNavigator` for final status |
| Resources | `strings.xml`, `dimens.xml` tokens, day/night colors; shared step-row drawable states |
| Theme | Manrope + `@drawable/screen_background` mesh |

### Efficiency rules

- One Progress layout parameterized by journey  
- Shared `item_kyc_step.xml` (or include) for rows  
- Selection / “can open step” / percent only in ViewModel/Repository  
- No hex / magic dp in layouts  
- Minimal diff on form screens (navigate + mark complete)

## Files to add / change

**Add**
- `kyc/KycStep.kt` (or extend `OnboardingModels.kt`) — steps + status
- `kyc/KycProgressRepository.kt` — in-memory progress
- `kyc/KycProgressActivity.kt`, `KycProgressViewModel.kt`, `activity_kyc_progress.xml`
- `kyc/SelfieVerificationActivity.kt`, `SelfieVerificationViewModel.kt`, `activity_selfie_verification.xml`
- Drawables: step icons (check / in-progress / pending), progress bar track/fill, selfie frame border, guidelines card bg
- Strings / dimens / colors as needed
- Manifest entries

**Change**
- `ChooseJourneyActivity` / ViewModel — 3PL → Personal Account + pass journey
- Form Activities/ViewModels — after success navigate to Progress; pass/mark journey + step
- `NavExtras` — journey (and optional step) extras if not already sufficient
- `BankDetailsActivity` — Progress instead of immediate `KycNavigator` when Bank is in flow
- `strings.xml` — remove/repurpose `coming_soon_3pl` usage for main path

## Copy (canonical)

- KYC Progress; Skip; Rent Verification Progress; Rider Verification Progress  
- `{n}/{m} COMPLETE`; Overall Progress; helper line; VERIFICATION STEPS  
- Completed on {date}; In progress…; Pending  
- Need Help? / support body / Contact Support (reuse)  
- Selfie; Guidelines for Best Results; four guideline lines  
- Position your face…; identity helper; Capture Photo; Photo Captured; Continue; Retake Photo  
- Stub: Reference / Other Docs / Skip messages  

## Out of scope

- CameraX, ML Kit face detection, uploading selfie  
- Real Reference / Other Docs forms  
- Accordion-embedded PAN form inside Progress  
- Backend sync of KYC status  
- Home / Dashboard  
- Compose  
- Pixel-perfect deltas that fight tokens  

## Verification

1. Rent EV: Personal Continue → Progress with Rent title and 6 steps; tap Aadhaar → Aadhaar flow → back to Progress with step completed.  
2. Sequential lock: cannot open Selfie before prior steps completed.  
3. Rent path reaches Selfie without Bank; Selfie Continue → Pending.  
4. 3PL: Journey Continue opens Personal (not Toast); Progress shows 8 steps including Bank; Bank → Progress → Selfie → Pending.  
5. Selfie: Capture ↔ Retake; Continue only from review state.  
6. Light + dark readable; no hardcoded hex/strings in layouts.  
7. Contact Support / Skip stubs do not crash.

## Success criteria

- Figma Progress + Selfie screens exist in-app on theme tokens  
- Existing form UIs unchanged in structure; only exit navigation + journey wiring  
- Hub is the checkpoint between steps; Selfie is last KYC UI before Pending  
- 3PL onboarding path is no longer a dead-end Toast  

## Open points (defaults if unchanged)

1. **Skip** on Progress → Toast stub (not Pending).  
2. **Reference / 3PL Other Docs** → Toast stub.  
3. **Rent EV** omits Bank from Progress and from happy path after PAN.  
4. **Timestamps** → fixed stub strings matching Figma style when completed in-session (or “Completed just now”).  
