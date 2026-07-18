# Personal Details Validation + Accordion Persistence Design

**Date:** 2026-07-18  
**App:** TranscilMobileApp (Kotlin + XML Views, MVVM)  
**Status:** Approved for planning after user review  
**Branch context:** Continue on `feature/kyc-progress-selfie` (or current KYC feature branch).

## Goal

Tighten **Create Personal Account** input validation (production-lean rules + inline field errors), keep saving personal details into the existing in-memory KYC store, and ensure **KYC Progress** Personal Details accordion shows those values and allows the same validated edits. No Compose, no API, no disk persistence this pass.

## Confirmed decisions

| Decision | Choice |
|----------|--------|
| Validation depth | B+C: production rules + inline field errors |
| Full name max | **40** characters |
| Email domains | **Allowlist** of common providers |
| DOB | Not in the future; age **≥ 18** |
| Gender | **No silent default**; must be selected |
| Persistence | Existing `KycProgressRepository` (in-memory session) |
| Accordion | Prefill from draft; edit/submit uses same shared validator |
| Error UX | Per-field helper text + error input background; clear on change |

## Current baseline

- `CreatePersonalAccountActivity` / `CreatePersonalAccountViewModel`: required fields + `Patterns.EMAIL_ADDRESS`; gender defaults to `Gender.MALE`; Toast errors; saves via `KycProgressRepository.savePersonal` then opens Progress.
- `KycProgressActivity` Personal accordion: hydrates `personalDraft()`, live `savePersonalDraft`, `submitPersonal` with duplicated light validation.
- No shared validator; no name/age/domain rules; no inline field errors.

## Target behavior

```
Create Personal Account
  → validate (shared) with inline errors
  → save PersonalDraft + mark PERSONAL completed
  → KycProgressActivity
       → Personal accordion shows saved fields
       → Edit / Submit re-validates with same rules
       → invalid: inline errors, stay expanded
       → valid: save draft, mark completed, refresh
```

## Validation rules (canonical)

| Field | Rules |
|-------|--------|
| Full Name | Required after trim; letters and spaces only; length **2–40**; `android:maxLength="40"` on inputs |
| Email | Required; matches `Patterns.EMAIL_ADDRESS`; domain (after `@`) lowercased must be in allowlist |
| Date of Birth | Required (date picker); format `dd - MM - yyyy` (existing); not after today (UTC); age ≥ 18 |
| Gender | Required; initial state `null` (no chip selected) until user taps Male / Female / Other |

### Email allowlist (exact domains, case-insensitive)

`gmail.com`, `googlemail.com`, `yahoo.com`, `yahoo.co.in`, `outlook.com`, `hotmail.com`, `live.com`, `icloud.com`, `me.com`, `aol.com`, `proton.me`, `protonmail.com`, `zoho.com`, `rediffmail.com`

### Error copy (strings.xml)

| Key | Message |
|-----|---------|
| `error_full_name_required` | Please enter your full name |
| `error_full_name_invalid` | Name can only contain letters and spaces |
| `error_full_name_too_short` | Name must be at least 2 characters |
| `error_full_name_too_long` | Name must be at most 40 characters |
| `error_email_required` | Please enter your email address |
| `error_invalid_email` | Please enter a valid email address (reuse/update existing) |
| `error_email_domain` | Please use a common email provider (Gmail, Yahoo, Outlook, etc.) |
| `error_dob_required` | Please select your date of birth |
| `error_dob_future` | Date of birth cannot be in the future |
| `error_dob_underage` | You must be at least 18 years old |
| `error_gender_required` | Please select your gender |

Show **all** failing fields on submit (not only the first), so the user can fix everything in one pass.

## Architecture

| Layer | Approach |
|-------|----------|
| Validator | New `PersonalDetailsValidator` in `kyc` (or `core`) — pure Kotlin, returns `PersonalFieldErrors` with optional `@StringRes` / message keys per field |
| Allowlist | `PersonalEmailDomains` (set of lowercase domains) next to validator |
| Create screen VM | Drop default `Gender.MALE`; call validator on Continue; expose `LiveData<PersonalFieldErrors>` (or sealed UI state); save + navigate only when valid |
| Progress VM | `submitPersonal` uses same validator; expose field errors for accordion bind; keep draft autosave without blocking typing |
| UI | Helper `TextView`s under name/email/DOB/gender on `activity_create_personal_account.xml` and `item_kyc_step.xml` personal form; `bg_input_error` (+ optional chip error stroke); apply/clear via small `UiFormHelpers` extension |
| Tokens | Add `color/error` (day + night) if missing; use for helper text and error stroke — no hex in layouts |
| Tests | Unit tests for validator: name bounds/charset, email format/domain, DOB future/underage/ok, gender null |

### Suggested API

```kotlin
data class PersonalFieldErrors(
    val fullName: Int? = null,   // string res
    val email: Int? = null,
    val dateOfBirth: Int? = null,
    val gender: Int? = null
) {
    val hasErrors: Boolean get() = fullName != null || email != null || dateOfBirth != null || gender != null
}

object PersonalDetailsValidator {
    fun validate(
        fullName: String,
        email: String,
        dateOfBirth: String, // "dd - MM - yyyy" or blank
        gender: Gender?,
        todayMillis: Long = System.currentTimeMillis()
    ): PersonalFieldErrors
}
```

DOB parsing uses the same UTC `SimpleDateFormat("dd - MM - yyyy")` as the Activities. Age: calendar year/month/day comparison so birthday today at 18 is allowed.

## Persistence + accordion

Unchanged store shape:

```kotlin
data class PersonalDraft(
    val fullName: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val gender: Gender? = null
)
```

| Event | Behavior |
|-------|----------|
| Create Continue (valid) | `savePersonal` → `markCompleted(PERSONAL)` → open Progress |
| Accordion field change (editable) | `savePersonalDraft` (no hard block while typing) |
| Accordion Submit (valid) | `savePersonal` → `markCompleted(PERSONAL)` → clear inline edit / refresh |
| Accordion Submit (invalid) | Set field errors; do not mark completed |
| Open / Edit Personal | Prefill from `personalDraft()`; gender chips only selected if draft.gender != null |
| Process death | Drafts lost (existing limitation; out of scope) |

## UI details

**Create Personal Account**

- Gender chips start unselected (`bg_chip_default` all three).
- On validation failure: show relevant `tv*Error` (gone when null); set input/container background to `bg_input_error`.
- On text/gender/DOB change: clear that field’s error only.
- Name `EditText`: `maxLength=40`, `inputType=textPersonName`.

**KYC Progress Personal accordion**

- Same inline error views under the four controls.
- Completed + not editing: errors hidden; fields read-only as today.
- Editing: same validate-on-submit + clear-on-change behavior.

## Files to add / change

**Add**

- `app/src/main/java/com/example/transcilmobileapp/kyc/PersonalDetailsValidator.kt` (validator + errors + allowlist)
- `app/src/test/java/com/example/transcilmobileapp/kyc/PersonalDetailsValidatorTest.kt`
- `res/drawable/bg_input_error.xml` (and chip error if needed)
- Error color token(s) in `values/colors.xml` + `values-night/colors.xml`

**Change**

- `CreatePersonalAccountViewModel.kt` — shared validator, null gender default, field-error LiveData
- `CreatePersonalAccountActivity.kt` — bind/clear inline errors; stop relying on Toast for these fields
- `activity_create_personal_account.xml` — error TextViews; maxLength
- `KycProgressViewModel.kt` — `submitPersonal` via shared validator; expose personal field errors
- `KycProgressActivity.kt` — bind/clear accordion personal errors
- `item_kyc_step.xml` — personal form error TextViews; maxLength
- `strings.xml` — error copy above
- `UiFormHelpers.kt` (if present) — optional apply/clear error helpers

## Out of scope

- SharedPreferences / DataStore persistence
- Backend / Digio API validation
- Changing Address / Aadhaar / other step validators
- Expanding email allowlist via remote config
- Compose migration

## Success criteria

1. Empty/invalid name, bad/disallowed email, missing/future/under-18 DOB, or unselected gender block Continue and accordion Submit with **inline** messages.
2. Valid submit saves draft; Progress Personal accordion shows the same values.
3. Editing in accordion and re-submitting re-validates and updates the draft.
4. Gender never appears selected until the user chooses (or a saved draft is hydrated).
5. Unit tests cover the validator rules listed above.
