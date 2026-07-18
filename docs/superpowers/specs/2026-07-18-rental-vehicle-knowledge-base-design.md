# Rental Vehicle Knowledge Base — Design

**Date:** 2026-07-18  
**Status:** Approved (Approach B — domain catalog + wire existing UI)  
**Source:** `/Users/sudheer/Downloads/rental-vehicle-knowledge-base.pdf` (v2)

## Goal

Embed the business knowledge-base data (fleet models, plan prices, onboarding fee) into the Android client as a single in-memory catalog, and wire existing Home / Wallet / journey copy to that catalog — without adding backend, payment UI, or breaking KYC / navigation / home shell.

## Decision: Approach B

| Option | Summary | Verdict |
|--------|---------|---------|
| A – Strings-only rename | Fast; no schema; drifts easily | Rejected |
| **B – Small domain catalog + wire UI** | One source of truth; stub UI reads it | **Chosen** |
| C – Full PDF §11 schema + payment step | Too large; risks KYC/payment breakage | Deferred |

## Scope

### In

- Vehicle model catalog: **Ellod Elite**, **Elacil 2.5** (30 Ah, 60 V each)
- Plan types: **WEEKLY**, **MONTHLY** with exact prices from KB
- Onboarding fee: **₹2,500**, non-refundable, new riders only (constant in catalog)
- Wire Home carousel + active vehicle labels + rental alert + wallet stubs
- Correct journey marketing copy (weekly & monthly only — no false “daily” plan claim)
- Unit tests for catalog + updated wallet repository assertions

### Out (explicit)

- Backend / DB / Retrofit / persistence
- Real payment gateway or KYC onboarding-fee payment step
- Telemetry, GPS, trips, support tickets, referral, gamification
- New screens or layout hierarchy changes (no new vehicle-card fields this pass)
- Changing `StationStatus` / battery-swap station models
- Repeat-customer detection logic (only document the fee rule in catalog)

## Canonical data (must match KB)

| Model | Battery | Voltage | Weekly | Monthly |
|-------|---------|---------|--------|---------|
| Ellod Elite | 30 Ah | 60 V | ₹1,549.00 | ₹5,900.00 |
| Elacil 2.5 | 30 Ah | 60 V | ₹1,799.00 | ₹6,500.00 |

| Item | Value |
|------|-------|
| Onboarding fee | ₹2,500.00 |
| Refundable | `false` |
| Applicable to | new riders only |

Per-day equivalents (derived, optional display helper — not stored as pricing source of truth):

| Model | Weekly ₹/day | Monthly ₹/day |
|-------|--------------|---------------|
| Ellod Elite | 221.29 | 196.67 |
| Elacil 2.5 | 257.00 | 216.67 |

## Architecture

```
RentalCatalog (object)          ← single source of truth
  ├── VehicleModelId
  ├── VehicleModelSpec
  ├── PlanType
  ├── OnboardingFee
  └── price(model, plan) / display helpers

HomeDashboardFragment           ← carousel from catalog.models()
fragment_home_dashboard.xml     ← active model / alert strings updated
WalletRepository                ← monthly rental + onboarding fee stubs
strings.xml                     ← display names, formatted amounts, journey copy
```

- **No API.** Same pattern as `WalletRepository` / `BatterySwapRepository`.
- **Money in catalog:** **`Long` paise only** (154900 = ₹1,549.00). UI amounts stay as formatted `strings.xml` values derived from those numbers.
- **Default demo vehicle:** Ellod Elite (active card when KYC approved).
- **Do not reuse** `StationStatus` for vehicle lifecycle. If an enum is added for future use, name it `VehicleStatus { AVAILABLE, RENTED, IN_SERVICE, RETIRED }` but **do not force it into UI** this pass (carousel keeps “Available” badge string).

## UI wiring rules (do not break)

| Surface | Change |
|---------|--------|
| Pending KYC carousel | Models → Ellod Elite, Elacil 2.5 (same card layout, same image drawable) |
| Approved active vehicle | Model label → Ellod Elite; plate stays `TS 01 EV 1234` (demo stub unchanged) |
| Rental due alert | Body amount → Ellod Elite monthly ₹5,900 (not ₹8,500) |
| Wallet monthly rental tx | Amount → ₹5,900 debit |
| Wallet deposit refund tx | **Remove**; replace with Onboarding Fee ₹2,500 debit, `isPending = true` |
| Journey bullet | “Weekly and monthly plans” (not “Daily, weekly, monthly”) |
| Details toast | May show Ah/V + prices from catalog (optional enhancement; keep toast pattern) |

## Wallet test contract

Existing `HomeRepositoriesTest.wallet_transactionsHaveUniqueIdsAndPendingCredit` expects a **pending CREDIT**. After this change there is no pending credit. **Update the test** to assert a pending **DEBIT** onboarding-fee transaction (and unique ids). Update `WalletViewModelTest` only if it hardcodes deposit/credit assumptions (today it only checks any pending exists).

## Target types (Kotlin sketch)

```kotlin
enum class VehicleModelId { ELLOD_ELITE, ELACIL_2_5 }

enum class PlanType { WEEKLY, MONTHLY }

enum class VehicleStatus { AVAILABLE, RENTED, IN_SERVICE, RETIRED } // defined, unused in UI this pass

data class VehicleModelSpec(
    val id: VehicleModelId,
    val displayNameRes: Int,
    val batteryAh: Int,      // 30
    val voltage: Int,        // 60
    val weeklyPricePaise: Long,
    val monthlyPricePaise: Long,
    val imageRes: Int        // R.drawable.scooter_onboarding for both
)

data class OnboardingFee(
    val amountPaise: Long,   // 250_000
    val refundable: Boolean, // false
)

object RentalCatalog {
    const val ONBOARDING_FEE_PAISE = 250_000L
    fun models(): List<VehicleModelSpec>
    fun model(id: VehicleModelId): VehicleModelSpec
    fun pricePaise(id: VehicleModelId, plan: PlanType): Long
    fun defaultActiveModel(): VehicleModelSpec // ELLOD_ELITE
}
```

## Non-goals reminder (from KB)

Incorrect stubs to eliminate on this pass:

- Fake models `EV-001` / `EV-002` / `Transcil S1 Pro` as if they were fleet SKUs
- Monthly rental ₹8,500
- Deposit Refund ₹2,000 pending credit

Keep unrelated stubs (swap fee ₹50, earnings, battery % 72, hubs) unchanged.

## Success criteria

1. Catalog unit tests assert exact paise for both models × both plans + onboarding fee.
2. Home carousel shows Ellod Elite and Elacil 2.5.
3. Active vehicle shows Ellod Elite.
4. Rental alert and wallet monthly rental show ₹5,900.
5. Wallet has onboarding fee ₹2,500 pending debit; no deposit-refund string.
6. Existing KYC / nav / battery / hubs tests still pass.
7. No new network or persistence dependencies.

## Risks

| Risk | Mitigation |
|------|------------|
| Tests asserting pending CREDIT | Update assertion intentionally |
| Float formatting inconsistency | Paise in catalog; formatted strings in `strings.xml` |
| Accidental layout breakage | No XML structure changes; string/content only |
| Scope creep into payment step | Explicitly out of scope |

---

**Next:** Implementation plan at `docs/superpowers/plans/2026-07-18-rental-vehicle-knowledge-base.md`.  
**Gate:** User reviews this spec + plan before any code changes.
