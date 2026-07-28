# Android Identity OTP + Digio Wiring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire `TranscilMobileApp` to gateway identity APIs for Cognito phone OTP login (Bearer tokens + refresh) and Digio KYC for Aadhaar/Bank via Custom Tabs + deep link.

**Architecture:** Thin `AuthRepository` / `DigioKycRepository` over existing Retrofit `TranscilApi` + `ApiClient`. `TokenStore` persists tokens; `AuthInterceptor` attaches Bearer; `TokenAuthenticator` refreshes once on 401. Digio opens Custom Tabs on `gateway_url`, returns to `transcil://kyc/callback`, then `POST /v1/kyc/sync-status` and local progress marks.

**Tech Stack:** Kotlin, Retrofit, OkHttp, Gson, Coroutines, LiveData, EncryptedSharedPreferences, AndroidX Browser (Custom Tabs). Base URL already `http://10.0.2.2:4000/`.

**Spec:** `docs/superpowers/specs/2026-07-24-android-identity-digio-wiring-design.md`  
**Backend contracts:** `transcil-identity-service/docs/MOBILE_APP_INTEGRATION_GUIDE.md`, `ONBOARDING_VERIFICATION_PROGRESS_FLOW.md`

## Global Constraints

- No Amplify / AppAuth Hosted UI / Digio SDK / Ktor
- Slice B only: do not poll `GET /v1/me/onboarding`; do not rewrite Personal/Address/PAN/Selfie
- Paths without leading slash: `v1/auth/start` (base URL has trailing `/`)
- Phone UI: 10 digits → repository builds `+91XXXXXXXXXX`
- Digio redirect: exactly `transcil://kyc/callback`
- Digio `customer_name`: letters/spaces only from `KycProgressRepository.personalDraft().fullName`
- Happy path must not navigate to `AadhaarOtpActivity`
- Keep public content GETs working with `error: null` envelopes
- Unit tests: JUnit4 + MockWebServer (existing style); no new test frameworks

---

## File map

| File | Responsibility |
|------|----------------|
| `data/model/ApiError.kt` | Typed identity/content error object |
| `data/model/ApiResponse.kt` | Envelope; meta accepts `request_id` + `requestId`; typed `error` |
| `data/model/auth/AuthDtos.kt` | start/verify/refresh request + response DTOs |
| `data/model/kyc/DigioDtos.kt` | Digio start/status DTOs |
| `data/network/TranscilApi.kt` | Auth + Digio Retrofit methods |
| `data/network/AuthTokenRefresher.kt` | Plain OkHttp refresh (no authenticator) |
| `data/network/TokenAuthenticator.kt` | 401 → refresh → retry once |
| `data/network/AuthInterceptor.kt` | unchanged behavior |
| `data/local/TokenStore.kt` | EncryptedSharedPreferences + memory fallback for tests |
| `TranscilApp.kt` + manifest `android:name` | `TokenStore.init` |
| `repository/AuthRepository.kt` | OTP start/verify/refresh/logout |
| `repository/DigioKycRepository.kt` | start + syncStatus |
| `auth/WelcomeViewModel.kt` / `WelcomeActivity.kt` | call start; pass session |
| `auth/VerifyOtpViewModel.kt` / `VerifyOtpActivity.kt` | call verify; save tokens |
| `core/NavExtras.kt` | `OTP_SESSION` |
| `kyc/DigioLauncher.kt` | Custom Tabs (+ WebView fallback Activity if needed) |
| `kyc/DigioKycCallbackActivity.kt` | deep link → sync → mark steps → progress |
| `kyc/KycProgressViewModel.kt` / Activity | Aadhaar/Bank CTAs launch Digio |
| `AndroidManifest.xml` | Application class + deep link |
| `gradle/libs.versions.toml` + `app/build.gradle.kts` | browser + security-crypto |
| Tests under `app/src/test/...` | envelope, auth repo, authenticator, digio repo |

---

### Task 1: Envelope + typed ApiError

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/data/model/ApiError.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/model/ApiResponse.kt`
- Modify: `app/src/test/java/com/example/transcilmobileapp/data/model/ApiResponseTest.kt`

**Interfaces:**
- Consumes: existing Gson usage
- Produces: `data class ApiError(code, message, retryable, category, clientAction)` with `@SerializedName`; `ApiMeta.requestId` from either `requestId` or `request_id`; `ApiResponse.error: ApiError?`

- [ ] **Step 1: Extend failing tests**

Add to `ApiResponseTest.kt`:

```kotlin
@Test
fun parsesIdentityEnvelopeWithSnakeCaseMetaAndError() {
    val json = """
        {
          "data": { "session": "abc" },
          "meta": { "request_id": "01J", "service": "identity", "timestamp": "2026-05-25T13:30:00.000Z" },
          "error": null
        }
    """.trimIndent()
    val type = object : TypeToken<ApiResponse<JsonObject>>() {}.type
    val res: ApiResponse<JsonObject> = Gson().fromJson(json, type)
    assertEquals("01J", res.meta?.requestId)
    assertNull(res.error)
}

@Test
fun parsesTypedErrorObject() {
    val json = """
        {
          "data": null,
          "meta": { "request_id": "01J" },
          "error": {
            "code": "AUTH_OTP_INVALID",
            "message": "Incorrect OTP",
            "retryable": false,
            "category": "auth",
            "client_action": "stay_on_screen"
          }
        }
    """.trimIndent()
    val type = object : TypeToken<ApiResponse<JsonObject>>() {}.type
    val res: ApiResponse<JsonObject> = Gson().fromJson(json, type)
    assertEquals("AUTH_OTP_INVALID", res.error?.code)
    assertEquals("stay_on_screen", res.error?.clientAction)
}
```

Keep existing `parsesEnvelopeWithNullError` passing.

- [ ] **Step 2: Run tests — expect new ones FAIL**

```bash
cd /Users/sudheer/AndroidStudioProjects/TranscilMobileApp
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.data.model.ApiResponseTest
```

Expected: FAIL on snake_case / typed error (current `error: Any?`, meta only `requestId`).

- [ ] **Step 3: Implement models**

`ApiError.kt`:

```kotlin
package com.example.transcilmobileapp.data.model

import com.google.gson.annotations.SerializedName

data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val retryable: Boolean? = null,
    val category: String? = null,
    @SerializedName("client_action")
    val clientAction: String? = null,
)
```

`ApiResponse.kt`:

```kotlin
package com.example.transcilmobileapp.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val data: T?,
    val meta: ApiMeta?,
    val error: ApiError?,
)

data class ApiMeta(
    @SerializedName(value = "requestId", alternate = ["request_id"])
    val requestId: String?,
    val nextCursor: String? = null,
)
```

- [ ] **Step 4: Re-run tests — expect PASS**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.data.model.ApiResponseTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/transcilmobileapp/data/model/ApiError.kt \
  app/src/main/java/com/example/transcilmobileapp/data/model/ApiResponse.kt \
  app/src/test/java/com/example/transcilmobileapp/data/model/ApiResponseTest.kt
git commit -m "$(cat <<'EOF'
feat(api): parse identity error envelope and snake_case request_id

EOF
)"
```

---

### Task 2: Auth DTOs + TranscilApi methods

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/data/model/auth/AuthDtos.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/network/TranscilApi.kt`

**Interfaces:**
- Consumes: `ApiResponse`
- Produces: Retrofit methods `authStart`, `authVerify`, `authRefresh`, `authLogout`

- [ ] **Step 1: Add DTOs**

```kotlin
package com.example.transcilmobileapp.data.model.auth

import com.google.gson.annotations.SerializedName

data class AuthStartRequest(
    @SerializedName("phone_e164") val phoneE164: String,
)

data class AuthStartData(
    val session: String,
    @SerializedName("ttl_seconds") val ttlSeconds: Int? = null,
    @SerializedName("resend_after_seconds") val resendAfterSeconds: Int? = null,
    @SerializedName("phone_e164_masked") val phoneE164Masked: String? = null,
)

data class AuthVerifyRequest(
    val session: String,
    @SerializedName("phone_e164") val phoneE164: String,
    val otp: String,
)

data class AuthTokensData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("id_token") val idToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
)

data class AuthRefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class AuthLogoutData(
    val ok: Boolean? = null,
)
```

- [ ] **Step 2: Extend `TranscilApi`**

```kotlin
import com.example.transcilmobileapp.data.model.auth.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// inside interface — keep existing GETs
@POST("v1/auth/start")
suspend fun authStart(
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: AuthStartRequest,
): ApiResponse<AuthStartData>

@POST("v1/auth/verify")
suspend fun authVerify(
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: AuthVerifyRequest,
): ApiResponse<AuthTokensData>

@POST("v1/auth/refresh")
suspend fun authRefresh(
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: AuthRefreshRequest,
): ApiResponse<AuthTokensData>

@POST("v1/auth/logout")
suspend fun authLogout(): ApiResponse<AuthLogoutData>
```

- [ ] **Step 3: Compile check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/transcilmobileapp/data/model/auth/AuthDtos.kt \
  app/src/main/java/com/example/transcilmobileapp/data/network/TranscilApi.kt
git commit -m "$(cat <<'EOF'
feat(api): add identity auth Retrofit endpoints

EOF
)"
```

---

### Task 3: AuthRepository (E.164 + start/verify)

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/repository/AuthRepository.kt`
- Create: `app/src/test/java/com/example/transcilmobileapp/repository/AuthRepositoryTest.kt`

**Interfaces:**
- Consumes: `TranscilApi` (injectable), `AuthStartData` / `AuthTokensData`
- Produces:
  - `fun toE164(tenDigitMobile: String): String`
  - `suspend fun start(tenDigitMobile: String): Result<AuthStartData>`
  - `suspend fun verify(session: String, tenDigitMobile: String, otp: String): Result<AuthTokensData>`
  - `suspend fun refresh(refreshToken: String): Result<AuthTokensData>`
  - `suspend fun logout(): Result<Unit>`
  - On API `error != null` → `Result.failure` with message from `error.message` or `error.code`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.ApiError
import com.example.transcilmobileapp.data.model.ApiResponse
import com.example.transcilmobileapp.data.model.auth.*
import com.example.transcilmobileapp.data.network.TranscilApi
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {

    private class FakeApi : TranscilApi {
        var lastStart: AuthStartRequest? = null
        var startResponse: ApiResponse<AuthStartData> = ApiResponse(
            AuthStartData(session = "S1"), null, null
        )
        var verifyResponse: ApiResponse<AuthTokensData> = ApiResponse(
            AuthTokensData(accessToken = "a", refreshToken = "r"), null, null
        )
        // stub all other TranscilApi methods with throw UnsupportedOperationException
        override suspend fun authStart(idempotencyKey: String, body: AuthStartRequest): ApiResponse<AuthStartData> {
            lastStart = body
            return startResponse
        }
        override suspend fun authVerify(idempotencyKey: String, body: AuthVerifyRequest) = verifyResponse
        override suspend fun authRefresh(idempotencyKey: String, body: AuthRefreshRequest) =
            error("unused")
        override suspend fun authLogout() = error("unused")
        // ... implement remaining GET stubs returning empty ApiResponse or throw
    }

    @Test
    fun toE164_prefixesIndia() {
        assertEquals("+919876543210", AuthRepository.toE164("9876543210"))
    }

    @Test
    fun start_sendsE164() = runBlocking {
        val api = FakeApi()
        val repo = AuthRepository(api)
        val result = repo.start("9876543210")
        assertTrue(result.isSuccess)
        assertEquals("+919876543210", api.lastStart?.phoneE164)
        assertEquals("S1", result.getOrNull()?.session)
    }

    @Test
    fun verify_mapsApiError() = runBlocking {
        val api = FakeApi().apply {
            verifyResponse = ApiResponse(
                null, null,
                ApiError(code = "AUTH_OTP_INVALID", message = "Incorrect OTP", clientAction = "stay_on_screen")
            )
        }
        val result = AuthRepository(api).verify("S1", "9876543210", "000000")
        assertTrue(result.isFailure)
        assertEquals("Incorrect OTP", result.exceptionOrNull()?.message)
    }
}
```

Implement FakeApi GET stubs minimally so the class compiles (return `ApiResponse(null,null,null)` or empty typed objects).

- [ ] **Step 2: Run — expect FAIL (AuthRepository missing)**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.repository.AuthRepositoryTest
```

- [ ] **Step 3: Implement `AuthRepository`**

```kotlin
package com.example.transcilmobileapp.repository

import com.example.transcilmobileapp.data.model.auth.*
import com.example.transcilmobileapp.data.network.TranscilApi
import java.util.UUID

class AuthRepository(
    private val api: TranscilApi = com.example.transcilmobileapp.data.network.ApiClient.transcilApi,
) {
    companion object {
        fun toE164(tenDigitMobile: String): String {
            val digits = tenDigitMobile.filter { it.isDigit() }.takeLast(10)
            return "+91$digits"
        }
    }

    suspend fun start(tenDigitMobile: String): Result<AuthStartData> = runCatching {
        val res = api.authStart(UUID.randomUUID().toString(), AuthStartRequest(toE164(tenDigitMobile)))
        res.error?.let { error(it.message ?: it.code ?: "AUTH_START_FAILED") }
        res.data ?: error("AUTH_START_EMPTY")
    }

    suspend fun verify(session: String, tenDigitMobile: String, otp: String): Result<AuthTokensData> =
        runCatching {
            val res = api.authVerify(
                UUID.randomUUID().toString(),
                AuthVerifyRequest(session, toE164(tenDigitMobile), otp)
            )
            res.error?.let { error(it.message ?: it.code ?: "AUTH_VERIFY_FAILED") }
            res.data ?: error("AUTH_VERIFY_EMPTY")
        }

    suspend fun refresh(refreshToken: String): Result<AuthTokensData> = runCatching {
        val res = api.authRefresh(UUID.randomUUID().toString(), AuthRefreshRequest(refreshToken))
        res.error?.let { error(it.message ?: it.code ?: "AUTH_REFRESH_FAILED") }
        res.data ?: error("AUTH_REFRESH_EMPTY")
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val res = api.authLogout()
        res.error?.let { error(it.message ?: it.code ?: "AUTH_LOGOUT_FAILED") }
        Unit
    }
}
```

- [ ] **Step 4: Run tests — PASS**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.repository.AuthRepositoryTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/transcilmobileapp/repository/AuthRepository.kt \
  app/src/test/java/com/example/transcilmobileapp/repository/AuthRepositoryTest.kt
git commit -m "$(cat <<'EOF'
feat(auth): add AuthRepository for OTP start/verify

EOF
)"
```

---

### Task 4: Persistent TokenStore + Application

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/TranscilApp.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/local/TokenStore.kt`
- Modify: `app/src/main/AndroidManifest.xml` (`android:name=".TranscilApp"`)
- Modify: `gradle/libs.versions.toml` + `app/build.gradle.kts` — `androidx.security:security-crypto`

**Interfaces:**
- Consumes: `Context` via `TokenStore.init(context)`
- Produces: same `save` / `getAccessToken` / `getRefreshToken` / `clear` / `hasToken`; if not inited, memory-only (keeps unit tests working)

- [ ] **Step 1: Add dependency**

`libs.versions.toml`:

```toml
securityCrypto = "1.1.0-alpha06"
# libraries:
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
```

`app/build.gradle.kts`: `implementation(libs.androidx.security.crypto)`

- [ ] **Step 2: Implement TokenStore with prefs + memory fallback**

```kotlin
package com.example.transcilmobileapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    private const val PREFS = "transcil_tokens"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var memAccess: String? = null
    @Volatile private var memRefresh: String? = null

    fun init(context: Context) {
        if (prefs != null) return
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        memAccess = prefs?.getString(KEY_ACCESS, null)
        memRefresh = prefs?.getString(KEY_REFRESH, null)
    }

    @Synchronized
    fun save(accessToken: String, refreshToken: String? = null) {
        memAccess = accessToken
        if (refreshToken != null) memRefresh = refreshToken
        prefs?.edit()
            ?.putString(KEY_ACCESS, accessToken)
            ?.apply {
                if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
            }
            ?.apply()
    }

    fun getAccessToken(): String? = memAccess ?: prefs?.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = memRefresh ?: prefs?.getString(KEY_REFRESH, null)

    @Synchronized
    fun clear() {
        memAccess = null
        memRefresh = null
        prefs?.edit()?.clear()?.apply()
    }

    fun hasToken(): Boolean = !getAccessToken().isNullOrBlank()
}
```

- [ ] **Step 3: `TranscilApp` + manifest**

```kotlin
package com.example.transcilmobileapp

import android.app.Application
import com.example.transcilmobileapp.data.local.TokenStore

class TranscilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
```

Manifest `<application android:name=".TranscilApp" ...>`.

- [ ] **Step 4: Re-run existing TokenAuthenticatorTest — still PASS** (memory fallback)

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.data.network.TokenAuthenticatorTest
```

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts \
  app/src/main/java/com/example/transcilmobileapp/data/local/TokenStore.kt \
  app/src/main/java/com/example/transcilmobileapp/TranscilApp.kt \
  app/src/main/AndroidManifest.xml
git commit -m "$(cat <<'EOF'
feat(auth): persist tokens with EncryptedSharedPreferences

EOF
)"
```

---

### Task 5: TokenAuthenticator refresh

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/data/network/AuthTokenRefresher.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/network/TokenAuthenticator.kt`
- Modify: `app/src/test/java/com/example/transcilmobileapp/data/network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: `TokenStore`, `BuildConfig.BASE_URL`
- Produces: `AuthTokenRefresher.refreshBlocking(refreshToken): String?` (new access token or null)
- Authenticator: on 401 with refresh token → refresh → `TokenStore.save(newAccess, sameRefresh)` → retry with Bearer; never authenticate refresh URL; max 1 retry

- [ ] **Step 1: Write failing authenticator test**

```kotlin
@Test
fun tokenAuthenticator_refreshesAndRetries() {
    // 1) protected call → 401
    server.enqueue(MockResponse().setResponseCode(401))
    // 2) refresh → 200 with new access
    server.enqueue(
        MockResponse().setBody(
            """{"data":{"access_token":"new-access","id_token":"id","token_type":"Bearer","expires_in":3600},"meta":null,"error":null}"""
        ).addHeader("Content-Type", "application/json")
    )
    // 3) retry → 200
    server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

    TokenStore.save("old-access", "refresh-1")

    // Point refresher base URL at mock server — inject via AuthTokenRefresher.baseUrlOverride for tests
    AuthTokenRefresher.baseUrlOverride = server.url("/").toString()

    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .authenticator(TokenAuthenticator())
        .build()

    val response = client.newCall(
        Request.Builder().url(server.url("/secure")).build()
    ).execute()

    assertEquals(200, response.code)
    assertEquals("new-access", TokenStore.getAccessToken())
    assertEquals("refresh-1", TokenStore.getRefreshToken())
}
```

- [ ] **Step 2: Run — FAIL**

```bash
./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.data.network.TokenAuthenticatorTest
```

- [ ] **Step 3: Implement refresher + authenticator**

`AuthTokenRefresher.kt` — synchronous OkHttp POST to `{base}v1/auth/refresh` with JSON body `{"refresh_token":"..."}`, parse `data.access_token` with Gson, **no** Authenticator on that client.

`TokenAuthenticator` — replace TODO with:

```kotlin
val newAccess = AuthTokenRefresher.refreshBlocking(refreshToken) ?: run {
    TokenStore.clear()
    return null
}
TokenStore.save(newAccess, refreshToken)
return response.request.newBuilder()
    .header("Authorization", "Bearer $newAccess")
    .build()
```

- [ ] **Step 4: Run TokenAuthenticatorTest — all PASS**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/transcilmobileapp/data/network/AuthTokenRefresher.kt \
  app/src/main/java/com/example/transcilmobileapp/data/network/TokenAuthenticator.kt \
  app/src/test/java/com/example/transcilmobileapp/data/network/TokenAuthenticatorTest.kt
git commit -m "$(cat <<'EOF'
feat(auth): refresh access token on HTTP 401

EOF
)"
```

---

### Task 6: Wire Welcome + Verify OTP UI

**Files:**
- Modify: `core/NavExtras.kt` — add `OTP_SESSION = "OTP_SESSION"`
- Modify: `auth/WelcomeViewModel.kt`, `WelcomeActivity.kt`
- Modify: `auth/VerifyOtpViewModel.kt`, `VerifyOtpActivity.kt`

**Interfaces:**
- Consumes: `AuthRepository`, `TokenStore`
- Produces: Welcome navigates with phone + session; Verify saves tokens then `ChooseJourneyActivity`

- [ ] **Step 1: WelcomeViewModel — call start**

Use `viewModelScope.launch` + `AuthRepository().start(mobile)`. On success emit `NavigateToOtp(mobile, session)`. On failure `showError(message)`. Keep 10-digit validation.

Pass both extras from Activity:

```kotlin
intent.putExtra(NavExtras.MOBILE_NUMBER, event.mobile)
intent.putExtra(NavExtras.OTP_SESSION, event.session)
```

- [ ] **Step 2: VerifyOtpViewModel — call verify**

Constructor/factory or default `AuthRepository()`. `onVerifyClicked(session, mobile, otp)` → verify → `TokenStore.save(access, refresh)` → `_navigateToHome.value = true`. Resend: call `start` again and expose new session via LiveData for Activity to update local field.

- [ ] **Step 3: VerifyOtpActivity**

Read `OTP_SESSION`; on missing session toast + finish. Keep `KycProgressRepository.saveSessionMobile`. Loading: disable button while `isLoading`.

- [ ] **Step 4: Compile + unit-smoke optional ViewModel tests if cheap; else manual**

```bash
./gradlew :app:compileDebugKotlin
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/transcilmobileapp/core/NavExtras.kt \
  app/src/main/java/com/example/transcilmobileapp/auth/*.kt
git commit -m "$(cat <<'EOF'
feat(auth): wire Welcome/Verify OTP to identity APIs

EOF
)"
```

---

### Task 7: Digio DTOs + API + DigioKycRepository

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/data/model/kyc/DigioDtos.kt`
- Modify: `app/src/main/java/com/example/transcilmobileapp/data/network/TranscilApi.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/repository/DigioKycRepository.kt`
- Create: `app/src/test/java/com/example/transcilmobileapp/repository/DigioKycRepositoryTest.kt`

**Interfaces:**
- Consumes: `TranscilApi`, Bearer via interceptor
- Produces:
  - `const val DIGIO_REDIRECT_URL = "transcil://kyc/callback"`
  - `suspend fun start(customerName: String): Result<DigioStartData>`
  - `suspend fun syncStatus(): Result<DigioStatusData>`
  - Reject blank / digit-containing `customerName` locally before network

DTOs:

```kotlin
data class DigioStartRequest(
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("redirect_url") val redirectUrl: String,
)
data class DigioStartData(
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("digio_request_id") val digioRequestId: String? = null,
    val status: String? = null,
    @SerializedName("gateway_url") val gatewayUrl: String,
)
data class DigioStatusData(
    val status: String,
    @SerializedName("digio_request_id") val digioRequestId: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("onboarding_required") val onboardingRequired: Boolean? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
)
```

API:

```kotlin
@POST("v1/kyc/start")
suspend fun kycStart(
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: DigioStartRequest,
): ApiResponse<DigioStartData>

@POST("v1/kyc/sync-status")
suspend fun kycSyncStatus(
    @Header("Idempotency-Key") idempotencyKey: String,
): ApiResponse<DigioStatusData>

@GET("v1/kyc/status")
suspend fun kycStatus(): ApiResponse<DigioStatusData>
```

- [ ] **Step 1: Failing test — start sends redirect + name**

```kotlin
@Test
fun start_sendsRedirectAndName() = runBlocking {
    // fake api captures DigioStartRequest
    val result = repo.start("Ravi Kumar")
    assertTrue(result.isSuccess)
    assertEquals("transcil://kyc/callback", fake.lastStart?.redirectUrl)
    assertEquals("Ravi Kumar", fake.lastStart?.customerName)
}

@Test
fun start_rejectsNameWithDigits() = runBlocking {
    val result = repo.start("Ravi 123")
    assertTrue(result.isFailure)
}
```

- [ ] **Step 2: Implement repository + API — tests PASS**

- [ ] **Step 3: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(kyc): add Digio start/sync repository

EOF
)"
```

---

### Task 8: Custom Tabs + deep-link callback

**Files:**
- Modify: `gradle/libs.versions.toml` + `app/build.gradle.kts` — `androidx.browser:browser`
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/DigioLauncher.kt`
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/DigioKycCallbackActivity.kt`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- Consumes: `DigioKycRepository.syncStatus()`, `KycProgressRepository.markCompleted`
- Produces: `DigioLauncher.open(activity, gatewayUrl)`; callback Activity handles `transcil://kyc/callback`

- [ ] **Step 1: Add browser dependency**

```toml
browser = "1.8.0"
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
```

- [ ] **Step 2: DigioLauncher**

```kotlin
object DigioLauncher {
    fun open(activity: Activity, gatewayUrl: String) {
        val uri = Uri.parse(gatewayUrl)
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(activity, uri)
    }
}
```

- [ ] **Step 3: DigioKycCallbackActivity**

- `onCreate`: `lifecycleScope.launch { DigioKycRepository().syncStatus() }`
- If `status.equals("approved", ignoreCase = true)`: `markCompleted(AADHAAR)` and `markCompleted(BANK)` (slice B local projection per design; bank may still need details later — still mark Aadhaar; mark Bank only when status approved **and** you choose to treat Digio as bank-complete — **lock:** mark **Aadhaar** on `approved`; mark **Bank** on `approved` only if `KycProgressRepository.bankDraft()` already has account fields **or** always mark both for slice B demo — **lock: mark Aadhaar always on approved; mark Bank on approved too** so progress unlocks; user can still edit bank form later)
- Start `KycProgressActivity` with `FLAG_ACTIVITY_CLEAR_TOP`, finish
- On failure / pending: toast + return to progress without marking complete

- [ ] **Step 4: Manifest intent-filter**

```xml
<activity
    android:name=".kyc.DigioKycCallbackActivity"
    android:exported="true"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="transcil"
            android:host="kyc"
            android:pathPrefix="/callback" />
    </intent-filter>
</activity>
```

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

- [ ] **Step 6: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(kyc): Custom Tabs Digio launch and deep-link callback

EOF
)"
```

---

### Task 9: Point Aadhaar/Bank progress CTAs at Digio

**Files:**
- Modify: `kyc/KycProgressViewModel.kt`
- Modify: `kyc/KycProgressActivity.kt` (observe launch URL / errors)
- Modify: `kyc/AadhaarVerificationViewModel.kt` + Activity only if still reachable — prefer progress path; if Activity still used from elsewhere, same Digio launch instead of OTP nav

**Interfaces:**
- Consumes: `DigioKycRepository`, `DigioLauncher`, personal draft name
- Produces: LiveData `openDigioUrl: String` / error; no navigation to `AadhaarOtpActivity`

- [ ] **Step 1: Replace `submitAadhaarNumber` Digio path**

After consent + optional 12-digit validation (keep UX validation if field still shown):

```kotlin
fun startDigioFromAadhaar(consent: Boolean) {
    if (!consent) { _showStubMessage.value = R.string.error_aadhaar_consent; return }
    val name = KycProgressRepository.personalDraft().fullName.trim()
    if (name.isBlank() || name.any { it.isDigit() }) {
        _showStubMessage.value = R.string.kyc_digio_need_personal_name // add string
        return
    }
    viewModelScope.launch {
        DigioKycRepository().start(name)
            .onSuccess { _openDigioUrl.value = it.gatewayUrl }
            .onFailure { _showStubMessage.value = /* string or use error LiveData */ }
    }
}
```

Remove OTP-sent branch that expects `submitAadhaarOtp` for happy path (keep method unused or delete call sites in Activity).

- [ ] **Step 2: Bank Digio entry**

Add `startDigioFromBank(consent: Boolean)` same as above (consent from bank draft). Wire primary Bank CTA in `KycProgressActivity` to Digio when user taps Verify (keep `submitBank` as secondary only if a separate Save exists; if single CTA, Digio replaces `submitBank` for Verify).

**Lock:** Bank accordion primary Verify → Digio; do not require account number for Digio start.

- [ ] **Step 3: Activity observes `_openDigioUrl` → `DigioLauncher.open`**

- [ ] **Step 4: Ensure `AadhaarVerificationActivity` does not navigate to OTP**

Change ViewModel to emit Digio URL event (or finish + toast “use progress screen”) — **lock:** if Activity opened, call same Digio start and open Custom Tabs; `navigateToOtp` removed.

- [ ] **Step 5: Compile + existing KYC unit tests still pass**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(kyc): launch Digio from Aadhaar and Bank steps

EOF
)"
```

---

### Task 10: Manual E2E against local gateway

**Files:** none (verification only)

- [ ] **Step 1: Preconditions**

- Docker gateway on host `:4000`
- Emulator `BASE_URL=http://10.0.2.2:4000/`
- Identity OTP visible in identity service logs (local/dev)

- [ ] **Step 2: Auth checklist**

1. Welcome → 10-digit → OTP SMS/log → Verify → Choose Journey  
2. Logcat OkHttp shows `Authorization: Bearer` on a protected call (Digio start or any `/v1/me/*`)  
3. Force-stop app → relaunch → `TokenStore.hasToken()` still true (add temporary log on splash if needed)  
4. Public `GET v1/settings` still works

- [ ] **Step 3: Digio checklist**

1. Complete Personal name (letters only)  
2. Aadhaar consent → Verify → Custom Tab opens Digio/`gateway_url`  
3. Complete or cancel; on return deep link → sync toast  
4. On approved: Aadhaar (+ Bank) marked complete in progress  
5. Confirm no navigation to `AadhaarOtpActivity`

- [ ] **Step 4: No code commit unless fixes needed; if fixes, commit with message describing the fix**

---

## Self-review (plan vs spec)

| Spec requirement | Task |
|---|---|
| Auth start/verify/refresh/logout API | 2, 3, 5, 6 |
| EncryptedSharedPreferences TokenStore | 4 |
| AuthInterceptor + TokenAuthenticator refresh | 5 (interceptor already exists) |
| Welcome/Verify wiring | 6 |
| Envelope snake_case + typed errors | 1 |
| Digio start/sync + redirect URL | 7 |
| Custom Tabs + deep link | 8 |
| Aadhaar/Bank CTAs → Digio; no Aadhaar OTP happy path | 9 |
| Leave Personal/Address/PAN/Selfie / no onboarding poll | constrained; not tasked |
| Manual E2E | 10 |

No TBD/TODO placeholders remain. Types consistent: `AuthTokensData.accessToken` / `refreshToken`, `DigioStartData.gatewayUrl`, `DIGIO_REDIRECT_URL`.
