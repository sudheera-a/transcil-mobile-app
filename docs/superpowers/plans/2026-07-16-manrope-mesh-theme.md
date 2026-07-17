# Manrope + Mesh Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply Manrope typography and day/night mesh backgrounds across splash and all existing screens.

**Architecture:** Theme-driven resources — mesh bitmaps in `drawable-nodpi`, day/night drawable aliases, Manrope font family XML, color tokens, layouts updated to shared `@drawable/screen_background` and theme colors.

**Tech Stack:** Native Android (Kotlin + XML Views), Material3 DayNight, Android font resources.

**Spec:** `docs/superpowers/specs/2026-07-16-manrope-mesh-theme-design.md`

## Global Constraints

- Splash title: Manrope Bold, 27sp, centered “Transcil”
- Light mesh + dark mesh from provided Cursor assets (convert dark JPEG → PNG)
- Bitmaps in `drawable-nodpi`; background via `<bitmap gravity="fill">`
- Color tokens: `text_primary`, `text_secondary`, `text_muted`, `brand_primary`, `text_on_brand`
- Out of scope: Home, Skip wiring, SplashScreen API, new illustrations, input restyle
- Branch: `feature/manrope-mesh-theme`

---

### Task 1: Import mesh assets + background drawables

**Files:**
- Create: `app/src/main/res/drawable-nodpi/bg_mesh_light.png`
- Create: `app/src/main/res/drawable-nodpi/bg_mesh_dark.png`
- Create: `app/src/main/res/values/drawables.xml`
- Create: `app/src/main/res/values-night/drawables.xml`
- Create: `app/src/main/res/drawable/screen_background.xml`

**Interfaces:**
- Produces: `@drawable/screen_background`, `@drawable/screen_background_src`, `@drawable/bg_mesh_light`, `@drawable/bg_mesh_dark`

- [ ] **Step 1: Create nodpi dir and copy/convert assets**

```bash
mkdir -p app/src/main/res/drawable-nodpi
cp "/Users/sudheer/.cursor/projects/Users-sudheer-AndroidStudioProjects-TranscilMobileApp/assets/SplashScreen-bg-46ee53db-2ece-45ea-9c72-30394bbecfce.png" \
  app/src/main/res/drawable-nodpi/bg_mesh_light.png
sips -s format png \
  "/Users/sudheer/.cursor/projects/Users-sudheer-AndroidStudioProjects-TranscilMobileApp/assets/SplashScreen-cae2a970-a140-4be9-8f1e-03f096dff4e1.png" \
  --out app/src/main/res/drawable-nodpi/bg_mesh_dark.png
file app/src/main/res/drawable-nodpi/bg_mesh_*.png
```

Expected: both report PNG image data.

- [ ] **Step 2: Add day/night source aliases**

`app/src/main/res/values/drawables.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <drawable name="screen_background_src">@drawable/bg_mesh_light</drawable>
</resources>
```

`app/src/main/res/values-night/drawables.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <drawable name="screen_background_src">@drawable/bg_mesh_dark</drawable>
</resources>
```

- [ ] **Step 3: Add fill bitmap wrapper**

`app/src/main/res/drawable/screen_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/screen_background_src"
    android:gravity="fill" />
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable-nodpi app/src/main/res/drawable/screen_background.xml \
  app/src/main/res/values/drawables.xml app/src/main/res/values-night/drawables.xml \
  docs/superpowers/specs/2026-07-16-manrope-mesh-theme-design.md
git commit -m "Add day/night mesh screen backgrounds."
```

---

### Task 2: Manrope font family + color/theme tokens

**Files:**
- Create: `app/src/main/res/font/manrope_regular.ttf`
- Create: `app/src/main/res/font/manrope_bold.ttf`
- Create: `app/src/main/res/font/manrope_family.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values-night/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/res/values/strings.xml` (`GET STARTED`)

**Interfaces:**
- Consumes: `@drawable/screen_background`
- Produces: `@font/manrope_family`, color tokens, themed `Theme.TranscilMobileApp`

- [ ] **Step 1: Download Manrope Regular + Bold**

```bash
mkdir -p app/src/main/res/font
# Use Google Fonts github raw TTFs
curl -L -o app/src/main/res/font/manrope_regular.ttf \
  "https://github.com/google/fonts/raw/main/ofl/manrope/Manrope%5Bwght%5D.ttf"
```

If variable font only: download static cuts from fonts.google.com zip, or use:

```bash
curl -L -o /tmp/manrope.zip "https://fonts.google.com/download?family=Manrope"
unzip -l /tmp/manrope.zip
# copy static Regular and Bold into res/font with legal names
```

Prefer static files `Manrope-Regular.ttf` → `manrope_regular.ttf`, `Manrope-Bold.ttf` → `manrope_bold.ttf`.

- [ ] **Step 2: Create font family**

`app/src/main/res/font/manrope_family.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:app="http://schemas.android.com/apk/res-auto">
    <font
        app:font="@font/manrope_regular"
        app:fontStyle="normal"
        app:fontWeight="400" />
    <font
        app:font="@font/manrope_bold"
        app:fontStyle="normal"
        app:fontWeight="700" />
</font-family>
```

- [ ] **Step 3: Colors**

`values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="text_primary">#1A1A1A</color>
    <color name="text_secondary">#666666</color>
    <color name="text_muted">#999999</color>
    <color name="brand_primary">#8BC34A</color>
    <color name="text_on_brand">#FFFFFF</color>
</resources>
```

`values-night/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="text_primary">#FFFFFF</color>
    <color name="text_secondary">#B3B3B3</color>
    <color name="text_muted">#8A8A8A</color>
    <color name="brand_primary">#8BC34A</color>
    <color name="text_on_brand">#FFFFFF</color>
</resources>
```

- [ ] **Step 4: Themes**

`values/themes.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.TranscilMobileApp" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:fontFamily">@font/manrope_family</item>
        <item name="fontFamily">@font/manrope_family</item>
        <item name="android:windowBackground">@drawable/screen_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="colorPrimary">@color/brand_primary</item>
    </style>
    <style name="Theme.TranscilMobileApp" parent="Base.Theme.TranscilMobileApp" />
</resources>
```

`values-night/themes.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.TranscilMobileApp" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:fontFamily">@font/manrope_family</item>
        <item name="fontFamily">@font/manrope_family</item>
        <item name="android:windowBackground">@drawable/screen_background</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="colorPrimary">@color/brand_primary</item>
    </style>
</resources>
```

- [ ] **Step 5: Fix string**

In `strings.xml`: `get_started` → `GET STARTED`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/font app/src/main/res/values app/src/main/res/values-night
git commit -m "Add Manrope font family and day/night theme tokens."
```

---

### Task 3: Update all 7 layouts

**Files:**
- Modify: `activity_main.xml`, `activity_onboarding1–4.xml`, `activity_welcome.xml`, `activity_verify_otp.xml`

**Interfaces:**
- Consumes: `@drawable/screen_background`, `@color/text_*`, `@color/brand_primary`, `@color/text_on_brand`, `@font/manrope_family`

- [ ] **Step 1: Splash `activity_main.xml`**

- `android:background="@drawable/screen_background"`
- Remove `android:fontFamily="sans-serif-condensed"` (theme provides Manrope)
- `android:textSize="27sp"`, `android:lineSpacingExtra="5sp"` (≈32 line height)
- `android:textStyle="bold"`, `android:textColor="@color/text_primary"`

- [ ] **Step 2: Onboarding + Welcome + OTP layouts**

For each:
- Root background → `@drawable/screen_background`
- `@color/brand_dark` → `@color/text_primary`
- `#666666` → `@color/text_secondary`
- `#999999` → `@color/text_muted`
- `#8BC34A` backgroundTint → `@color/brand_primary`
- Button `#FFFFFF` text → `@color/text_on_brand`

- [ ] **Step 3: Build to verify resources**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout
git commit -m "Apply mesh background and theme colors to all screens."
```

---

### Task 4: Verify + plan doc commit

**Files:**
- Create: `docs/superpowers/plans/2026-07-16-manrope-mesh-theme.md` (this file)

- [ ] **Step 1: Confirm no leftover splash_background / brand_dark / hardcoded greys in layouts**

```bash
rg "splash_background|brand_dark|#666666|#999999|#8BC34A" app/src/main/res/layout
```

Expected: no matches (dots may still use hex in drawable XML — OK).

- [ ] **Step 2: Commit plan + any remaining docs**

```bash
git add docs/superpowers
git commit -m "Add Manrope mesh theme implementation plan."
```

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| Mesh light/dark assets | 1 |
| screen_background fill | 1 |
| Manrope family | 2 |
| Color tokens + themes + status bar | 2 |
| GET STARTED | 2 |
| 7 layouts + splash 27sp | 3 |
| Build verification | 3–4 |
