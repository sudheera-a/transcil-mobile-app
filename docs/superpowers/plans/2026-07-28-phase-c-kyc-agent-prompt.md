# Agent prompt — Phase C KYC (Digio-only)

Copy everything inside the fence below into a new Cursor Agent chat with workspace  
`/Users/sudheer/AndroidStudioProjects/TranscilMobileApp`.

---

````text
You are implementing Phase C KYC API integration in TranscilMobileApp (native Kotlin Android).

## Read first (mandatory)
1. `docs/superpowers/specs/2026-07-28-phase-c-kyc-digio-integration-guide.md` — architect spec (source of truth for decisions)
2. `docs/superpowers/plans/2026-07-28-phase-c-kyc-digio-integration.md` — task plan
3. Existing patterns: `TranscilApi`, `OnboardingRepository`, `DigioKycRepository`, `OnboardingSync`, `KycProgressViewModel`, `DigioKycCallbackActivity`, tests under `app/src/test`

Backend contract reference (if available on machine): rider guide v1.4 §§8–11 + Appendices A–B. Local gateway: `http://localhost:4000` (emulator: `http://10.0.2.2:4000/`).

## Locked decisions (do not reopen)
- **Digio-only** for Aadhaar + Bank. Do NOT wire happy path to `POST /v1/me/verify/aadhaar`, `/aadhaar/otp`, or `POST /v1/me/bank-account`.
- **Completion authority:** only `GET /v1/me/onboarding` after every successful write and after Digio return. Local `KycProgressRepository.markCompleted` must not be the sole source of green checks.
- Digio: `POST /v1/kyc/start` → Custom Tabs(`gateway_url`) → deep link `transcil://kyc/callback` → `POST /v1/kyc/sync-status` → **always** `GET /v1/me/onboarding` → `OnboardingSync.apply`.
- Bank accordion must launch Digio / wait on Digio status — never local-complete bank from form validation.
- Wire: `PUT /v1/me/reference`, KYC upload pipeline (presign → plain S3/MinIO PUT with `required_headers` → `POST /v1/me/kyc/submit`), selfie + other docs, `POST /v1/me/verify/pan` for 3PL.
- Pending/Approved UI from `documents.overall` / `documents.verified` only.
- Idempotency-Key UUID on every POST/PUT/PATCH. Paths like `v1/...` (base URL trailing slash).
- No Digio SDK, no Amplify, no new DI framework. YAGNI. Match existing code style.
- Never persist OTP / Aadhaar / PAN / full account numbers. Don’t log PII.

## Current gaps to fix
- `DigioKycCallbackActivity` marks AADHAAR/BANK complete locally on Digio approved — must refetch onboarding instead (or in addition as non-authority).
- `KycProgressViewModel.submitBank` / `submitReference` / `submitOtherDocs` mark complete locally without APIs.
- Selfie / other docs upload not on `TranscilApi`.
- `BuildConfig.BASE_URL` is prod — add debug `http://10.0.2.2:4000/`.
- `putAddress` may be missing Idempotency-Key — fix if so.
- Happy path must not navigate to `AadhaarOtpActivity`.

## Implementation order (execute task-by-task, TDD where practical)
0. Confirm Docker gateway / Postman onboarding works (note if blocked; continue with MockWebServer tests).
1. Debug BASE_URL → `http://10.0.2.2:4000/` (keep release prod).
2. Harden Digio callback + OnboardingSync as completion authority.
3. Bank CTA Digio-only (remove local bank complete).
4. Wire `PUT /v1/me/reference` + refresh onboarding.
5. Wire shared KYC document pipeline; other docs + selfie; handle rejection + CONFLICT.
6. Wire PAN for 3PL (one-shot; `name_match:false` still 200).
7. Gate Verification Pending / KYC Approved / Home from `documents.*`; refetch on resume.
8. Regression: unit tests green; no direct-Aadhaar happy path; cleanup.

## Acceptance (Definition of Done)
- Digio-only Aadhaar/Bank; no false green steps vs onboarding
- Reference / selfie / other docs survive process death via server state
- Upload S3 failure does not complete step; restart from upload-request
- Rejected docs show `rejection_reason` and allow new upload cycle
- Pending when `documents.overall == in_progress`; Approved only when `documents.verified`
- 401 refresh still works; Idempotency-Key on mutations; `./gradlew :app:testDebugUnitTest` green

## Working style
- Smallest diff that works; reuse repositories/helpers; deletion over new abstractions.
- One commit per task with clear message when I ask to commit (do not commit unless I ask).
- Prefer `superpowers:subagent-driven-development` or execute tasks inline with checkpoints after each task.
- If something in the spec conflicts with the code, follow the Phase C Digio guide and tell me.

Start with Task 1 (debug BASE_URL) after a quick read of the two docs above. Summarize the gap plan in 5 bullets, then implement Task 1.
````

---

## How to use

1. Open Cursor on **`/Users/sudheer/AndroidStudioProjects/TranscilMobileApp`** (not the gateway repo).
2. New Agent chat → paste the fenced prompt.
3. Keep Docker up:  
   `cd /Users/sudheer/Transcil/transcil-gateway/transcil-gateway && docker compose -f docker-compose.yml --project-directory .. up -d`
4. Optional: Postman `baseUrl = http://localhost:4000` smoke before trusting device calls.
