# Android ↔ Identity OTP + Digio Wiring Design

**Date:** 2026-07-24  
**App:** `TranscilMobileApp` (`/Users/sudheer/AndroidStudioProjects/TranscilMobileApp`)  
**Backend contract:** `transcil-identity-service` — `docs/MOBILE_APP_INTEGRATION_GUIDE.md`, `docs/ONBOARDING_VERIFICATION_PROGRESS_FLOW.md`  
**Status:** Design approved in chat (scope B, approach #2). Awaiting user review of this written spec before implementation plan.

---

## 1. Goal

Wire the existing native Kotlin app to the Transcil gateway identity APIs:

1. **Phone OTP login** via Cognito-backed `POST /v1/auth/start` → `verify` → `refresh` / `logout`
2. **Bearer tokens** on the existing Retrofit `ApiClient`
3. **Digio KYC** for Aadhaar and Bank only: Custom Tabs + deep-link return → `sync-status`

Replace stub login and stub Aadhaar-OTP navigation. Do **not** greenfield a new client or rewrite the whole KYC funnel.

---

## 2. Decisions (locked)

| Decision | Choice |
|---|---|
| Client | Existing `TranscilMobileApp` (Kotlin, Retrofit, OkHttp, Gson) |
| Architecture | Thin repositories over `TranscilApi` (approach #2) |
| Slice | **B** — Auth + Digio for Aadhaar/Bank; Personal/Address/PAN/Selfie stay local |
| Digio UI | Custom Tabs primary; WebView only if Custom Tabs unavailable |
| Deep link | `transcil://kyc/callback` |
| Token storage | EncryptedSharedPreferences (not in-memory; not hardware Keystore yet) |
| Auth SDK | No Amplify / AppAuth Hosted UI — identity owns OTP |

---

## 3. Out of scope

- Flutter / RN / new module in gateway monorepo
- Staff SRP / MFA
- `GET /v1/me/onboarding` as progress source of truth (slice C later)
- Digio Android SDK
- Digio webhook handling (server-only)
- Rewriting Personal / Address / PAN / Selfie Activities
- Hardware-backed Keystore / biometric unlock of tokens

---

## 4. Architecture

```text
WelcomeActivity / VerifyOtpActivity
        │
        ▼
  AuthRepository ──► TranscilApi ──► gateway :4000 / prod
        │
        ▼
  TokenStore (EncryptedSharedPreferences)
        │
        ├── AuthInterceptor      (Bearer access_token)
        └── TokenAuthenticator   (401 → POST /v1/auth/refresh once)

Aadhaar / Bank CTA (progress or step Activities)
        │
        ▼
  DigioKycRepository ──► POST /v1/kyc/start
        │
        ▼
  Custom Tabs(gateway_url)
        │
        ▼
  DigioKycCallbackActivity (transcil://kyc/callback)
        │
        ▼
  DigioKycRepository.syncStatus() ──► local KycProgressRepository mark Aadhaar/Bank
        │
        ▼
  KycProgressActivity
```

Public content GETs already on `TranscilApi` stay unchanged and keep working without a token.

---

## 5. Auth + tokens

### 5.1 API surface (`TranscilApi`)

| Method | Path | Body (request) | Success data |
|---|---|---|---|
| POST | `v1/auth/start` | `phone_e164` | `session`, `ttl_seconds`, `resend_after_seconds`, … |
| POST | `v1/auth/verify` | `session`, `phone_e164`, `otp` | `access_token`, `id_token`, `refresh_token`, `expires_in`, `token_type` |
| POST | `v1/auth/refresh` | `refresh_token` | `access_token`, `id_token`, `expires_in` (no new refresh) |
| POST | `v1/auth/logout` | (Bearer only) | `{ ok: true }` |

Phone UI collects 10 digits; repository prefixes `+91` to form E.164.

### 5.2 Envelope / Gson

Identity uses snake_case (`request_id`, `phone_e164`, `access_token`, `error.code`, `client_action`).  
Existing content DTOs may use camelCase (`requestId`). Use `@SerializedName` on new DTOs and on shared `ApiMeta` / typed `ApiError` so both envelopes parse. Do not break public GET parsing.

Parse `error` as a typed object when present; treat JSON `null` as no error (current public responses).

### 5.3 Components

| Component | Responsibility |
|---|---|
| `AuthRepository` | start / verify / refresh / logout; map errors to UI messages / `client_action` |
| `TokenStore` | persist access + refresh; `clear()`; thread-safe reads for OkHttp |
| `AuthInterceptor` | if access token present → `Authorization: Bearer …` |
| `TokenAuthenticator` | on 401, if refresh available and not already retried / not refresh path → call refresh on a **plain** OkHttp client (no authenticator) → save access → retry once; else clear and fail |
| `WelcomeViewModel` | validate → start → navigate with `phone` + `session` extras |
| `VerifyOtpViewModel` | verify → `TokenStore.save` → navigate `ChooseJourneyActivity`; resend = start again, replace session |

### 5.4 Headers

- `Content-Type: application/json`
- `Idempotency-Key: <uuid>` on auth POSTs; new key per user action; reuse only on retry of the same action
- Optional `X-Request-Id`

### 5.5 Navigation (unchanged after login)

Verify success → `ChooseJourneyActivity` (existing). Session mobile may still be saved in `KycProgressRepository` for display.

### 5.6 Error handling (auth)

| `client_action` / code | App behavior |
|---|---|
| `stay_on_screen` / `AUTH_OTP_INVALID` | Toast / inline; stay on Verify |
| `restart_flow` / `AUTH_SESSION_EXPIRED`, `AUTH_OTP_LOCKED` | Back to Welcome or force new start |
| `retry_after` / rate limits | Disable CTA; honor `Retry-After` |
| `refresh_session` / `AUTH_TOKEN_EXPIRED` | Authenticator refresh (protected calls) |
| `sign_in_again` / `AUTH_REFRESH_INVALID` | Clear tokens; route to Welcome |

---

## 6. Digio (Aadhaar + Bank only)

### 6.1 API surface

| Method | Path | Body | Success data |
|---|---|---|---|
| POST | `v1/kyc/start` | `customer_name` (required, letters/spaces), `redirect_url` | `gateway_url`, `session_id`, `digio_request_id`, `status` |
| POST | `v1/kyc/sync-status` | (empty / as contract) | updated Digio / KYC status |
| GET | `v1/kyc/status` | — | optional poll helper |

`redirect_url` = `transcil://kyc/callback`.

`customer_name` = display / given name from personal draft (`KycProgressRepository`); reject empty or names with digits (match server validation).

### 6.2 Components

| Component | Responsibility |
|---|---|
| `DigioKycRepository` | start + syncStatus |
| `DigioLauncher` | open Custom Tabs; WebView Activity fallback |
| `DigioKycCallbackActivity` | exported; handles `transcil://kyc/callback`; calls sync; updates local progress; returns to `KycProgressActivity` |

### 6.3 Entry / exit

1. User opens Aadhaar or Bank from progress (or existing step Activity).
2. Consent checkbox remains (local UX).
3. Verify CTA → `kyc/start` → Custom Tabs(`gateway_url`).
4. Digio completes → deep link → `sync-status`.
5. Mark local Aadhaar and/or Bank step complete in `KycProgressRepository` when sync reports approved / completed (map server status explicitly in repo; if sync says pending, leave step pending and show toast).
6. Do **not** navigate to `AadhaarOtpActivity` on the happy path (class may remain unused).

`BankDetailsActivity` remains available as optional manual fallback; not required for Digio happy path in this slice.

### 6.4 Manifest

- Intent-filter on callback Activity: `VIEW` + `BROWSABLE` + `DEFAULT`, scheme `transcil`, host `kyc`, pathPrefix `/callback` (or equivalent for `transcil://kyc/callback`).
- Custom Tabs: `androidx.browser` dependency.

### 6.5 Errors (Digio)

| Code | Behavior |
|---|---|
| `VALIDATION_FAILED` on `customer_name` | Stay; prompt complete personal name first |
| `UPSTREAM_DIGIO_FAILED` | Toast retry |
| 401 | Refresh via authenticator; if fail → Welcome |
| User cancels Custom Tab | Stay on progress; no false “complete” |

---

## 7. File map (expected touch set)

**New**

- `data/network/AuthApi` methods on `TranscilApi` (or keep single interface)
- `data/model/auth/*` DTOs
- `data/model/kyc/Digio*.kt` DTOs
- `data/model/ApiError.kt` (typed)
- `repository/AuthRepository.kt`
- `repository/DigioKycRepository.kt`
- `kyc/DigioLauncher.kt`
- `kyc/DigioKycCallbackActivity.kt`
- Unit tests: auth repo mapping, TokenAuthenticator refresh success/fail, Digio start request shape

**Edit**

- `TokenStore.kt` → EncryptedSharedPreferences
- `TokenAuthenticator.kt` → real refresh
- `ApiResponse.kt` / `ApiMeta` → dual naming / typed error
- `WelcomeViewModel.kt` / `WelcomeActivity.kt` (pass session)
- `VerifyOtpViewModel.kt` / `VerifyOtpActivity.kt`
- `AadhaarVerificationActivity` (+ ViewModel) and Bank entry from progress → Digio start
- `AndroidManifest.xml` — deep link
- `app/build.gradle.kts` — `androidx.browser`, security-crypto if needed
- `NavExtras` — session key for OTP screen

**Leave alone (slice B)**

- Personal / Address / PAN / Selfie Activities
- Public content GETs / Help Center wiring
- Home wallet / hubs stubs

---

## 8. Delivery order

1. DTOs + envelope hardening + `AuthRepository`
2. `TokenStore` persistence + `TokenAuthenticator` refresh + unit tests
3. Wire Welcome / Verify OTP UI
4. Digio DTOs + repository + Custom Tabs + deep link + sync
5. Point Aadhaar/Bank CTAs at Digio; disconnect Aadhaar OTP nav
6. Manual E2E against local gateway (`10.0.2.2:4000`)

---

## 9. Testing / success criteria

**Automated**

- `TokenAuthenticator`: 401 → refresh → retry with new Bearer; refresh fail → clear store
- Auth repository: builds `+91` E.164; maps `AUTH_OTP_INVALID`
- Digio start: sends `customer_name` + `redirect_url=transcil://kyc/callback`

**Manual (emulator + Docker gateway)**

1. OTP start/verify (OTP from identity logs in local Cognito-disabled/dev mode)
2. Logcat shows `Authorization: Bearer` on a protected call after login
3. Kill app → reopen → access token still present (EncryptedSharedPreferences)
4. Force expired access → refresh recovers without re-login
5. Aadhaar/Bank → Custom Tab opens Digio gateway URL (or mock gateway in local)
6. Deep link return → sync → progress shows Aadhaar/Bank done locally
7. Public `/v1/settings` still works logged-out and logged-in

---

## 10. Backend base URL (unchanged)

| Runtime | `BuildConfig.BASE_URL` |
|---|---|
| Emulator | `http://10.0.2.2:4000/` |
| Physical device | host LAN IP, e.g. `http://192.168.x.x:4000/` |
| Prod (later) | `https://api.transcil.in/` |

Cleartext already enabled. Paths without leading slash (`v1/auth/start`).

---

## 11. Follow-ups (explicitly later)

- Slice **C:** drive `KycProgressActivity` from `GET /v1/me/onboarding`
- Profile `PATCH /v1/me/profile` for personal/address instead of local-only drafts
- Keystore-backed token storage
- Logout from Profile → `POST /v1/auth/logout` + clear store
