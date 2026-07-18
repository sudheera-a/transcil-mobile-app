# Home Shell + Nearby Hubs + Battery Swap Design

**Date:** 2026-07-18  
**Status:** Approved (Approach B)

## Goal

Wire Figma frames **Nearby Hubs** and **Battery Swap** into the post-KYC app using a production-style **single Activity shell + Fragments + Navigation Component**, without breaking KYC/onboarding or the existing Home dashboard UI.

## Decision: Approach B

| Option | Summary | Verdict |
|--------|---------|---------|
| A – Separate Activities + duplicated bottom nav | Fast, matches current Activity style | Rejected for tabs (stack churn, duplicated chrome) |
| **B – Home shell Activity + Fragments** | One bottom nav, NavHost destinations | **Chosen** |
| C – Screens without bottom nav | Fastest content-only | Rejected (breaks frames) |

**Why B:** Industry standard for bottom tabs; preserves tab state; deep-link ready; backend work stays in ViewModel → Repository regardless of Fragment vs Activity.

**Scope boundary:** Only the post-login home shell migrates to Fragments. Onboarding + KYC remain Activities.

## Architecture

```
KycPending / KycApproved
        │
        ▼
HomeDashboardActivity  (shell: NavHost + bottom nav)
        │
        ├── HomeDashboardFragment      (existing home UI)
        ├── NearbyHubsFragment         (Map tab)
        ├── BatterySwapFragment        (Battery tab)
        ├── WalletFragment             (Wallet tab)
        ├── ProfileFragment            (Profile tab)
        └── SettingsFragment           (Profile sub-screen; bottom nav hidden)
```

- **Activity:** owns bottom nav selection sync + `NavController`; passes `KycStatus` / start tab via Intent extras.
- **Fragments:** thin UI; observe feature ViewModels.
- **Data:** stub repositories (`NearbyHubsRepository`, `BatterySwapRepository`) so API swap later is a drop-in.
- **Map:** placeholder surface (no Google Maps SDK yet). Pins are decorative until Maps integration.
- **Navigate / Scan QR:** stub Toast / navigation event (no external maps/camera APIs yet).

## Entry points (must not break)

| From | Behavior |
|------|----------|
| `KycNavigator.openHomeDashboard` | Unchanged → `HomeDashboardActivity` → Home tab |
| Home quick action Battery Swap | Navigate to Battery destination |
| Home quick action Nearby Hubs | Navigate to Map destination |
| Home row Swap Battery / Navigate | Battery / Map respectively (Navigate → Map) |
| Bottom nav Map / Battery | Real destinations |
| Bottom nav Wallet / Profile | Stub fragments (same “coming soon” copy) |
| Back on Map/Battery header | `navigateUp` / pop to Home |

## UI / design system

- Tokens only: `dimens.xml`, `colors.xml` / `values-night`, shared drawables (`bg_home_card`, badges, outline buttons).
- Light + dark via existing DayNight theme (frames are dark; light uses `home_*` tokens).
- Strings in `strings.xml` only.
- No Compose.

## Backend-ready shape

```
Fragment → ViewModel → Repository (stub now) → Api later
```

Models: `SwapStation`, `StationStatus`, `BatteryStatus`, `RecentSwap`.  
UI state: lists + scalars on LiveData; later wrap in `Loading | Success | Error` if needed.

## Non-goals

- Google Maps SDK / real GPS
- Camera QR scanning
- Wallet / Profile real screens
- API / persistence
- Refactoring KYC flows

## How we built it (for review / seniors)

### Tradeoffs considered

| Approach | Pros | Cons |
|----------|------|------|
| A – Activity per tab | Matches older KYC pattern | Duplicated bottom nav; Activity stack noise; harder deep links |
| **B – Shell + Fragments + Nav** | Production tab standard; one chrome; state/save restore; backend-agnostic UI | One-time home refactor |
| C – No bottom nav | Fastest | Breaks design frames |

### Why B

1. Bottom tabs are a **shell concern**, not a full-screen Activity concern.
2. Google’s Navigation Component + single host is the durable pattern for Map/Battery/Wallet/Profile.
3. KYC/onboarding stay Activities — we did **not** rewrite the whole app.
4. Stub repositories mean backend is a replace of `NearbyHubsRepository` / `BatterySwapRepository`, not a UI rewrite.

### What shipped

- `HomeDashboardActivity` = NavHost + bottom nav
- `HomeDashboardFragment` = previous home scroll UI (behavior preserved)
- `NearbyHubsFragment` + `BatterySwapFragment` = screenshot frames
- Wallet/Profile = stub fragments inside the same shell
- Quick actions (Battery Swap / Nearby Hubs / Navigate) route into the shell tabs
- `./gradlew :app:assembleDebug` verified after change
