# Home Shell + Nearby Hubs + Battery Swap Implementation Plan

> **For agentic workers:** Implement inline in this session. Checkbox tracking optional.

**Goal:** Convert post-KYC Home into a NavHost shell and ship Nearby Hubs + Battery Swap fragments matching screenshots, without breaking KYC entry.

**Architecture:** `HomeDashboardActivity` hosts `NavHostFragment` + shared bottom nav. Feature fragments own layouts/VMs; stub repos for stations/swaps.

**Tech Stack:** Kotlin, XML Views, ViewBinding/DataBinding, Navigation Component, LiveData, RecyclerView

## Global Constraints

- No Compose; no Maps SDK; no camera/API
- Colors/dimens tokens only; strings in `strings.xml`
- Preserve `HomeDashboardActivity.createIntent` + `KycNavigator.openHomeDashboard`
- Wallet/Profile remain stubs inside the shell

## File map

| File | Responsibility |
|------|----------------|
| `gradle/libs.versions.toml` + `app/build.gradle.kts` | navigation, fragment, recyclerview |
| `res/navigation/home_nav_graph.xml` | destinations |
| `layout/activity_home_dashboard.xml` | shell: NavHost + bottom nav |
| `layout/fragment_home_dashboard.xml` | extracted home scroll content |
| `layout/fragment_nearby_hubs.xml` + item | Map tab UI |
| `layout/fragment_battery_swap.xml` + item | Battery tab UI |
| `layout/fragment_home_stub.xml` | Wallet/Profile stub |
| `home/*Fragment.kt`, VMs, models, repos | feature logic |
| `HomeDashboardActivity.kt` | shell wiring |
| `strings/colors/dimens/drawables` | tokens + icons |

## Tasks

1. Add Navigation / Fragment / RecyclerView deps  
2. Extract home content → fragment; shell activity layout  
3. Nearby Hubs UI + stub repo + adapter  
4. Battery Swap UI + stub repo + adapter  
5. Wire nav graph, bottom nav, quick actions, Intent start tab  
6. AssembleDebug verify  

## Verification

- KYC → Home still opens  
- Map / Battery tabs show new screens with correct selected nav  
- Quick actions navigate to those tabs  
- Wallet/Profile show stub, no crash  
- Light + dark theme resolve colors from tokens  
