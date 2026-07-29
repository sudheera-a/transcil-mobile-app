# Phase C — KYC API Integration Guide (Architect)

**Date:** 2026-07-28  
**Audience:** Android mobile engineers integrating rider KYC  
**App:** `TranscilMobileApp` (`/Users/sudheer/AndroidStudioProjects/TranscilMobileApp`)  
**Backend contract:** `RIDER_APP_INTEGRATION_GUIDE.md` v1.4 (§8–§11, Appendices A–B)  
**Local gateway:** `/Users/sudheer/Transcil/transcil-gateway/transcil-gateway` → `http://localhost:4000`  
**Status:** Decision locked — Digio-only for Aadhaar + Bank (Approach A). No implementation code in this document.

---

## 0. Purpose of this document

This is the **integration playbook** for Phase C. Use it to:

1. Understand the architecture and ownership boundaries  
2. Know which APIs to call, in what order, and what to ignore  
3. Verify contracts in Postman before touching the app  
4. Avoid local-only “step complete” bugs that diverge from the server  
5. Ship a robust Digio-first KYC funnel that still works when review is pending

It does **not** prescribe Kotlin snippets. It does prescribe contracts, sequencing, failure modes, and acceptance criteria.

---

## 1. Locked decisions

| Decision | Choice | Why |
|---|---|---|
| Aadhaar + Bank happy path | **Digio only** | Production path in the platform guide; one journey, less dual-path drift |
| Direct Aadhaar OTP (`/v1/me/verify/aadhaar`) | **Out of scope for Phase C** | Keep UI affordances dormant; do not wire as happy path |
| Direct bank (`POST /v1/me/bank-account`) | **Out of scope for Phase C** | Digio covers bank; do not mark bank complete from a local form submit |
| Progress source of truth | **`GET /v1/me/onboarding` only** | Server owns step composition, status, and document overall state |
| Digio UI shell | Custom Tabs (primary), WebView fallback if needed | Already aligned with prior Digio design |
| Deep link | Exactly `transcil://kyc/callback` | Must match Digio `redirect_url` |
| Local drafts | Allowed for form UX | Never authoritative for completion |
| PAN (3PL) | Wire if step appears in onboarding for `3pl` | One-shot `POST /v1/me/verify/pan`; no OTP |
| Reference (rental) | Wire `PUT /v1/me/reference` | Required for `rider` checklist |
| Selfie + Other Docs | Presign → S3 PUT → submit | Shared upload pipeline |
| Booking / wallet / hubs | Out of Phase C | Separate phases |

---

## 2. Workspace map (where work happens)

| Concern | Path | You do |
|---|---|---|
| Run APIs locally | `/Users/sudheer/Transcil/transcil-gateway/transcil-gateway` | `docker compose -f docker-compose.yml --project-directory .. up -d --build` |
| Prove contracts | Postman against gateway | Base URL below |
| Integrate client | `/Users/sudheer/AndroidStudioProjects/TranscilMobileApp` | Repositories, ViewModels, Activities, sync |
| Contracts reference | Rider guide + `transcil-contracts` / identity docs | Shapes and error codes |

### Base URLs

| Environment | Base URL | Notes |
|---|---|---|
| Local (host / Postman) | `http://localhost:4000` | Nginx gateway |
| Android emulator → host Docker | `http://10.0.2.2:4000/` | Trailing slash required if Retrofit paths omit leading `/` |
| Physical device → Mac | `http://<LAN-IP>:4000/` | Same Wi‑Fi; allow cleartext for debug only |
| Production | `https://api.transcil.in/` | Current `BuildConfig.BASE_URL` default |

**Rule:** App and Postman must hit the **gateway**, never raw service ports (`8081` identity, etc.), except when debugging a single upstream.

---

## 3. Architecture (layers and ownership)

```text
┌─────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│  KycProgressActivity, Digio callback, Selfie / Other Docs UI │
│  — renders steps; collects input; never invents completion   │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│ Application / ViewModels                                     │
│  — validate UX; trigger one repository action;              │
│  — always finish with onboarding refresh + navigate          │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│ Domain repositories (thin)                                   │
│  DigioKycRepository     → /v1/kyc/*                          │
│  OnboardingRepository   → /v1/me/onboarding, profile, address│
│  ReferenceRepository    → /v1/me/reference                   │
│  KycDocumentRepository  → upload-request / submit (+ S3 PUT) │
│  PanRepository (3PL)    → /v1/me/verify/pan                  │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│ Network                                                      │
│  TranscilApi + ApiClient + AuthInterceptor + TokenAuthenticator│
│  Envelope: data | error | meta.request_id                    │
└────────────────────────────┬────────────────────────────────┘
                             │
                    Gateway :4000 / api.transcil.in
                             │
              identity (KYC, Digio, uploads, onboarding)
```

### Ownership rules

1. **Server owns truth** — step list, order, labels, status, `documents.overall`, `documents.verified`.  
2. **Client owns UX** — accordion, consent checkboxes, confirm-account equality, camera capture, Custom Tabs.  
3. **Gateway owns routing** — one base URL; path → service.  
4. **Identity owns Digio** — app never talks to Digio APIs with vendor credentials.  
5. **Local `KycProgressRepository` is a cache/draft store** — after every successful write or Digio return, overwrite completion from onboarding. Prefer deleting “mark complete locally then hope” patterns.

### Anti-patterns (do not ship)

| Anti-pattern | Why it fails |
|---|---|
| `markCompleted(AADHAAR/BANK)` from Digio sync alone without refetching onboarding | Diverges when Digio approved but onboarding still `in_progress`, or address back-fill changes steps |
| Completing bank from local form validation without Digio | Violates Digio-only decision; fake “green check” |
| Hard-coding 6 vs 7 steps from Figma | Server step lists differ by role; Figma ≠ API |
| Skipping `Idempotency-Key` on mutating calls | Duplicate Digio sessions / duplicate KYC rows on retries |
| Multipart upload to Transcil for KYC docs | Contract is presigned **plain PUT** to S3/MinIO |
| Persisting Aadhaar / PAN / full account / OTP | Security + compliance |
| Calling identity on `:8081` from the app | Breaks when nginx headers / routing differ from prod |
| Polling Digio forever in the background | Battery + rate limits; poll on resume/focus + after callback |

---

## 4. Source of truth: `GET /v1/me/onboarding`

### When to call

| Trigger | Required |
|---|---|
| KYC Progress screen `onResume` / first open | Yes |
| After every successful profile / address / reference / PAN / upload submit | Yes |
| After Digio deep-link callback (after sync) | **Yes — mandatory** |
| After forced `POST /v1/kyc/sync-status` | Yes |
| App foreground while on Verification Pending | Yes (pull-based; no KYC push yet) |

### What to use from the response

| Field | Client use |
|---|---|
| `rider_role` | Journey context (`rider` vs `3pl`) |
| `steps[]` | Render checklist in **array order**; map `key` → screen |
| `steps[].status` | `pending` / `in_progress` / `complete` → UI state machine |
| `steps[].edit_endpoint` | Optional routing hint (Digio steps → `/v1/kyc/start`) |
| `steps[].options` | Relations / doc_types for forms |
| `steps[].fields` | Prefill drafts (never treat missing keys as null objects) |
| `documents.overall` | `pending` \| `in_progress` \| `verified` |
| `documents.verified` | Gate **KYC Approved** → dashboard |
| `overall_percent`, `completed_steps`, `total_steps` | Progress chrome (prefer server numbers over local math) |

### Status → UI (authoritative)

| Server status | UI |
|---|---|
| `pending` | CTA enabled (unless locked by product sequencing) |
| `in_progress` | Clock / “In progress…”; **no resubmit** for that step |
| `complete` | Green check; show `completed_at` if present |
| `documents.overall == in_progress` | Verification Pending screen |
| `documents.verified == true` | KYC Approved → unlock Home |

### Role step lists (server-owned; do not hard-code as sole source)

| Role | Typical keys (order) |
|---|---|
| `rider` | personal_details → address → aadhaar → bank → reference → other_docs → selfie |
| `3pl` | personal_details → address → aadhaar → pan → bank → selfie |

Render whatever `steps[]` returns. Digio usually advances **aadhaar + bank** together when Digio status is `approved`.

---

## 5. Cross-cutting API conventions

### Headers

| Header | When | Rule |
|---|---|---|
| `Authorization: Bearer <access_token>` | All protected routes | From TokenStore; never from request body |
| `Content-Type: application/json` | JSON bodies | UTF-8 |
| `Idempotency-Key: <uuid-v4>` | Every POST / PUT / PATCH | New UUID per logical user action; **reuse only when retrying that same action** after timeout |
| `X-Request-Id: <uuid>` | Optional | Echoed in `meta.request_id`; include in bug reports |

### Envelope

- Success: use `data`; ignore null `error`.  
- Failure: branch on `error.code`; show `error.message`; honor `error.client_action`.  
- Always log/store `meta.request_id` for support (never log PII).

### Auth / session (already Phase B — still binding)

| Signal | App behavior |
|---|---|
| `AUTH_TOKEN_EXPIRED` / 401 | TokenAuthenticator → `POST /v1/auth/refresh` once |
| `AUTH_REFRESH_INVALID` | Clear tokens → Welcome |
| Rate limit 429 | Honor `Retry-After`; disable CTA |

### Money / PII

- Phase C mostly avoids money; if amounts appear, use integer paise.  
- Never persist: OTP, full Aadhaar, PAN, bank account number.  
- Masked values from API are display-safe; raw form values stay in memory until submit.

---

## 6. End-to-end flows (Digio-only Phase C)

### 6.1 Preconditions (must already work)

1. Rider authenticated (`access_token` present).  
2. Journey chosen (`PUT /v1/me/rider-role`).  
3. Personal details saved (`PATCH /v1/me/profile`) with a Digio-safe `customer_name` (letters/spaces; no digits).  
4. Address saved (`PUT /v1/me/address`) when product requires it before KYC (guide still allows Digio to back-fill address on approval — do not rely on that for UX).

### 6.2 Digio: Aadhaar + Bank (single hosted journey)

```text
[KYC Progress — Aadhaar or Bank CTA]
        │ consent + (optional local aadhaar field for UX only)
        ▼
POST /v1/kyc/start
  body: { redirect_url: "transcil://kyc/callback", customer_name: "<from profile/draft>" }
  headers: Authorization, Idempotency-Key, Content-Type
        │
        ▼
Open data.gateway_url in Custom Tabs
        │
        ▼
Rider completes Digio (UIDAI + bank)
        │
        ▼
Deep link → DigioKycCallbackActivity
        │
        ▼
POST /v1/kyc/sync-status   (or GET /v1/kyc/status if sync not needed)
        │
        ▼
GET /v1/me/onboarding      ← ALWAYS; replace local completion from steps[]
        │
        ▼
Navigate:
  - aadhaar/bank still pending/in_progress → Progress (+ toast if Digio pending)
  - documents.overall in_progress → Verification Pending when docs submitted
  - documents.verified → KYC Approved
```

**Digio status mapping (gateway/identity):**

| Digio / KYC status | Expected onboarding effect |
|---|---|
| `none` | No Digio session; steps remain pending |
| `pending` | aadhaar/bank often `in_progress`; do not show green complete |
| `approved` | aadhaar + bank → `complete`; address may back-fill |
| `rejected` | Steps reopen as pending (or stay actionable); show message; allow restart Digio |

**Important correction vs fragile client behavior:**  
Do not treat Digio `approved` as sufficient to `markCompleted` locally and stop. **Onboarding is the only completion authority.** Digio sync is a trigger to refresh onboarding.

**Aadhaar number on Digio path:**  
Collecting 12 digits in the app is optional UX/consent theater. Digio hosts UIDAI entry. Do not send Aadhaar to `/v1/kyc/start`. Do not navigate to direct Aadhaar OTP screens on the happy path.

**Bank form on Digio path:**  
Bank account fields in the accordion must **not** call `POST /v1/me/bank-account` and must **not** locally complete BANK. Primary CTA for BANK = launch Digio (same as Aadhaar) or “Continue verification” if Digio already pending.

### 6.3 Reference (rental / `rider` only)

```text
Validate relation ∈ onboarding step options.relations
Validate mobile → E.164 +91[6-9]xxxxxxxxx
        │
        ▼
PUT /v1/me/reference  { relation, mobile_e164 }
  + Idempotency-Key
        │
        ▼
GET /v1/me/onboarding
        │
        ▼
reference step status from server (expect complete)
```

Response may mask `mobile_e164` — keep raw value only in ephemeral form state.

### 6.4 Other Docs + Selfie (shared upload pipeline)

```text
Capture / pick file
  — content_type ∈ image/jpeg | image/png | application/pdf
  — size ≤ max (default 10 MiB)
  — compute sha256 (recommended; server/S3 may enforce)
        │
        ▼
POST /v1/me/kyc/upload-request
  { doc_type, content_type, size_bytes, sha256 }
  selfie: doc_type = "selfie"
  other:  doc_type from options.doc_types (voter_id | driving_license | pan | …)
        │
        ▼
PUT <upload_url>
  — raw bytes
  — forward required_headers exactly (Content-Type, SSE headers, etc.)
  — Content-Length exact
  — NOT multipart; NOT through TranscilApi JSON client
        │
        ▼
If S3/MinIO non-2xx → restart from upload-request (new idempotency key)
        │
        ▼
POST /v1/me/kyc/submit
  { kyc_id, doc_number, holder_name }
  selfie: doc_number = "SELFIE" (or empty per contract)
  other:  real document number
        │
        ▼
GET /v1/me/onboarding
  — step becomes in_progress until back-office approves
        │
        ▼
Optional: GET /v1/me/kyc to show rejection_reason and re-upload
```

**Rejection loop:**  
`status == rejected` → show `rejection_reason` verbatim → new upload-request cycle (new `kyc_id`). Do not reuse a terminal `kyc_id`.

**Conflict:**  
`CONFLICT_KYC_DOC_PENDING` (409) → refetch onboarding/KYC list; do not blind retry submit.

### 6.5 PAN (3PL only)

```text
POST /v1/me/verify/pan
  { pan_number, name, dob? }
  — uppercase PAN pattern; one-shot; ignore Figma OTP
        │
        ▼
If HTTP 200 and name_match == false → warn with registered_name; allow continue / edit profile
        │
        ▼
GET /v1/me/onboarding
```

“Skip for Now” is **client navigation only** — step stays `pending`.

### 6.6 Verification Pending → Approved

| Condition | Screen |
|---|---|
| Docs submitted, review open (`documents.overall == in_progress`) | Verification Pending |
| `documents.verified == true` | KYC Approved → Home |
| No push event yet for approval | Refetch onboarding on foreground / Pending screen focus |

Do not invent a local timer that flips approved.

---

## 7. Recommended client module boundaries

Keep thin repositories; do not put HTTP in Activities.

| Module | Responsibility | Does not |
|---|---|---|
| `OnboardingRepository` | getOnboarding, profile, address, journey already present | Digio / S3 bytes |
| `DigioKycRepository` | start, syncStatus, status | Mark local steps complete as authority |
| `OnboardingSync` | Map onboarding → drafts + step statuses | Call network |
| `KycDocumentRepository` (new conceptually) | upload-request, submit; orchestrate binary PUT | Parse Digio |
| `Reference` API surface | put/get reference | Complete steps locally |
| `Pan` API surface | verify pan | OTP UI |
| Upload binary client | OkHttp (or equivalent) plain PUT with exact headers | Gson JSON body |
| `KycProgressViewModel` | Orchestrate validate → repo → refresh → UI events | Own business rules for Digio approval |

**Idempotency ownership:** generate the key at the start of a user gesture; pass through retries of that gesture; new gesture → new key.

---

## 8. Error handling matrix (Phase C)

| Area | Code / signal | Client action |
|---|---|---|
| Auth | `AUTH_TOKEN_EXPIRED` | Refresh; retry once |
| Auth | `AUTH_REFRESH_INVALID` | Sign in again |
| Digio start | `VALIDATION_FAILED` (name) | Stay; force personal details complete |
| Digio | `UPSTREAM_DIGIO_FAILED` | Toast; retry_same_action with new or same key per product policy (prefer new start session if previous expired) |
| Digio user cancel | (no callback / early close) | Stay on Progress; no complete |
| Digio pending after return | status `pending` | Toast pending; refetch onboarding; leave `in_progress` |
| Reference | `VALIDATION_FAILED` | Field errors from `details.fields` |
| Upload request | size / content_type validation | Stay; fix file |
| S3 PUT failure | non-2xx | Restart upload-request |
| Submit | `CONFLICT_KYC_DOC_PENDING` | Refetch state |
| PAN / bank (if ever called) | `name_match: false` on 200 | Warn; do not treat as hard failure |
| Generic | `retryable: true` | Honor `client_action` / backoff |
| Generic | unknown code | Show message; stay_on_screen; attach request_id |

---

## 9. Security & privacy checklist

1. Tokens: refresh in EncryptedSharedPreferences (or stronger); access short-lived.  
2. Clear tokens on logout and refresh-invalid.  
3. Deep link Activity: validate it only triggers sync + onboarding refresh; no open redirect.  
4. Custom Tabs preferred over embedding vendor pages in a full WebView when possible.  
5. Do not log request bodies that contain document numbers or account numbers.  
6. Debug cleartext only on debug builds; release always HTTPS.  
7. MinIO/S3 URLs are short-lived; do not persist `upload_url`.  
8. sha256 of file content is fine to send; do not send file base64 through identity JSON.

---

## 10. Local vs production realities

| Topic | Local Docker | Production |
|---|---|---|
| Base URL | `localhost` / `10.0.2.2:4000` | `https://api.transcil.in` |
| OTP login | Mock OTP in identity logs | Real SMS (MSG91 / Cognito path) |
| Digio | Often mock / stub depending on identity config | Real Digio hosted flow |
| KYC object storage | MinIO (`9000`) via media/identity wiring | Real S3 + KMS headers |
| Document approval | May need admin/back-office or seed tooling | Manual review 24–48h |
| Direct verify stubs | Available on identity | Still stub until vendor procurement (guide §9.2) — **unused in Digio-only Phase C** |

**Postman before app:** if Digio start fails locally, fix stack/config first; do not paper over with local `markCompleted`.

---

## 11. Postman verification plan (gate before coding)

Create / extend collection **Transcil Rider API** with `baseUrl = http://localhost:4000`.

### Folder: Phase C — KYC

Run in order after Auth + Journey + Profile + Address succeed:

| # | Request | Pass criteria |
|---|---|---|
| 1 | `GET /v1/me/onboarding` | Envelope OK; `steps[]` present |
| 2 | `POST /v1/kyc/start` | `gateway_url`, `session_id`; status pending |
| 3 | (Manual) open `gateway_url` if real Digio; or note mock behavior | — |
| 4 | `POST /v1/kyc/sync-status` or `GET /v1/kyc/status` | status in `none|pending|approved|rejected` |
| 5 | `GET /v1/me/onboarding` | aadhaar/bank statuses match Digio outcome |
| 6 | `PUT /v1/me/reference` | 200; then onboarding reference `complete` |
| 7 | `POST /v1/me/kyc/upload-request` (selfie) | `upload_url`, `required_headers`, `kyc_id` |
| 8 | Binary PUT to `upload_url` | 2xx |
| 9 | `POST /v1/me/kyc/submit` | status `submitted` |
| 10 | `GET /v1/me/onboarding` | selfie `in_progress` or per env |
| 11 | `GET /v1/me/kyc` | document listed; rejection fields when applicable |
| 12 | (3PL) `POST /v1/me/verify/pan` | verified / name_match handling |

Save `access_token`, `session_id`, `kyc_id`, and every `meta.request_id` for failures.

### Explicitly do **not** require for Phase C Digio-only

- `POST /v1/me/verify/aadhaar`  
- `POST /v1/me/verify/aadhaar/otp`  
- `POST /v1/me/bank-account`  

(Keep them in a “Deferred / Direct stub” folder for learning only.)

---

## 12. Current app gap analysis (baseline for implementers)

Use this as the delta checklist — not as blame.

| Area | Current tendency | Target for Phase C |
|---|---|---|
| Digio start + Custom Tabs + callback | Present | Keep |
| Callback completion | Local `markCompleted` on Digio approved | Sync → **onboarding refresh** → apply statuses |
| Progress refresh | `getOnboarding` + `OnboardingSync` on refresh | After every write and Digio return |
| Bank accordion submit | Local complete | Digio launch only; no local bank complete |
| Reference / Other Docs | Local complete | Real APIs + onboarding |
| Selfie | Separate UI, upload likely unwired | Shared upload pipeline |
| Direct Aadhaar OTP screens | May still exist | Unreachable on happy path |
| `BASE_URL` | Often prod | Debug build → local gateway for integration |

---

## 13. Implementation sequencing (when you do write code later)

Order maximizes learning and reduces rework. Still no code here — task order only.

1. **Postman Phase C green** on local gateway.  
2. **Debug BASE_URL** to gateway; smoke `GET /v1/me/onboarding` from app.  
3. **Harden Digio callback** to always refresh onboarding; remove Digio-approved local-only completion as authority.  
4. **Bank CTA = Digio** (disable fake complete from bank form).  
5. **Reference API** + refresh.  
6. **Document upload pipeline** (selfie first, then other docs).  
7. **PAN** for 3PL if role selected.  
8. **Pending / Approved navigation** from `documents.*` only.  
9. **Regression:** cold start with token → onboarding routes correctly (progress vs pending vs home).  
10. **Cleanup:** hide/disable direct Aadhaar OTP navigation on Digio path.

Each step should leave the app runnable and Postman still green.

---

## 14. Acceptance criteria (Definition of Done)

Phase C is done when all of the following hold:

1. **Digio-only:** Completing Aadhaar/Bank in the app requires Digio start → browser → callback → sync → onboarding; no direct verify/bank-account on the happy path.  
2. **No false greens:** A step shows `complete` only when onboarding says so.  
3. **Reference / selfie / other docs** persist via API; killing the app and reopening progress restores server state.  
4. **Upload failures** do not mark steps complete; S3 failure restarts presign.  
5. **Rejected docs** show server `rejection_reason` and allow a new upload cycle.  
6. **Verification Pending** appears when `documents.overall == in_progress`; **Approved** only when `documents.verified == true`.  
7. **401/refresh** still works across KYC calls; refresh failure returns to login.  
8. **Idempotency-Key** present on all Phase C mutating calls.  
9. **PII** not written to logs or long-term storage.  
10. **Postman Phase C folder** documented and re-runnable on Docker.  
11. Unit tests cover repository mapping and OnboardingSync status application (when implementation starts) — not part of this guide’s delivery.

---

## 15. Testing strategy (architect view)

| Layer | What to prove |
|---|---|
| Contract (Postman) | Real gateway envelopes and status transitions |
| Repository unit tests | DTO mapping, error.code → Result failure, no completion without onboarding apply |
| ViewModel tests | Digio cancel / pending / approved navigation; bank form does not complete locally |
| Manual device | Custom Tabs return via deep link; cleartext debug URL; MinIO upload on local |
| Regression | Personal/address still refetch onboarding; journey role still drives step list |

Prefer one integration smoke script (Postman or shell) over a heavy UI automation suite for Phase C.

---

## 16. Support & operability

When filing backend bugs:

- Include `meta.request_id`  
- Include environment (local Docker vs prod)  
- Include Digio `session_id` / `digio_request_id` if present  
- Include onboarding step key + status snapshot (redact PII)  
- Never paste full Aadhaar/PAN/account/OTP  

Channel: platform backend team / `#transcil-backend` per rider guide.

---

## 17. Out of scope (explicit)

- Direct Aadhaar OTP and direct bank penny-drop as product paths  
- Digio Android vendor SDK  
- Digio webhook handling (server-only)  
- Push notification on KYC approval (poll instead)  
- Wallet, bookings, hubs, PhonePe mandate  
- Rewriting the entire KYC UI/visual system  
- iOS (this guide’s primary client is Android; contracts are shared)  
- Changing nginx/gateway routing (unless Postman proves a missing route — then escalate to platform)

---

## 18. Quick reference — Phase C API list

| Step | Method | Path | Phase C? |
|---|---|---|---|
| Progress truth | GET | `/v1/me/onboarding` | **Required** |
| Digio start | POST | `/v1/kyc/start` | **Required** |
| Digio sync | POST | `/v1/kyc/sync-status` | **Required** |
| Digio status | GET | `/v1/kyc/status` | Optional helper |
| Reference | PUT | `/v1/me/reference` | **Required** (rider) |
| Upload request | POST | `/v1/me/kyc/upload-request` | **Required** |
| Binary upload | PUT | `<presigned upload_url>` | **Required** |
| Submit doc | POST | `/v1/me/kyc/submit` | **Required** |
| List docs | GET | `/v1/me/kyc` | Recommended |
| PAN | POST | `/v1/me/verify/pan` | **Required** if 3PL |
| Direct Aadhaar | POST | `/v1/me/verify/aadhaar[+ /otp]` | Deferred |
| Direct bank | POST | `/v1/me/bank-account` | Deferred |

---

## 19. Related docs

| Doc | Role |
|---|---|
| `RIDER_APP_INTEGRATION_GUIDE.md` v1.4 | Canonical API contracts |
| `docs/superpowers/specs/2026-07-24-android-identity-digio-wiring-design.md` | Prior Auth + Digio slice (B) |
| `docs/superpowers/plans/2026-07-24-android-identity-digio-wiring.md` | Prior implementation plan |
| This file | Phase C Digio-only KYC architect guide |

---

## 20. Approval gate

Before writing implementation code or an implementation plan:

- [ ] You agree Digio-only for Aadhaar + Bank  
- [ ] You agree onboarding is the only completion authority  
- [ ] You will verify Postman Phase C on Docker before app changes  
- [ ] Bank local form will not fake completion  
- [ ] Upload pipeline will use presign + plain PUT + submit  

Reply **“guide approved”** (with any edits) when ready for a task-level implementation plan. No code until then.
