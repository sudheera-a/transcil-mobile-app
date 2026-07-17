# KYC Progress Hub + Selfie Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add journey-specific KYC Progress hub as a post-step checkpoint and Selfie Verification screens, keeping existing form Activities and wiring Rent EV / 3PL navigation to match the approved spec.

**Architecture:** In-memory `KycProgressRepository` owns step state by `JourneyType`. Forms mark a step complete then open `KycProgressActivity`. Progress launches the next allowed Activity (or Selfie). Selfie Continue ends at `KycNavigator.openAfterSubmission`. Kotlin + XML Views + ViewBinding + LiveData; no Compose, no CameraX this pass.

**Tech Stack:** Kotlin, XML Views, ViewBinding, Material3, LiveData, JUnit.

**Spec:** `docs/superpowers/specs/2026-07-17-kyc-progress-selfie-design.md`  
**Skill:** `.cursor/skills/screenshot-to-android-ui/SKILL.md`

## Global Constraints

- Hub model **B + Approach 1**: keep form Activities; Progress is post-step checkpoint
- Strings in `strings.xml`; no hex / magic dp in layouts — use dimens/color tokens
- Mesh `@drawable/screen_background` + Manrope theme; light + dark
- LiveData (match existing); navigation / unlock rules in ViewModel + Repository only
- Copy: “Rent Verification Progress” (not “Rents”); strict sequential step unlock
- Rent EV: 6 steps, **no Bank** on happy path; 3PL: 8 steps including Bank
- Camera stub only; Reference / 3PL Other Docs / Skip → Toast stubs
- Package: `com.example.transcilmobileapp` feature folders (`core`, `journey`, `kyc`)

## File map

| File | Responsibility |
|------|----------------|
| `core/NavExtras.kt` | Add `JOURNEY_TYPE` extra key |
| `kyc/KycStepModels.kt` | `KycStep`, `KycStepStatus`, `KycStepUi`, journey step lists |
| `kyc/KycProgressRepository.kt` | In-memory progress; complete / current / percent / canOpen |
| `kyc/KycProgressActivity.kt` + VM + `activity_kyc_progress.xml` + `item_kyc_step.xml` | Hub UI |
| `kyc/SelfieVerificationActivity.kt` + VM + `activity_selfie_verification.xml` | Capture / review stub |
| Form Activities / `ChooseJourney*` | Navigate to Progress; pass journey; unlock 3PL |
| `AndroidManifest.xml`, `strings.xml`, `dimens.xml`, colors, drawables | Registration + tokens |
| `app/src/test/.../KycProgressRepositoryTest.kt` | Unit tests for progress rules |

---

### Task 1: Models + Repository + unit tests

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/KycStepModels.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/KycProgressRepository.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/core/NavExtras.kt`
- Create: `app/src/test/java/com/example/transcilmobileapp/kyc/KycProgressRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `enum class KycStep { PERSONAL, AADHAAR, ADDRESS, REFERENCE, OTHER_DOCS, PAN, BANK, SELFIE }`
  - `enum class KycStepStatus { COMPLETED, IN_PROGRESS, PENDING }`
  - `data class KycStepUi(val step: KycStep, val status: KycStepStatus, val titleRes: Int, val subtitle: String?)`
  - `object KycProgressRepository` with:
    - `fun startJourney(journey: JourneyType)`
    - `fun currentJourney(): JourneyType?`
    - `fun stepsFor(journey: JourneyType): List<KycStep>`
    - `fun markCompleted(step: KycStep, completedSubtitle: String = "Completed just now")`
    - `fun uiSteps(): List<KycStepUi>`
    - `fun completedCount(): Int`
    - `fun totalCount(): Int`
    - `fun progressPercent(): Int`
    - `fun inProgressStep(): KycStep?`
    - `fun canOpen(step: KycStep): Boolean`
    - `fun reset()` (for tests)
  - `NavExtras.JOURNEY_TYPE = "JOURNEY_TYPE"`

- [ ] **Step 1: Write failing unit tests**

```kotlin
package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.JourneyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KycProgressRepositoryTest {

    @Before
    fun setUp() {
        KycProgressRepository.reset()
    }

    @Test
    fun rentEv_hasSixSteps_withoutBank() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        val steps = KycProgressRepository.stepsFor(JourneyType.RENT_EV)
        assertEquals(6, steps.size)
        assertFalse(steps.contains(KycStep.BANK))
        assertFalse(steps.contains(KycStep.PAN))
        assertTrue(steps.contains(KycStep.OTHER_DOCS))
        assertTrue(steps.contains(KycStep.SELFIE))
    }

    @Test
    fun threePl_hasEightSteps_includingBankAndPan() {
        KycProgressRepository.startJourney(JourneyType.THREE_PL)
        val steps = KycProgressRepository.stepsFor(JourneyType.THREE_PL)
        assertEquals(8, steps.size)
        assertTrue(steps.contains(KycStep.PAN))
        assertTrue(steps.contains(KycStep.BANK))
    }

    @Test
    fun afterPersonal_aadhaarIsInProgress_andOnlyAllowedOpen() {
        KycProgressRepository.startJourney(JourneyType.RENT_EV)
        KycProgressRepository.markCompleted(KycStep.PERSONAL)
        assertEquals(KycStep.AADHAAR, KycProgressRepository.inProgressStep())
        assertTrue(KycProgressRepository.canOpen(KycStep.PERSONAL))
        assertTrue(KycProgressRepository.canOpen(KycStep.AADHAAR))
        assertFalse(KycProgressRepository.canOpen(KycStep.ADDRESS))
        assertEquals(1, KycProgressRepository.completedCount())
        assertEquals(17, KycProgressRepository.progressPercent()) // round(100/6)
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL** (types missing)

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.kyc.KycProgressRepositoryTest
```

Expected: compile/fail — unresolved references.

- [ ] **Step 3: Implement models + repository**

`NavExtras.kt` — add:

```kotlin
const val JOURNEY_TYPE = "JOURNEY_TYPE"
```

`KycStepModels.kt`:

```kotlin
package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.JourneyType

enum class KycStep {
    PERSONAL, AADHAAR, ADDRESS, REFERENCE, OTHER_DOCS, PAN, BANK, SELFIE
}

enum class KycStepStatus {
    COMPLETED, IN_PROGRESS, PENDING
}

data class KycStepUi(
    val step: KycStep,
    val status: KycStepStatus,
    val titleRes: Int,
    val subtitle: String?
)

object KycStepCatalog {
    fun stepsFor(journey: JourneyType): List<KycStep> = when (journey) {
        JourneyType.RENT_EV -> listOf(
            KycStep.PERSONAL, KycStep.AADHAAR, KycStep.ADDRESS,
            KycStep.REFERENCE, KycStep.OTHER_DOCS, KycStep.SELFIE
        )
        JourneyType.THREE_PL -> listOf(
            KycStep.PERSONAL, KycStep.AADHAAR, KycStep.ADDRESS,
            KycStep.REFERENCE, KycStep.OTHER_DOCS, KycStep.PAN,
            KycStep.BANK, KycStep.SELFIE
        )
    }

    fun titleRes(step: KycStep): Int = when (step) {
        KycStep.PERSONAL -> R.string.kyc_step_personal
        KycStep.AADHAAR -> R.string.kyc_step_aadhaar
        KycStep.ADDRESS -> R.string.kyc_step_address
        KycStep.REFERENCE -> R.string.kyc_step_reference
        KycStep.OTHER_DOCS -> R.string.kyc_step_other_docs
        KycStep.PAN -> R.string.kyc_step_pan
        KycStep.BANK -> R.string.kyc_step_bank
        KycStep.SELFIE -> R.string.kyc_step_selfie
    }
}
```

`KycProgressRepository.kt` — hold `journey`, `completed: MutableMap<KycStep, String>`, compute first incomplete as `IN_PROGRESS`, rest `PENDING`. `canOpen` = completed or in-progress. `progressPercent` = `kotlin.math.round(100f * completed / total).toInt()`. `startJourney` resets and sets journey (all pending until first mark; before any complete, first step is IN_PROGRESS). `markCompleted` removes step from pending, sets subtitle, advances in-progress to next incomplete.

Note: string resources referenced above are added in Task 2 — for Task 1 compile, either add minimal step title strings in the same task or use temporary string literals in `titleRes` only after Task 2. **Prefer adding the eight `kyc_step_*` string names in Task 1** so tests compile.

- [ ] **Step 4: Run tests — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.kyc.KycProgressRepositoryTest
```

- [ ] **Step 5: Commit** (only if user asked to commit in session)

```bash
git add app/src/main/java/com/example/transcilmobileapp/kyc/KycStepModels.kt \
  app/src/main/java/com/example/transcilmobileapp/kyc/KycProgressRepository.kt \
  app/src/main/java/com/example/transcilmobileapp/core/NavExtras.kt \
  app/src/main/res/values/strings.xml \
  app/src/test/java/com/example/transcilmobileapp/kyc/KycProgressRepositoryTest.kt
git commit -m "$(cat <<'EOF'
Add KYC progress repository and step models.

EOF
)"
```

---

### Task 2: Tokens, strings, drawables

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values-night/colors.xml`
- Create drawables under `app/src/main/res/drawable/`:
  - `bg_kyc_summary_card.xml`, `bg_kyc_step_default.xml`, `bg_kyc_step_active.xml`
  - `bg_progress_badge.xml`, `bg_progress_track.xml`, `bg_progress_fill.xml` (or layer-list)
  - `ic_kyc_step_done.xml`, `ic_kyc_step_progress.xml`, `ic_kyc_step_pending.xml`
  - `bg_selfie_guidelines.xml`, `bg_selfie_frame.xml`
  - `ic_guideline_check.xml` (reuse `ic_check_circle` if suitable)

**Interfaces:**
- Consumes: existing `brand_primary`, `surface_card`, `text_*`, spacing/radius tokens
- Produces: new color tokens `progress_badge_stroke`, `progress_gradient_start`, `progress_gradient_end`, `kyc_connector_line`; dimens `kyc_step_icon_size`, `kyc_progress_bar_height`, `selfie_frame_height`, `radius_selfie_frame`

- [ ] **Step 1: Add colors (day + night)**

```xml
<!-- values/colors.xml -->
<color name="progress_badge_stroke">#FFB300</color>
<color name="progress_badge_text">#FFB300</color>
<color name="progress_gradient_start">#00BCD4</color>
<color name="progress_gradient_end">#8BC34A</color>
<color name="kyc_connector_line">#4A90E2</color>
```

Night: keep badge/gradient accents; connector can stay blue-ish readable on dark.

- [ ] **Step 2: Add dimens**

```xml
<dimen name="kyc_step_icon_size">28dp</dimen>
<dimen name="kyc_progress_bar_height">8dp</dimen>
<dimen name="kyc_connector_width">2dp</dimen>
<dimen name="selfie_frame_height">280dp</dimen>
<dimen name="radius_selfie_frame">24dp</dimen>
```

- [ ] **Step 3: Add strings** (Progress, steps, Selfie, stubs)

Include at minimum:
- `kyc_progress_title`, `kyc_progress_skip`
- `kyc_progress_rent_title`, `kyc_progress_rider_title`
- `kyc_progress_complete_badge` (`%1$d/%2$d COMPLETE`)
- `kyc_progress_overall`, `kyc_progress_helper`, `kyc_verification_steps`
- `kyc_step_personal`, `kyc_step_aadhaar`, `kyc_step_address`, `kyc_step_reference`, `kyc_step_other_docs`, `kyc_step_pan`, `kyc_step_bank`, `kyc_step_selfie`
- `kyc_step_in_progress`, `kyc_step_pending`, `kyc_step_completed_just_now`
- `kyc_need_help_title`, `kyc_need_help_body` (or reuse pending support copy)
- `kyc_skip_stub`, `kyc_reference_stub`, `kyc_other_docs_stub`, `kyc_step_locked`
- Selfie: `selfie_title`, `selfie_guidelines_title`, four guideline lines, `selfie_position_hint`, `selfie_identity_hint`, `selfie_capture`, `selfie_photo_captured`, `selfie_retake`

- [ ] **Step 4: Create shape/vector drawables** using `@color` / `@dimen` only (no hex in XML drawable fills if project already uses color refs; match existing drawable style)

- [ ] **Step 5: Commit** (if requested)

```bash
git commit -m "$(cat <<'EOF'
Add KYC Progress and Selfie design tokens.

EOF
)"
```

---

### Task 3: KYC Progress screen

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/KycProgressViewModel.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/KycProgressActivity.kt`
- Create: `app/src/main/res/layout/activity_kyc_progress.xml`
- Create: `app/src/main/res/layout/item_kyc_step.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `KycProgressRepository`, `KycStepUi`, `JourneyType`, `NavExtras.JOURNEY_TYPE`
- Produces:
  - VM LiveData: `summaryTitleRes`, `badgeText`, `percent`, `steps`, `navigateToStep: KycStep?`, `showStubMessage: Int?`
  - `fun refresh()`, `fun onStepClicked(step: KycStep)`, `fun onSkipClicked()`, `fun onContactSupport()`
  - Activity maps step → Intent:
    - PERSONAL → `CreatePersonalAccountActivity`
    - AADHAAR → `AadhaarVerificationActivity`
    - ADDRESS → `AddressDetailsActivity`
    - REFERENCE → Toast `kyc_reference_stub`
    - OTHER_DOCS → Rent: `PanVerificationActivity`; 3PL: Toast `kyc_other_docs_stub`
    - PAN → `PanVerificationActivity`
    - BANK → `BankDetailsActivity`
    - SELFIE → `SelfieVerificationActivity`
  - Pass `NavExtras.JOURNEY_TYPE` on every outbound Intent

- [ ] **Step 1: ViewModel**

```kotlin
fun refresh() {
    val journey = KycProgressRepository.currentJourney() ?: return
    _summaryTitleRes.value = when (journey) {
        JourneyType.RENT_EV -> R.string.kyc_progress_rent_title
        JourneyType.THREE_PL -> R.string.kyc_progress_rider_title
    }
    _badgeText.value = /* format complete badge */
    _percent.value = KycProgressRepository.progressPercent()
    _steps.value = KycProgressRepository.uiSteps()
}

fun onStepClicked(step: KycStep) {
    if (!KycProgressRepository.canOpen(step)) {
        _showStubMessage.value = R.string.kyc_step_locked
        return
    }
    _navigateToStep.value = step
}
```

- [ ] **Step 2: Layouts**

`activity_kyc_progress.xml`:
- Root: `screen_background`, ScrollView
- Toolbar row: back, title, Skip
- Summary card (title, badge, overall row, ProgressBar or custom View with clipped gradient fill width = percent, helper)
- “VERIFICATION STEPS” label
- `LinearLayout` `llSteps` populated in code (or RecyclerView)
- Help card + Contact Support

`item_kyc_step.xml`:
- Icon + vertical connector stub, title, subtitle, chevron
- Active: `@drawable/bg_kyc_step_active`; else default

For gradient bar: horizontal `FrameLayout` with track + inner `View` whose width is set in code as `parentWidth * percent / 100`, background `@drawable/bg_progress_fill` (gradient).

- [ ] **Step 3: Activity wiring** — observe VM; inflate step rows; onResume `viewModel.refresh()`; back `finish()`; Skip/Support Toasts

- [ ] **Step 4: Manifest** — register `.kyc.KycProgressActivity`

- [ ] **Step 5: Assemble**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit** (if requested)

```bash
git commit -m "$(cat <<'EOF'
Add KYC Progress hub screen.

EOF
)"
```

---

### Task 4: Selfie Verification screen

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/SelfieVerificationViewModel.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/SelfieVerificationActivity.kt`
- Create: `app/src/main/res/layout/activity_selfie_verification.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `KycProgressRepository`, `KycNavigator`
- Produces:
  - VM: `enum class SelfieUiState { CAPTURE, REVIEW }`; LiveData `uiState`; `onCapture()`, `onRetake()`, `onContinue()` → mark `KycStep.SELFIE` completed + `navigateToPending`
  - Activity: toggle Capture vs Review visibility; Continue → `KycNavigator.openAfterSubmission(this)`

- [ ] **Step 1: ViewModel**

```kotlin
fun onCapture() { _uiState.value = SelfieUiState.REVIEW }
fun onRetake() { _uiState.value = SelfieUiState.CAPTURE }
fun onContinue() {
    if (_uiState.value != SelfieUiState.REVIEW) return
    KycProgressRepository.markCompleted(KycStep.SELFIE)
    _navigateToPending.value = true
}
```

- [ ] **Step 2: Layout** — guidelines card, frame `ImageView` (`@drawable` placeholder — use existing avatar-like vector or solid frame), hints, `btnCapture`, `tvPhotoCaptured`, `btnContinue`, `btnRetake`. Show/hide groups by state.

- [ ] **Step 3: Activity + Manifest**

- [ ] **Step 4: AssembleDebug**

- [ ] **Step 5: Commit** (if requested)

```bash
git commit -m "$(cat <<'EOF'
Add Selfie Verification capture and review stub.

EOF
)"
```

---

### Task 5: Wire journey + form navigation

**Files:**
- Modify: `journey/ChooseJourneyViewModel.kt`, `ChooseJourneyActivity.kt`
- Modify: `kyc/CreatePersonalAccountActivity.kt` (+ VM if needed)
- Modify: `kyc/AddressDetailsActivity.kt`
- Modify: `kyc/AadhaarVerificationActivity.kt` / `AadhaarOtpActivity.kt`
- Modify: `kyc/PanVerificationActivity.kt`
- Modify: `kyc/BankDetailsActivity.kt`
- Optional helper: `kyc/KycFlowNavigator.kt` to avoid duplicating “open Progress” Intent code

**Interfaces:**
- Consumes: `KycProgressRepository`, `NavExtras.JOURNEY_TYPE`, `KycProgressActivity`
- Produces: consistent pattern after each form success:

```kotlin
KycProgressRepository.markCompleted(KycStep.XXX)
startActivity(Intent(this, KycProgressActivity::class.java))
```

Journey start:

```kotlin
// ChooseJourney on continue for RENT_EV or THREE_PL:
KycProgressRepository.startJourney(type)
startActivity(
  Intent(this, CreatePersonalAccountActivity::class.java)
    .putExtra(NavExtras.JOURNEY_TYPE, type.name)
)
```

Remove 3PL coming-soon Toast path from Continue (string may remain unused).

**Mark mapping**

| Screen success | markCompleted |
|----------------|---------------|
| CreatePersonalAccount | `PERSONAL` |
| Aadhaar OTP verify (or Aadhaar skip-to-next if any) | `AADHAAR` |
| AddressDetails | `ADDRESS` |
| Pan verify/skip (Rent = Other Docs) | Rent: `OTHER_DOCS`; 3PL: `PAN` |
| Bank verify/skip | `BANK` |
| Selfie continue | `SELFIE` (Task 4) |

Reference never auto-completed this pass (stub only).

- [ ] **Step 1: ChooseJourney** — both journeys → Personal + `startJourney`; drop `_showComingSoon` for Continue path (or leave LiveData unused)

- [ ] **Step 2: Personal → Progress** (not Address)

- [ ] **Step 3: Aadhaar success path → Progress** (change from PAN). Prefer mark + navigate from `AadhaarOtpActivity` on verify; if Aadhaar has skip-to-PAN, mark Aadhaar and go Progress instead.

- [ ] **Step 4: Address → Progress** (not Aadhaar)

- [ ] **Step 5: Pan → Progress** (not Bank); mark `OTHER_DOCS` if Rent else `PAN`

- [ ] **Step 6: Bank → Progress** (not `KycNavigator`); mark `BANK`

- [ ] **Step 7: Ensure Progress step clicks pass journey extra; forms that need journey read `intent.getStringExtra(NavExtras.JOURNEY_TYPE)` or repository `currentJourney()`

- [ ] **Step 8: Manual smoke checklist**
  1. Rent EV: Personal → Progress (6 steps, Aadhaar in progress)
  2. Open Aadhaar → OTP → Progress (Aadhaar done)
  3. Continue Address → Progress → Other Docs opens PAN → Progress → Selfie → Pending
  4. 3PL: Progress shows 8 steps; PAN → Bank → Selfie → Pending
  5. Locked step Toast; Skip/Support Toast; light+dark glance

- [ ] **Step 9: Assemble + unit tests**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

- [ ] **Step 10: Commit** (if requested)

```bash
git commit -m "$(cat <<'EOF'
Wire KYC forms through Progress hub for Rent EV and 3PL.

EOF
)"
```

---

### Task 6: Spec status + final verification

**Files:**
- Modify: `docs/superpowers/specs/2026-07-17-kyc-progress-selfie-design.md` — Status → `Implemented` (or keep Approved until merge)

- [ ] **Step 1: Run full verify**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL; repository tests PASS

- [ ] **Step 2: Confirm checklist vs spec**
  - Progress hub both journeys
  - Selfie capture/review stub
  - Forms unchanged structurally
  - 3PL not dead-end
  - Rent skips Bank
  - No CameraX / no accordion PAN form

- [ ] **Step 3: Stop for user UI review** (hook screens for review before next work)

---

## Spec coverage

| Spec item | Task |
|-----------|------|
| `KycStep` / repository / sequential unlock / percent | 1 |
| Tokens / strings / drawables | 2 |
| KYC Progress UI + Skip/Help stubs | 3 |
| Selfie capture/review + Pending | 4 |
| Journey unlock 3PL; form → Progress wiring; Rent no Bank | 5 |
| Verification / handoff for review | 6 |

## Self-review notes

- No accordion PAN inside Progress (visual active row only) — covered Task 3
- Reference / Other Docs (3PL) stubs — Task 3 + 5
- Figma order Personal → Aadhaar → Address — Task 1 lists + Task 5 nav
- Commits in tasks are optional until the user explicitly asks to commit
