# Phase C — Digio-only KYC Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire `TranscilMobileApp` Phase C KYC so Aadhaar+Bank use Digio only, reference/docs/selfie hit real APIs, and `GET /v1/me/onboarding` is the sole completion authority.

**Architecture:** Thin repositories over existing Retrofit `TranscilApi` + OkHttp binary PUT for uploads. ViewModels call repos then always refresh onboarding via `OnboardingSync`. Digio Custom Tabs + `transcil://kyc/callback` remain; callback must sync then refetch onboarding (never local-only `markCompleted` as truth).

**Tech Stack:** Kotlin, Retrofit, OkHttp, Gson, Coroutines, LiveData, EncryptedSharedPreferences, AndroidX Browser. Spec: `docs/superpowers/specs/2026-07-28-phase-c-kyc-digio-integration-guide.md`.

**Agent prompt (copy-paste):** `docs/superpowers/plans/2026-07-28-phase-c-kyc-agent-prompt.md`

## Global Constraints

- Digio-only for Aadhaar + Bank — do **not** wire `/v1/me/verify/aadhaar`, `/otp`, or `/v1/me/bank-account` on the happy path
- Completion authority: **only** `GET /v1/me/onboarding` after every write / Digio return
- Deep link redirect: exactly `transcil://kyc/callback`
- Paths without leading slash: `v1/...` (base URL has trailing `/`)
- `Idempotency-Key` (UUID) on every POST/PUT/PATCH; reuse only on retry of same action
- Never persist OTP, Aadhaar, PAN, full account number
- Follow existing patterns: `OnboardingRepository`, `DigioKycRepository`, `OnboardingSync`, FakeTranscilApi tests
- YAGNI: no new DI framework, no Digio SDK, no Amplify
- Debug BASE_URL for emulator: `http://10.0.2.2:4000/`
- Unit tests: JUnit4 + MockWebServer / fakes (existing style)

---

## File map

| File | Responsibility |
|------|----------------|
| `app/build.gradle.kts` | debug `BASE_URL` → local gateway |
| `data/network/TranscilApi.kt` | Add reference, kyc upload/submit/list, pan, kyc status GET if missing |
| `data/model/kyc/*` | DTOs for upload, submit, reference, pan |
| `repository/OnboardingRepository.kt` | Keep getOnboarding; ensure putAddress sends Idempotency-Key |
| `repository/DigioKycRepository.kt` | start + sync (+ status); no local mark complete |
| `repository/ReferenceRepository.kt` or methods on OnboardingRepository | put/get reference |
| `repository/KycDocumentRepository.kt` | upload-request + OkHttp PUT + submit |
| `repository/PanRepository.kt` or OnboardingRepository | verify pan |
| `kyc/OnboardingSync.kt` | Map all step keys + hydrate drafts; apply `in_progress` |
| `kyc/DigioKycCallbackActivity.kt` | sync → getOnboarding → OnboardingSync → navigate |
| `kyc/KycProgressViewModel.kt` | Digio for Aadhaar/Bank; API for reference/docs; no local bank/reference/other complete |
| `kyc/SelfieVerificationActivity` (+ VM if any) | Use KycDocumentRepository |
| `kyc/PanVerificationActivity` (+ VM) | Use pan API when 3PL |
| Tests under `app/src/test/...` | repos + OnboardingSync + ViewModel behavior |

---

### Task 0: Preconditions (human / agent check)

- [ ] Docker gateway up at `:4000`
- [ ] Postman Auth + `GET /v1/me/onboarding` works
- [ ] Read spec: `docs/superpowers/specs/2026-07-28-phase-c-kyc-digio-integration-guide.md`

---

### Task 1: Debug BASE_URL

**Files:** `app/build.gradle.kts`

- [ ] Add `debug` buildType `buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:4000/\"")`
- [ ] Keep release/default prod `https://api.transcil.in/`
- [ ] Confirm cleartext / network security config allows cleartext for debug if needed
- [ ] Smoke: app `GET v1/me/onboarding` against local (after login)

---

### Task 2: Harden Digio callback + onboarding authority

**Files:**
- Modify: `kyc/DigioKycCallbackActivity.kt`
- Modify: `kyc/OnboardingSync.kt` (ensure aadhaar/bank/`in_progress` applied)
- Modify: `repository/DigioKycRepository.kt` if status helper needed
- Test: Digio callback / OnboardingSync tests

**Behavior:**
1. `syncStatus()` (or status)
2. Always `getOnboarding()` → `OnboardingSync.apply`
3. Remove sole reliance on `markCompleted(AADHAAR/BANK)` from Digio approved
4. Toast if Digio still pending/rejected; open `KycProgressActivity`

- [ ] Failing test: approved Digio without onboarding complete must not leave steps complete if onboarding says pending
- [ ] Implement
- [ ] Tests pass
- [ ] Commit: `fix(kyc): Digio callback uses onboarding as completion authority`

---

### Task 3: Bank CTA = Digio only (no local complete)

**Files:** `kyc/KycProgressViewModel.kt`, possibly Activity binding

**Behavior:**
- `submitBank` must **not** `markCompleted(BANK)`
- Bank primary action = Digio launch (reuse `startDigioFromBank` / `launchDigio`)
- Optional: hide or disable account-number “verify” as completing path; consent still required before Digio

- [ ] Test: submitBank does not mark complete without API/onboarding
- [ ] Implement
- [ ] Commit: `fix(kyc): bank step Digio-only, no local completion`

---

### Task 4: Reference API

**Files:**
- DTOs + `TranscilApi` `PUT/GET v1/me/reference` with Idempotency-Key on PUT
- Repository method
- `KycProgressViewModel.submitReference` → API → `refresh()` (onboarding)
- Tests

**Body:** `{ "relation", "mobile_e164": "+91..." }` from 10-digit UI

- [ ] Tests
- [ ] Implement
- [ ] Commit: `feat(kyc): wire PUT /v1/me/reference`

---

### Task 5: KYC document upload pipeline

**Files:**
- DTOs for upload-request + submit + list
- `TranscilApi` methods
- `KycDocumentRepository`: upload-request → OkHttp PUT with `required_headers` → submit
- Wire Other Docs submit in ViewModel
- Wire Selfie activity/VM
- Tests with MockWebServer for PUT headers

**Rules:** plain PUT, not multipart; selfie `doc_type=selfie`, `doc_number=SELFIE`; after submit always onboarding refresh; rejection → show reason → new cycle

- [ ] Tests
- [ ] Implement other docs
- [ ] Implement selfie
- [ ] Commit: `feat(kyc): presign upload pipeline for selfie and other docs`

---

### Task 6: PAN (3PL)

**Files:** Pan activity/VM + API `POST v1/me/verify/pan` + onboarding refresh

- [ ] One-shot; no OTP; handle `name_match: false` on 200
- [ ] Skip stays client-only (step pending)
- [ ] Commit: `feat(kyc): wire PAN verify for 3PL`

---

### Task 7: Pending / Approved navigation from documents.*

**Files:** `KycProgressViewModel` / navigators / cold start (`AuthSession`)

- [ ] `documents.overall == in_progress` → Verification Pending
- [ ] `documents.verified == true` → KYC Approved → Home
- [ ] Refetch onboarding on Pending `onResume`
- [ ] Commit: `feat(kyc): gate pending/approved from onboarding documents`

---

### Task 8: Cleanup + regression

- [ ] Happy path never opens `AadhaarOtpActivity`
- [ ] `putAddress` sends Idempotency-Key if missing
- [ ] `./gradlew :app:testDebugUnitTest` green
- [ ] Manual: login → journey → personal → address → Digio → reference → selfie → pending
- [ ] Commit: `chore(kyc): phase C regression cleanup`

---

## Definition of Done

Matches spec §14: Digio-only, no false greens, reference/selfie/docs persist across kill, upload failures don’t complete, rejection loop works, pending/approved from `documents.*`, idempotency + PII rules, tests green.

## Out of scope

Direct Aadhaar/bank APIs, Digio SDK, wallet/hubs/bookings, iOS, gateway nginx changes.
