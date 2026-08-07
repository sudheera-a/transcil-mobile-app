# Light production hardening — design

**Date:** 2026-07-29  
**App:** TranscilMobileApp  
**Status:** Approved in chat; awaiting file review before implementation

## Goal

Make the **release** build safer and more shippable without rebuilding product features (wallet / hubs / maps / payment SDKs stay stubs). Smallest working diffs; reuse existing patterns; no Hilt, no Compose, no KYC accordion split.

## Decisions

| Topic | Decision |
|-------|----------|
| Application ID | `in.transcil.rider` (Play / install identity) |
| Namespace + Kotlin packages | `com.transcil.rider` (`in` is a Kotlin reserved keyword, so source packages cannot use `in.transcil.rider`) |
| Bank draft at rest | Move `KycLocalStore` to EncryptedSharedPreferences (same pattern as `TokenStore`) |
| Help HTML | Escape non-HTML text fields; leave `body_html` as trusted server HTML |
| Demo scaffolding | Delete if unused by production + tests (`DemoApi`, `DemoRepository`, `DemoViewModel`, `PostDto`, `ApiClient.demoApi`) |
| Package rename migration | Existing debug installs of old ID will not update in place — uninstall / reinstall once |
| Commits | Only when explicitly requested |

## In scope (P0)

### 1. Release build hardening

- Enable R8 minify + resource shrinking for `release` in `app/build.gradle.kts` (replace `optimization { enable = false }`).
- Add `app/proguard-rules.pro` keep rules for:
  - Retrofit interfaces / annotations
  - Gson models (`@SerializedName` / reflective fields)
  - OkHttp
  - EncryptedSharedPreferences / security-crypto
  - Navigation / ViewBinding / DataBinding
  - Play Services SMS Retriever
  - Custom Tabs / browser
- Verify `:app:assembleRelease` succeeds.

### 2. App identity

- Set `applicationId` to `in.transcil.rider`; set `namespace` + Kotlin packages to `com.transcil.rider`.
- Relocate Kotlin sources and tests under `com.transcil.rider`; update manifests / BuildConfig consumers / instrumented test (asserts `applicationId`).
- **Migration note for developers:** uninstall the old `com.example.transcilmobileapp` install before installing the new app.

### 3. Network / secrets / logging

- Keep main manifest `android:usesCleartextTraffic="false"`.
- Keep OkHttp BODY logging gated on `BuildConfig.DEBUG` only (already true in `ApiClient` — re-verify after edits; no token body logs in release).
- `BASE_URL`:
  - Release / default: `https://api.transcil.in/`
  - Debug: optional override from `local.properties` (e.g. `transcil.baseUrl=...`) wired in `build.gradle.kts`; never bake debug HTTP into release.
- Add `network_security_config.xml`: cleartext blocked by default; debug build may allow cleartext to emulator/local gateway only if needed for existing local-dev workflow.

### 4. Sensitive local data

- Change `KycLocalStore` from plain `SharedPreferences` to `EncryptedSharedPreferences` + `MasterKey`, mirroring `TokenStore`.
- Prefer a tiny shared helper only if it shrinks duplication without new abstractions; otherwise duplicate the short create-prefs block (ponytail: keep small).
- Comment that bank account / IFSC are encrypted at rest for process-death survival.
- Extend backup / data-extraction excludes for `transcil_kyc_local` (and tokens already excluded).

### 5. WebView HTML safety

- Escape text fields in `HelpCenterHtml.build` (email, mobile, topic/article titles) before append.
- Do **not** escape `body_html` (trusted server HTML by product choice for this pass).
- Extend `HelpCenterHtmlTest` with a case proving a title containing `<script>` cannot appear as raw script markup (escaped entities instead).

### 6. Dead / demo scaffolding

- Remove unused demo leftovers when nothing production or tests reference them:
  - `DemoApi`, `PostDto`, `DemoRepository`, `DemoViewModel`, `ApiClient.demoApi`
- If a test still needs them, keep and document — do not delete blindly.

### 7. Manifest / backup

- Keep `allowBackup="true"` but tighten rules so sensitive prefs are excluded:
  - Already excluded: `transcil_tokens`
  - Add exclude: `transcil_kyc_local` (both `.xml` path variants as today)
- No broader backup redesign.

### 8. Minimal CI gate

- Add `.github/workflows/android-ci.yml` (or equivalent):
  - `./gradlew testDebugUnitTest`
  - Optionally `./gradlew assembleRelease` (include if CI time is acceptable; prefer both for hardening confidence)

## Out of scope (P1 — track only)

- Split / modularize `KycProgressActivity`
- Delete duplicate standalone KYC Activities vs accordion
- Hilt / Koin
- SharedFlow / SingleLiveEvent migration
- Real wallet / hubs / battery / maps / payment SDK
- Wire Profile Transcil ID from `getProfile`
- Espresso E2E suite
- Jacoco / Kover coverage gates
- Handling `ApiError.clientAction` in UI
- Full bank-verify API (local IFSC gate remains)

## Constraints

- Kotlin, XML Views, ViewBinding / MVVM — match existing style.
- Fix shared helpers once (HTML escape; encrypted prefs pattern from `TokenStore`).
- No new dependencies unless unavoidable.
- Non-trivial new logic leaves **one** small runnable check (extend existing unit tests where possible).
- Do not commit unless asked.

## Acceptance checklist

- [ ] Release minify / shrink on
- [ ] Release assemble succeeds
- [ ] Unit tests pass
- [ ] No OkHttp BODY logs in release
- [ ] Cleartext blocked in release
- [ ] Bank local storage encrypted
- [ ] Help HTML text fields escaped + test
- [ ] Demo dead code removed (or justified kept)
- [ ] Backup rules exclude token + KYC local prefs
- [ ] Basic CI runs unit tests
- [ ] applicationId is `in.transcil.rider`; source namespace is `com.transcil.rider`

## Spec self-review

- **Coverage:** All P0 items from the hardening prompt map to sections 1–8; P1 explicitly deferred.
- **Placeholders:** None; package ID and BASE_URL are concrete.
- **Consistency:** Encrypted prefs name remains `transcil_kyc_local` so backup excludes match store keys; tokens stay `transcil_tokens`.
- **Scope:** No feature rebuild; identity rename is included by explicit user choice (`in.transcil.rider`).
