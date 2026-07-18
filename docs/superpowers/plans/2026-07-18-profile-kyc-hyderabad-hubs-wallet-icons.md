# Profile KYC Wiring + Hyderabad Hubs + Wallet Icons

> **For agentic workers:** Execute task-by-task. Checkboxes track progress. Do not commit unless user asks.

**Goal:** Profile shows the same personal/KYC session data used elsewhere; hubs use Hyderabad locations; wallet credit/debit glyphs are clear and correctly oriented.

**Architecture:** Extend in-memory `KycProgressRepository` with session mobile; ProfileViewModel reads personal + address + KYC status (from Home Intent) like HomeDashboard; Documents opens KYC Progress; rename hub stubs; fix wallet vector paths + soft colors.

**Tech Stack:** Kotlin, XML Views, ViewBinding, MVVM, JUnit.

## Global Constraints

- No backend / SharedPreferences / Room.
- Do not break bottom nav or KYC Activities flow.
- KYC status source of truth remains `KycNavigator.EXTRA_KYC_STATUS` on `HomeDashboardActivity` Intent (same as Home).
- Hub cities: Nagole, Kukatpally, Cherlapally (Hyderabad) — not Bangalore.
- Credit icon = up / inflow; debit icon = down / outflow.

---

## File map

| File | Change |
|------|--------|
| `kyc/KycProgressRepository.kt` | `saveSessionMobile` / `sessionMobile`; clear on `reset` |
| `auth/VerifyOtpActivity.kt` | Save mobile on successful verify |
| `home/ProfileViewModel.kt` | Bind name, phone, email, location, kycStatus; menu from status |
| `home/ProfileFragment.kt` | Observe + bind phone/email/location/badge; Documents → KYC |
| `home/ProfileRepository.kt` | `menuItems(kycApproved: Boolean)` |
| `fragment_profile.xml` | `android:id` on KYC badge |
| `strings.xml` | Hyderabad hubs; profile pending badge; phone format |
| `NearbyHubsRepository.kt` + `BatterySwapRepository.kt` | New ids/string keys |
| `ic_tx_credit.xml` / `ic_tx_debit.xml` | Swap pathData (correct directions) |
| `colors.xml` + night | Stronger soft fills (~55% alpha) |
| Tests | Profile bind, hubs names, menu routing for Documents → KYC |

---

### Task 1: Session mobile in KYC repository

- [x] Add `sessionMobile` + `saveSessionMobile` / `sessionMobile()` (kept across journey reset)
- [x] On OTP verify success, save mobile before navigate
- [x] Unit test: save/normalize/survives journey switch

### Task 2: Profile binds personal + address + KYC status

- [x] `ProfileViewModel.bind(status)` from personal/address/session + KYC status
- [x] `ProfileFragment` binds phone/email/location + badge; refreshes onResume
- [x] `tvKycBadge` id; pending vs verified styling
- [x] Documents badge only when APPROVED; Documents opens `KycProgressActivity` (home shell kept)
- [x] `ProfileDisplayFormatter` + unit tests

### Task 3: Hyderabad hubs

- [x] Nagole / Kukatpally / Cherlapally strings + repo ids
- [x] BatterySwap recent swaps + tests + tools:text

### Task 4: Wallet icon clarity

- [x] Credit = up arrow; debit = down arrow
- [x] Stronger soft circle alphas (~55%) light + night

### Task 5: Verify

- [x] `:app:testDebugUnitTest` + `:app:assembleDebug` PASS

---

## Out of scope

- Persisting KYC across process death
- Editing profile inline
- Real Documents vault UI (opens existing KYC Progress)
- Changing ride/km/swap stat stubs
