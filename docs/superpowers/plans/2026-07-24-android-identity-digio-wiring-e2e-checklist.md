# Manual E2E checklist — Android identity + Digio wiring

**Branch:** `feature/android-identity-digio-wiring`  
**Date:** 2026-07-24  
**Status:** Human device QA required (emulator Digio UI cannot be fully driven from CI/agent)

## Preconditions

- [ ] Docker gateway running on host `:4000`
- [ ] Emulator `BASE_URL=http://10.0.2.2:4000/` (host machine `localhost:4000` maps to emulator `10.0.2.2:4000`)
- [ ] Identity OTP visible in identity service logs (local/dev)
- [ ] Debug APK built (`./gradlew :app:assembleDebug`) and installed on emulator/device

### BASE_URL notes

| Client | BASE_URL |
|--------|----------|
| Android emulator | `http://10.0.2.2:4000/` |
| Physical device (same LAN) | `http://<host-LAN-IP>:4000/` |
| Host curl / browser | `http://localhost:4000/` |

Public smoke (host): `curl -sS http://localhost:4000/v1/settings` → expect HTTP 200.

---

## Step 2: Auth

- [ ] Welcome → enter 10-digit mobile → OTP from SMS or identity service logs → Verify → lands on Choose Journey
- [ ] Logcat OkHttp shows `Authorization: Bearer` on a protected call (Digio start or any `/v1/me/*`)
- [ ] Force-stop app → relaunch → session still authenticated (`TokenStore.hasToken()` true; add temporary splash log if needed)
- [ ] Public `GET v1/settings` still works (no auth required; 200 + settings payload)

---

## Step 3: Digio

- [ ] Complete Personal name (letters only)
- [ ] Aadhaar consent → Verify → Custom Tab opens Digio / `gateway_url`
- [ ] Complete or cancel Digio; on return via deep link → sync toast shown
- [ ] On approved: Aadhaar (+ Bank) marked complete in progress UI
- [ ] Confirm no navigation to `AadhaarOtpActivity` on the happy path

---

## Sign-off

| Item | Result |
|------|--------|
| Auth path | _pending_ |
| Digio path | _pending_ |
| Tester | |
| Notes | |
