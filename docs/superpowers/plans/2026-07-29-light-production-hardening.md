# Light Production Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship light production hardening for TranscilMobileApp: R8 release minify, package `in.transcil.rider`, safer network/logging, encrypted bank draft, Help HTML escape, demo cleanup, backup excludes, and basic CI.

**Architecture:** Keep existing MVVM / ViewBinding / singleton stores. Reuse `TokenStore` EncryptedSharedPreferences pattern for `KycLocalStore`. Escape non-HTML Help fields in a one-liner helper. Wire debug `BASE_URL` from `local.properties` only. No DI framework, no Compose, no KYC accordion split.

**Tech Stack:** Kotlin, AGP 9.2, R8, Retrofit/Gson/OkHttp, security-crypto, JUnit, GitHub Actions.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-29-light-production-hardening-design.md`
- Package / applicationId / namespace: `in.transcil.rider`
- Release BASE_URL: `https://api.transcil.in/`
- No new dependencies unless unavoidable
- No commit unless user explicitly asks (skip all “Commit” steps until then)
- P1 items from the design are out of scope

---

### Task 1: Remove dead demo scaffolding

**Files:**
- Delete: `app/src/main/java/com/example/transcilmobileapp/data/network/DemoApi.kt`
- Delete: `app/src/main/java/com/example/transcilmobileapp/data/model/PostDto.kt`
- Delete: `app/src/main/java/com/example/transcilmobileapp/repository/DemoRepository.kt`
- Delete: `app/src/main/java/com/example/transcilmobileapp/demo/DemoViewModel.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/network/ApiClient.kt` — remove `demoApi`

**Interfaces:**
- Consumes: nothing
- Produces: leaner `ApiClient` with only `transcilApi`

- [ ] **Step 1: Confirm nothing else references demo types**

Run:

```bash
rg -n 'DemoApi|DemoRepository|DemoViewModel|PostDto|demoApi' --glob '*.kt'
```

Expected: only the files listed above (plus `ApiClient.demoApi`).

- [ ] **Step 2: Delete demo files and strip ApiClient**

`ApiClient.kt` should end as:

```kotlin
object ApiClient {

    private val okHttp: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(AuthInterceptor())
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        authenticator(TokenAuthenticator())
    }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val transcilApi: TranscilApi = retrofit.create(TranscilApi::class.java)
}
```

Delete the four demo files.

- [ ] **Step 3: Skip commit** (unless user asks)

---

### Task 2: HelpCenterHtml escape + unit test

**Files:**
- Modify: `app/src/main/java/com/example/transcilmobileapp/home/HelpCenterHtml.kt` (path becomes `in/transcil/rider/...` after Task 4 — edit in place before rename, or after; either OK)
- Modify: `app/src/test/java/com/example/transcilmobileapp/home/HelpCenterHtmlTest.kt`

**Interfaces:**
- Consumes: `HelpCenterDto`
- Produces: `HelpCenterHtml.build` returns HTML with escaped text fields; `body_html` unchanged

- [ ] **Step 1: Extend failing test for script injection in title**

Add to `HelpCenterHtmlTest`:

```kotlin
@Test
fun build_escapesScriptInTitle() {
    val html = HelpCenterHtml.build(
        HelpCenterDto(
            schema_version = "help_center_v1",
            support_email = "a<script>x</script>@t.com",
            support_mobile = null,
            topics = listOf(
                HelpTopicDto(
                    id = "t",
                    title = "<script>alert(1)</script>",
                    articles = listOf(
                        HelpArticleDto(
                            id = "a",
                            title = "Ok",
                            body_html = "<p>Trusted</p>",
                        )
                    ),
                )
            ),
            version = 1,
        )
    )
    assertTrue(!html.contains("<script>alert(1)</script>"))
    assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
    assertTrue(html.contains("<p>Trusted</p>"))
    assertTrue(html.contains("a&lt;script&gt;x&lt;/script&gt;@t.com"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.example.transcilmobileapp.home.HelpCenterHtmlTest.build_escapesScriptInTitle'`

Expected: FAIL (raw `<script>` still present).

- [ ] **Step 3: Escape text fields in HelpCenterHtml**

```kotlin
object HelpCenterHtml {

    fun build(dto: HelpCenterDto): String {
        val sb = StringBuilder()
        dto.support_email?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Email:</b> ").append(escape(it)).append("</p>")
        }
        dto.support_mobile?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Mobile:</b> ").append(escape(it)).append("</p>")
        }
        dto.topics.orEmpty().forEach { topic ->
            topic.title?.takeIf { it.isNotBlank() }?.let {
                sb.append("<h2>").append(escape(it)).append("</h2>")
            }
            topic.articles.orEmpty().forEach { article ->
                article.title?.takeIf { it.isNotBlank() }?.let {
                    sb.append("<h3>").append(escape(it)).append("</h3>")
                }
                // Trusted server HTML by product choice for this hardening pass.
                sb.append(article.body_html.orEmpty())
            }
        }
        return sb.toString()
    }

    private fun escape(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
```

- [ ] **Step 4: Run HelpCenterHtmlTest**

Run: `./gradlew :app:testDebugUnitTest --tests '*HelpCenterHtmlTest'`

Expected: PASS (both tests).

- [ ] **Step 5: Skip commit** (unless user asks)

---

### Task 3: Encrypt KycLocalStore + backup excludes

**Files:**
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/local/KycLocalStore.kt`
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`

**Interfaces:**
- Consumes: `Context`, `BankDraft` (unchanged public API: `init`, `saveBank`, `loadBank`, `clear`)
- Produces: encrypted prefs file still named `transcil_kyc_local`

- [ ] **Step 1: Switch KycLocalStore to EncryptedSharedPreferences**

Replace `init` body to mirror `TokenStore` (keep PREFS name `transcil_kyc_local`):

```kotlin
/**
 * Survives process death for temporary local-only KYC (bank IFSC path).
 * Encrypted at rest (account/IFSC) — same security-crypto pattern as [TokenStore].
 * Cleared on logout via [clear].
 */
object KycLocalStore {
    // ... same KEY_* constants ...

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            app,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    // saveBank / loadBank / clear unchanged
}
```

Add imports for `EncryptedSharedPreferences` and `MasterKey`.

- [ ] **Step 2: Exclude KYC prefs from backup**

`backup_rules.xml`:

```xml
<full-backup-content>
    <exclude domain="sharedpref" path="transcil_tokens.xml" />
    <exclude domain="sharedpref" path="transcil_tokens" />
    <exclude domain="sharedpref" path="transcil_kyc_local.xml" />
    <exclude domain="sharedpref" path="transcil_kyc_local" />
</full-backup-content>
```

`data_extraction_rules.xml` — add the same two `transcil_kyc_local` excludes under both `cloud-backup` and `device-transfer`.

- [ ] **Step 3: Skip commit** (unless user asks)

---

### Task 4: Rename package to `in.transcil.rider`

**Files:**
- Modify: `app/build.gradle.kts` (`namespace`, `applicationId`)
- Move tree: `app/src/main/java/com/example/transcilmobileapp/` → `app/src/main/java/in/transcil/rider/`
- Move tree: `app/src/test/java/com/example/transcilmobileapp/` → `app/src/test/java/in/transcil/rider/`
- Move tree: `app/src/androidTest/java/com/example/transcilmobileapp/` → `app/src/androidTest/java/in/transcil/rider/`
- Modify: all Kotlin `package` / `import` lines under those trees
- Modify: `app/src/main/res/navigation/home_nav_graph.xml` (and any other XML with fully-qualified class names)
- Modify: `ExampleInstrumentedTest` expected package string

**Interfaces:**
- Consumes: previous `com.example.transcilmobileapp.*`
- Produces: `in.transcil.rider.*` + applicationId `in.transcil.rider`

- [ ] **Step 1: Update Gradle identity**

In `app/build.gradle.kts`:

```kotlin
namespace = "in.transcil.rider"
// ...
applicationId = "in.transcil.rider"
```

- [ ] **Step 2: Move source trees**

```bash
mkdir -p app/src/main/java/in/transcil
mkdir -p app/src/test/java/in/transcil
mkdir -p app/src/androidTest/java/in/transcil
git mv app/src/main/java/com/example/transcilmobileapp app/src/main/java/in/transcil/rider
git mv app/src/test/java/com/example/transcilmobileapp app/src/test/java/in/transcil/rider
git mv app/src/androidTest/java/com/example/transcilmobileapp app/src/androidTest/java/in/transcil/rider
# remove empty com/example parents if left behind
rmdir -p app/src/main/java/com/example 2>/dev/null || true
rmdir -p app/src/test/java/com/example 2>/dev/null || true
rmdir -p app/src/androidTest/java/com/example 2>/dev/null || true
```

- [ ] **Step 3: Rewrite package/import references in app sources**

```bash
# Kotlin + navigation XML under app/
rg -l 'com\.example\.transcilmobileapp' app --glob '*.{kt,xml}' | while read -r f; do
  sed -i '' 's/com\.example\.transcilmobileapp/in.transcil.rider/g' "$f"
done
```

Update instrumented test assertion to `"in.transcil.rider"`.

Do **not** bulk-rewrite historical docs under `docs/` in this task (optional later); code + Gradle + nav XML are required.

- [ ] **Step 4: Smoke-compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Skip commit** (unless user asks)

**Migration note (report to user):** uninstall old `com.example.transcilmobileapp` before installing the new build.

---

### Task 5: Network config + debug BASE_URL override

**Files:**
- Create: `app/src/main/res/xml/network_security_config.xml`
- Create: `app/src/debug/res/xml/network_security_config.xml` (debug cleartext for local gateway)
- Modify: `app/src/main/AndroidManifest.xml` — `android:networkSecurityConfig`
- Modify: `app/build.gradle.kts` — debug BASE_URL from `local.properties`

**Interfaces:**
- Consumes: optional `transcil.baseUrl` in `local.properties`
- Produces: `BuildConfig.BASE_URL` (release always prod HTTPS)

- [ ] **Step 1: Main (release) network security config**

`app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

- [ ] **Step 2: Debug override allowing cleartext to emulator/local**

`app/src/debug/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
    </domain-config>
</network-security-config>
```

- [ ] **Step 3: Point manifest at config**

On `<application>` add:

```xml
android:networkSecurityConfig="@xml/network_security_config"
```

Keep `android:usesCleartextTraffic="false"`.

- [ ] **Step 4: Wire debug BASE_URL from local.properties**

In `app/build.gradle.kts` (concept):

```kotlin
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val prodBaseUrl = "https://api.transcil.in/"
val debugBaseUrl = localProps.getProperty("transcil.baseUrl", prodBaseUrl)

android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            // Task 6 fills minify; keep BASE_URL = prod (defaultConfig)
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
        }
    }
}
```

Re-verify `ApiClient` still gates BODY logging with `BuildConfig.DEBUG` only.

- [ ] **Step 5: Skip commit** (unless user asks)

---

### Task 6: R8 minify + ProGuard keep rules

**Files:**
- Create: `app/proguard-rules.pro`
- Modify: `app/build.gradle.kts` release block

**Interfaces:**
- Consumes: Retrofit APIs, Gson DTOs, security-crypto, Navigation, SMS Retriever, Custom Tabs
- Produces: minified release APK/AAB

- [ ] **Step 1: Enable optimization for release**

```kotlin
buildTypes {
    release {
        optimization {
            enable = true
        }
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        buildConfigField("String", "BASE_URL", "\"https://api.transcil.in/\"")
    }
}
```

If AGP rejects combining `optimization` with `proguardFiles`, fall back to documented AGP 9 form that still enables R8 + loads `proguard-rules.pro` (check build error and fix minimally).

- [ ] **Step 2: Add proguard-rules.pro**

```proguard
# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Gson / @SerializedName models
-keepattributes Signature
-keepattributes EnclosingMethod
-keep class in.transcil.rider.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# EncryptedSharedPreferences / security-crypto
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Navigation safe args / fragment class names in XML
-keepnames class in.transcil.rider.**Fragment
-keepnames class in.transcil.rider.**Activity

# ViewBinding / DataBinding
-keep class in.transcil.rider.databinding.** { *; }

# Play Services SMS Retriever
-keep class com.google.android.gms.auth.api.phone.** { *; }
-dontwarn com.google.android.gms.**

# Custom Tabs
-keep class androidx.browser.** { *; }
```

- [ ] **Step 3: Assemble release**

Run: `./gradlew :app:assembleRelease`

Expected: BUILD SUCCESSFUL. If R8 strips a needed class, add the smallest keep rule and retry.

- [ ] **Step 4: Skip commit** (unless user asks)

---

### Task 7: GitHub Actions CI

**Files:**
- Create: `.github/workflows/android-ci.yml`

**Interfaces:**
- Consumes: Gradle wrapper
- Produces: CI green on unit tests (+ release assemble)

- [ ] **Step 1: Add workflow**

```yaml
name: Android CI

on:
  push:
    branches: [ main, master ]
  pull_request:

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Unit tests
        run: ./gradlew testDebugUnitTest --stacktrace
      - name: Assemble release
        run: ./gradlew assembleRelease --stacktrace
```

- [ ] **Step 2: Skip commit** (unless user asks)

---

### Task 8: Full verification + summary

**Files:** none (verification only)

- [ ] **Step 1: Run unit tests**

```bash
./gradlew testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 2: Run release assemble**

```bash
./gradlew assembleRelease
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Acceptance checklist**

Confirm and report:

- Release minify/shrink on
- Release assemble succeeds
- Unit tests pass
- No OkHttp BODY logs in release (`BuildConfig.DEBUG` gate)
- Cleartext blocked in release (main network_security_config)
- Bank local storage encrypted
- Help HTML escaped + test
- Demo dead code removed
- Backup excludes tokens + KYC local
- CI workflow present
- Package `in.transcil.rider` (uninstall/reinstall note)

- [ ] **Step 4: Deliver short summary to user** (hardened vs deferred P1; ProGuard why; package migration note). Do not commit unless asked.

---

## Spec coverage self-check

| Design P0 | Task |
|-----------|------|
| R8 + keep rules | Task 6 |
| App identity `in.transcil.rider` | Task 4 |
| Network / logging / BASE_URL | Task 5 (+ ApiClient already DEBUG-gated) |
| Encrypted bank draft | Task 3 |
| Help HTML escape | Task 2 |
| Demo cleanup | Task 1 |
| Backup rules | Task 3 |
| CI | Task 7 |
| Verify tests + release | Task 8 |
| P1 deferred | Not in plan |
