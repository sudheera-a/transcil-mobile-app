# Rental Vehicle Knowledge Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an in-memory rental catalog from the knowledge base and wire Home/Wallet/journey stubs to Ellod Elite / Elacil 2.5 pricing and the ₹2,500 onboarding fee — without breaking KYC, navigation, or home shell.

**Architecture:** Single `RentalCatalog` object holds fleet + plan prices + onboarding fee (amounts in paise). Home carousel and wallet stubs read from it (or from strings derived from the same numbers). No API, no new screens, no payment flow.

**Tech Stack:** Kotlin, XML Views, ViewBinding, MVVM, JUnit (existing Android unit test setup).

## Global Constraints

- Amounts must match KB exactly: Ellod Elite weekly 154900 paise / monthly 590000; Elacil 2.5 weekly 179900 / monthly 650000; onboarding 250000 paise, non-refundable.
- Do not add Retrofit, Room, DataStore, or payment SDKs.
- Do not change KYC Activities, Nav graph destinations, or bottom-nav behavior.
- Do not change `StationStatus` / battery-swap / hubs repositories.
- User-facing strings live in `strings.xml`.
- Prefer `Long` paise over `Double` rupees in catalog code.
- Default active demo model: Ellod Elite.
- Replace Deposit Refund stub; do not leave both deposit refund and onboarding fee.
- Do not commit unless the user asks.

---

## File structure

| File | Responsibility |
|------|----------------|
| Create: `app/src/main/java/com/example/transcilmobileapp/home/RentalCatalog.kt` | Enums + specs + catalog API |
| Create: `app/src/test/java/com/example/transcilmobileapp/home/RentalCatalogTest.kt` | Exact price / fee / model assertions |
| Modify: `app/src/main/res/values/strings.xml` | Model names, amounts, alert, wallet labels, journey copy |
| Modify: `app/src/main/java/.../home/HomeDashboardFragment.kt` | Carousel from `RentalCatalog.models()` |
| Modify: `app/src/main/java/.../home/WalletRepository.kt` | Monthly rental + onboarding fee stubs |
| Modify: `app/src/test/java/.../home/HomeRepositoriesTest.kt` | Pending debit (not credit) |
| Do not modify layouts | Active model / alert already bind string keys; Task 2 updates string values only |

**Unchanged:** KYC package, `HomeModels.kt` station/battery types, Nearby/Battery repos, Nav graph, Manifest.

---

### Task 1: RentalCatalog + failing tests (TDD)

**Files:**
- Create: `app/src/test/java/com/example/transcilmobileapp/home/RentalCatalogTest.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/home/RentalCatalog.kt`
- Modify: `app/src/main/res/values/strings.xml` (display name string resources referenced by catalog)

**Interfaces:**
- Consumes: `R` string resources for display names
- Produces:
  - `enum class VehicleModelId { ELLOD_ELITE, ELACIL_2_5 }`
  - `enum class PlanType { WEEKLY, MONTHLY }`
  - `enum class VehicleStatus { AVAILABLE, RENTED, IN_SERVICE, RETIRED }` (unused in UI this pass)
  - `data class VehicleModelSpec(id, displayNameRes, batteryAh, voltage, weeklyPricePaise, monthlyPricePaise, imageRes)`
  - `data class OnboardingFee(amountPaise: Long, refundable: Boolean)`
  - `object RentalCatalog` with:
    - `fun models(): List<VehicleModelSpec>`
    - `fun model(id: VehicleModelId): VehicleModelSpec`
    - `fun pricePaise(id: VehicleModelId, plan: PlanType): Long`
    - `fun defaultActiveModel(): VehicleModelSpec`
    - `fun onboardingFee(): OnboardingFee`
    - `const val ONBOARDING_FEE_PAISE = 250_000L`

- [ ] **Step 1: Add string resources for model display names** (needed before catalog compiles)

In `strings.xml`, add:

```xml
<string name="vehicle_model_ellod_elite">Ellod Elite</string>
<string name="vehicle_model_elacil_2_5">Elacil 2.5</string>
```

Catalog uses only these keys. Task 3 stops using `home_vehicle_model_ev1` / `ev2`; Task 5 deletes unused EV stub keys.

- [ ] **Step 2: Write failing `RentalCatalogTest`**

```kotlin
package com.example.transcilmobileapp.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RentalCatalogTest {

    @Test
    fun models_areExactlyEllodEliteAndElacil25() {
        val ids = RentalCatalog.models().map { it.id }
        assertEquals(
            listOf(VehicleModelId.ELLOD_ELITE, VehicleModelId.ELACIL_2_5),
            ids
        )
    }

    @Test
    fun specs_matchKnowledgeBaseBatteryAndVoltage() {
        RentalCatalog.models().forEach { spec ->
            assertEquals(30, spec.batteryAh)
            assertEquals(60, spec.voltage)
        }
    }

    @Test
    fun prices_matchKnowledgeBasePaise() {
        assertEquals(154_900L, RentalCatalog.pricePaise(VehicleModelId.ELLOD_ELITE, PlanType.WEEKLY))
        assertEquals(590_000L, RentalCatalog.pricePaise(VehicleModelId.ELLOD_ELITE, PlanType.MONTHLY))
        assertEquals(179_900L, RentalCatalog.pricePaise(VehicleModelId.ELACIL_2_5, PlanType.WEEKLY))
        assertEquals(650_000L, RentalCatalog.pricePaise(VehicleModelId.ELACIL_2_5, PlanType.MONTHLY))
    }

    @Test
    fun onboardingFee_is2500NonRefundable() {
        val fee = RentalCatalog.onboardingFee()
        assertEquals(250_000L, fee.amountPaise)
        assertFalse(fee.refundable)
        assertEquals(250_000L, RentalCatalog.ONBOARDING_FEE_PAISE)
    }

    @Test
    fun defaultActiveModel_isEllodElite() {
        assertEquals(VehicleModelId.ELLOD_ELITE, RentalCatalog.defaultActiveModel().id)
    }

    @Test
    fun vehicleStatus_enumHasFleetLifecycleValues() {
        val names = VehicleStatus.entries.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("AVAILABLE", "RENTED", "IN_SERVICE", "RETIRED")))
    }
}
```

- [ ] **Step 3: Run test — expect FAIL (catalog missing)**

```bash
cd /Users/sudheer/AndroidStudioProjects/TranscilMobileApp
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.home.RentalCatalogTest
```

Expected: compilation failure or test failure (class not found).

- [ ] **Step 4: Implement `RentalCatalog.kt`**

```kotlin
package com.example.transcilmobileapp.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.transcilmobileapp.R

enum class VehicleModelId { ELLOD_ELITE, ELACIL_2_5 }

enum class PlanType { WEEKLY, MONTHLY }

enum class VehicleStatus { AVAILABLE, RENTED, IN_SERVICE, RETIRED }

data class VehicleModelSpec(
    val id: VehicleModelId,
    @param:StringRes val displayNameRes: Int,
    val batteryAh: Int,
    val voltage: Int,
    val weeklyPricePaise: Long,
    val monthlyPricePaise: Long,
    @param:DrawableRes val imageRes: Int,
)

data class OnboardingFee(
    val amountPaise: Long,
    val refundable: Boolean,
)

object RentalCatalog {
    const val ONBOARDING_FEE_PAISE = 250_000L

    private val specs = listOf(
        VehicleModelSpec(
            id = VehicleModelId.ELLOD_ELITE,
            displayNameRes = R.string.vehicle_model_ellod_elite,
            batteryAh = 30,
            voltage = 60,
            weeklyPricePaise = 154_900L,
            monthlyPricePaise = 590_000L,
            imageRes = R.drawable.scooter_onboarding,
        ),
        VehicleModelSpec(
            id = VehicleModelId.ELACIL_2_5,
            displayNameRes = R.string.vehicle_model_elacil_2_5,
            batteryAh = 30,
            voltage = 60,
            weeklyPricePaise = 179_900L,
            monthlyPricePaise = 650_000L,
            imageRes = R.drawable.scooter_onboarding,
        ),
    )

    fun models(): List<VehicleModelSpec> = specs

    fun model(id: VehicleModelId): VehicleModelSpec =
        specs.first { it.id == id }

    fun pricePaise(id: VehicleModelId, plan: PlanType): Long {
        val spec = model(id)
        return when (plan) {
            PlanType.WEEKLY -> spec.weeklyPricePaise
            PlanType.MONTHLY -> spec.monthlyPricePaise
        }
    }

    fun defaultActiveModel(): VehicleModelSpec = model(VehicleModelId.ELLOD_ELITE)

    fun onboardingFee(): OnboardingFee =
        OnboardingFee(amountPaise = ONBOARDING_FEE_PAISE, refundable = false)
}
```

- [ ] **Step 5: Run `RentalCatalogTest` — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.home.RentalCatalogTest
```

- [ ] **Step 6: Stop for review gate** (do not commit unless user asked)

---

### Task 2: Strings — prices, wallet labels, journey copy, home labels

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: none
- Produces: string keys used by layout / WalletRepository / Home

- [ ] **Step 1: Update / add these strings** (exact values)

| Key | New value |
|-----|-----------|
| `vehicle_model_ellod_elite` | `Ellod Elite` (if not already from Task 1) |
| `vehicle_model_elacil_2_5` | `Elacil 2.5` |
| `home_active_model` | `Ellod Elite` |
| `home_rental_alert_body` | `Your monthly rental payment of ₹5,900 is due on May 20, 2026` |
| `wallet_tx_monthly_rental` | keep label `Monthly Rental` |
| `wallet_tx_amount_8500` | **Rename** to `wallet_tx_amount_5900` = `₹5900`; update `WalletRepository` |
| `wallet_tx_deposit_refund` | **Rename** to `wallet_tx_onboarding_fee` = `Onboarding Fee` |
| `wallet_tx_amount_2000` | **Rename** to `wallet_tx_amount_2500` = `₹2500` |
| `journey_rent_ev_bullet_2` | `Weekly and monthly plans` |

After Task 3, Task 5 deletes unused keys: `home_vehicle_model_ev1`, `home_vehicle_model_ev2`, `home_vehicle_model` (if unused), and any leftover old wallet keys.

- [ ] **Step 2: Grep for old keys** to ensure no dangling references

```bash
rg "wallet_tx_deposit_refund|wallet_tx_amount_2000|wallet_tx_amount_8500|home_vehicle_model_ev1|EV-001|Deposit Refund|8500|Transcil S1" app/
```

Expected after Tasks 2–3: no remaining product references to those stubs (test source may mention in comments only if needed).

- [ ] **Step 3: Stop for review gate**

---

### Task 3: Wire Home carousel + active model

**Files:**
- Modify: `app/src/main/java/com/example/transcilmobileapp/home/HomeDashboardFragment.kt` (`setupVehicleCarousel`)
- Modify: `app/src/main/res/values/strings.xml` (`home_active_model` already Ellod Elite via Task 2)
- Do not modify XML layouts

**Interfaces:**
- Consumes: `RentalCatalog.models()`, `VehicleModelSpec.displayNameRes`, `imageRes`
- Produces: carousel cards for both fleet models

- [ ] **Step 1: Replace hardcoded string-res list with catalog**

In `setupVehicleCarousel()` use `RentalCatalog.models()`. Details/Select keep **simple toasts** (model name only) — no new format strings.

```kotlin
private fun setupVehicleCarousel() {
    val row = binding.vehicleCarouselRow
    row.removeAllViews()
    val vehicles = RentalCatalog.models()
    val cardWidth = resources.getDimensionPixelSize(R.dimen.home_vehicle_card_width)
    val gap = resources.getDimensionPixelSize(R.dimen.home_vehicle_card_gap)
    val inflater = LayoutInflater.from(requireContext())

    vehicles.forEachIndexed { index, spec ->
        val cardBinding = ItemHomeVehicleCardBinding.inflate(inflater, row, false)
        cardBinding.tvVehicleModel.setText(spec.displayNameRes)
        cardBinding.tvVehicleType.setText(R.string.home_vehicle_type)
        cardBinding.ivVehicleImage.setImageResource(spec.imageRes)

        val lp = ViewGroup.MarginLayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (index < vehicles.lastIndex) {
            lp.marginEnd = gap
        }
        cardBinding.root.layoutParams = lp

        val modelName = getString(spec.displayNameRes)
        cardBinding.btnVehicleDetails.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.home_vehicle_details) + " · " + modelName,
                Toast.LENGTH_SHORT
            ).show()
        }
        cardBinding.btnVehicleSelect.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.home_vehicle_selected_stub, modelName),
                Toast.LENGTH_SHORT
            ).show()
        }
        row.addView(cardBinding.root)
    }
}
```

- [ ] **Step 2: Confirm `home_active_model` is `Ellod Elite`** (layout already binds `@string/home_active_model`)

- [ ] **Step 3: Manual sanity** — build:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: SUCCESS

- [ ] **Step 4: Stop for review gate**

---

### Task 4: Wire WalletRepository + fix tests

**Files:**
- Modify: `app/src/main/java/com/example/transcilmobileapp/home/WalletRepository.kt`
- Modify: `app/src/test/java/com/example/transcilmobileapp/home/HomeRepositoriesTest.kt`
- Verify: `app/src/test/java/com/example/transcilmobileapp/home/WalletViewModelTest.kt` (likely unchanged)

**Interfaces:**
- Consumes: string keys `wallet_tx_onboarding_fee`, `wallet_tx_amount_2500`, `wallet_tx_amount_5900` (or updated keys from Task 2)
- Produces: same `List<WalletTransaction>` shape; 5 txs; one pending DEBIT onboarding fee

- [ ] **Step 1: Update `WalletRepository.recentTransactions()`**

Replace monthly amount ref with ₹5900 key.  
Replace deposit refund entry with:

```kotlin
WalletTransaction(
    id = "5",
    titleRes = R.string.wallet_tx_onboarding_fee,
    timeRes = R.string.wallet_tx_time_5,
    amountRes = R.string.wallet_tx_amount_2500,
    type = TransactionType.DEBIT,
    isPending = true
)
```

Keep ids unique (`"1"`…`"5"`). Keep daily earnings + swap fee stubs unchanged.

- [ ] **Step 2: Update `HomeRepositoriesTest`**

Replace:

```kotlin
assertTrue(txs.any { it.isPending && it.type == TransactionType.CREDIT })
```

With:

```kotlin
assertTrue(txs.any { it.isPending && it.type == TransactionType.DEBIT })
assertTrue(
    txs.any {
        it.isPending &&
            it.type == TransactionType.DEBIT &&
            it.titleRes == R.string.wallet_tx_onboarding_fee &&
            it.amountRes == R.string.wallet_tx_amount_2500
    }
)
```

Add import for `R` if needed.

- [ ] **Step 3: Run home + wallet unit tests**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.home.*
```

Expected: all PASS

- [ ] **Step 4: Stop for review gate**

---

### Task 5: Full verification + cleanup

**Files:**
- Possibly delete unused strings: `home_vehicle_model_ev1`, `home_vehicle_model_ev2`, `home_vehicle_model`, `wallet_tx_deposit_refund`, `wallet_tx_amount_2000`, `wallet_tx_amount_8500` **only if** `rg` shows zero references

- [ ] **Step 1: Grep cleanup**

```bash
rg "EV-001|EV-002|Transcil S1|Deposit Refund|wallet_tx_deposit|amount_8500|amount_2000|8,500|₹8500|₹2000" app/ docs/superpowers/plans/2026-07-18-rental-vehicle-knowledge-base.md
```

Product code under `app/` must be clean. Docs/plan may still mention old values historically — that is OK.

- [ ] **Step 2: Run full unit test suite**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all PASS. If an unrelated test fails, fix only if caused by this change; otherwise report separately.

- [ ] **Step 3: Compile debug APK / Kotlin**

```bash
./gradlew :app:assembleDebug
```

Expected: SUCCESS

- [ ] **Step 4: Manual checklist (device/emulator)**

1. Open home as PENDING KYC → carousel shows **Ellod Elite** and **Elacil 2.5**
2. Open home as APPROVED → active model **Ellod Elite**; alert mentions **₹5,900**
3. Wallet → Monthly Rental **₹5900**; Onboarding Fee **₹2500** with PENDING; no Deposit Refund
4. Bottom nav still switches Home / Map / Battery / Wallet / Profile
5. KYC progress still opens from pending profile card

- [ ] **Step 5: Do not commit** unless user explicitly asks

---

## Out-of-scope follow-ups (do not implement in this plan)

1. KYC payment step for onboarding fee (`JourneyType.RENT_EV` + `is_new_rider`)
2. Plan selection UI / mid-cycle upgrade
3. Vehicle telemetry / `VehicleStatus` UI binding
4. GST/tax fields, km caps, auto-renewal
5. Real payment status enum beyond wallet `isPending`

---

## Self-review checklist (for planner)

- [x] Exact paise values match PDF
- [x] Deposit refund removal + test update called out
- [x] KYC/nav non-touch listed
- [x] TDD order for catalog
- [x] No commit without user request
- [x] Success criteria match design spec
- [x] Ambiguities locked: Long paise only; simple toast; rename wallet keys; no layout XML edits; plate unchanged

## Plan check log (2026-07-18)

| Check | Result |
|-------|--------|
| PDF prices ↔ plan paise | Match (1549/5900, 1799/6500, fee 2500) |
| Spec vs plan scope | Aligned (Approach B, no payment/KYC step) |
| Breaking wallet test | Explicitly updated (pending CREDIT → pending DEBIT) |
| Layout risk | None — string values + Fragment carousel only |
| Placeholder / OR branches | Removed in self-review pass |
| Implementation | **Done** — `:app:testDebugUnitTest` + `:app:assembleDebug` PASS |
