# Kotlin file-header comments (Approach A)

## Goal

Every Kotlin file under `app/src` should explain its role in 2–4 lines at the top, so you can open a file and know what it does without reading the body.

## Scope

- **In:** all `*.kt` under `app/src` (main, test, androidTest) — ~144 files
- **Out:** XML/resources, Manifest, Gradle, non-Kotlin assets
- **No behavior changes** — comments only

## Format

Place a file-level KDoc **above** the `package` line:

```kotlin
/**
 * Restores session on cold start and routes to onboarding, KYC, or home.
 * Used from splash / MainActivity after token check.
 */
package com.transcil.rider.auth
```

Rules:

1. One block per file, 1–4 short sentences (prefer 1–2).
2. Cover **what** the file does; add **where it sits** only when it clarifies (caller/flow).
3. Do not restate the filename/class name alone (“AuthSession class”).
4. If the primary type already has a good KDoc, keep it; the file header is still added (may briefly overlap).
5. Tests: say what production code/behavior they cover.
6. Multi-type files: describe the file’s overall responsibility, not every type.

## Examples

| File | Header gist |
|------|-------------|
| `TranscilApp.kt` | Application entry; inits TokenStore / KycLocalStore |
| `ApiClient.kt` | Shared Retrofit/OkHttp client with auth + 401 refresh |
| `WelcomeActivity.kt` | Phone-entry welcome screen; starts OTP flow |
| `AuthSessionTest.kt` | Unit tests for cold-start routing in AuthSession |

## Teaching comments (expanded scope)

In addition to file headers, production files get short inline notes on Kotlin/Android idioms where they first appear (e.g. `override fun`, `suspend fun`, `companion object`, `sealed class`, ViewBinding `val`/`var`, SharedPreferences vs EncryptedSharedPreferences, Retrofit annotations). Foundation files (`TokenStore`, `BaseActivity`, `AuthSession`, network layer) carry the densest explanations; other files point at the pattern without repeating a textbook on every line.

## Non-goals

- Commenting every single line of code
- Package READMEs / architecture diagrams in source
- Reformatting or renaming files

## Verification

- Every `app/src/**/*.kt` starts with `/**` before `package`
- Project still compiles (comment-only diff)
