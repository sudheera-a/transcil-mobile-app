# Personal Details Validation + Accordion Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Shared production-lean Personal Details validation with inline field errors on Create Personal Account and KYC Progress accordion, using existing `KycProgressRepository` for save/hydrate/edit.

**Architecture:** Pure `PersonalDetailsValidator` returns per-field string-res errors. Both ViewModels call it on submit. Activities bind helper TextViews + `bg_input_error`. Gender starts `null`. Drafts stay in `KycProgressRepository`.

**Tech Stack:** Kotlin, XML Views, ViewBinding, LiveData, JUnit, Material DatePicker

## Global Constraints

- Full name: letters + spaces only; length 2–40; `maxLength=40`
- Email: `Patterns.EMAIL_ADDRESS` + allowlist domains from spec
- DOB: format `dd - MM - yyyy`; not future; age ≥ 18 (UTC)
- Gender: no silent default (`null` until selected)
- Inline errors on all failing fields; clear field error on change
- No Compose; no DataStore; no API
- Strings in `strings.xml`; colors/tokens only (no hex in layouts)
- Spec: `docs/superpowers/specs/2026-07-18-personal-details-validation-accordion-design.md`

---

## File map

| File | Responsibility |
|------|----------------|
| `kyc/PersonalDetailsValidator.kt` | Allowlist, `PersonalFieldErrors`, validate() |
| `test/.../PersonalDetailsValidatorTest.kt` | Unit tests |
| `values/strings.xml` | Error copy |
| `values/colors.xml` + night | `color/error` |
| `drawable/bg_input_error.xml` | Error input stroke/fill |
| `core/UiFormHelpers.kt` | show/clear field error helpers |
| `CreatePersonalAccountViewModel.kt` | Null gender; fieldErrors LiveData; validator on Continue |
| `CreatePersonalAccountActivity.kt` | Bind errors; clear on change; remove Toast for these |
| `activity_create_personal_account.xml` | Error TextViews; maxLength; chips default unselected |
| `KycProgressViewModel.kt` | submitPersonal via validator; personalFieldErrors |
| `KycProgressActivity.kt` | Bind/clear accordion personal errors |
| `item_kyc_step.xml` | Personal form error TextViews; maxLength |

---

### Task 1: Validator + unit tests

**Files:**
- Create: `app/src/main/java/com/example/transcilmobileapp/kyc/PersonalDetailsValidator.kt`
- Create: `app/src/test/java/com/example/transcilmobileapp/kyc/PersonalDetailsValidatorTest.kt`
- Modify: `app/src/main/res/values/strings.xml` (error strings)

**Interfaces:**
- Produces: `PersonalFieldErrors`, `PersonalDetailsValidator.validate(...)`, `PersonalEmailDomains.ALLOWED`

- [x] **Step 1: Add error strings** to `strings.xml`:

```xml
<string name="error_full_name_required">Please enter your full name</string>
<string name="error_full_name_invalid">Name can only contain letters and spaces</string>
<string name="error_full_name_too_short">Name must be at least 2 characters</string>
<string name="error_full_name_too_long">Name must be at most 40 characters</string>
<string name="error_email_required">Please enter your email address</string>
<string name="error_email_domain">Please use a common email provider (Gmail, Yahoo, Outlook, etc.)</string>
<string name="error_dob_required">Please select your date of birth</string>
<string name="error_dob_future">Date of birth cannot be in the future</string>
<string name="error_dob_underage">You must be at least 18 years old</string>
<string name="error_gender_required">Please select your gender</string>
```

Keep/update `error_invalid_email` for format failures.

- [x] **Step 2: Write failing tests** covering: blank name, short name, long name (>40), invalid chars, valid name; blank/invalid/disallowed/allowed email; blank DOB, future DOB, under 18, exactly 18; null gender vs selected; multiple fields fail together.

- [x] **Step 3: Implement validator**

```kotlin
data class PersonalFieldErrors(
    val fullName: Int? = null,
    val email: Int? = null,
    val dateOfBirth: Int? = null,
    val gender: Int? = null
) {
    val hasErrors: Boolean
        get() = fullName != null || email != null || dateOfBirth != null || gender != null
}

object PersonalEmailDomains {
    val ALLOWED = setOf(
        "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.in",
        "outlook.com", "hotmail.com", "live.com", "icloud.com", "me.com",
        "aol.com", "proton.me", "protonmail.com", "zoho.com", "rediffmail.com"
    )
}

object PersonalDetailsValidator {
    private val nameRegex = Regex("^[A-Za-z]+(?: [A-Za-z]+)*$")
    private val dobFormat = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun validate(
        fullName: String,
        email: String,
        dateOfBirth: String,
        gender: Gender?,
        todayMillis: Long = System.currentTimeMillis()
    ): PersonalFieldErrors { ... }
}
```

Name rules: trim; if blank → required; if length < 2 → too_short; if length > 40 → too_long; if !nameRegex → invalid.  
Email: trim; blank → required; !Patterns → invalid_email; domain not in ALLOWED → email_domain.  
DOB: blank → required; parse fail → required; date after today → future; age < 18 → underage.  
Gender: null → gender_required.

- [x] **Step 4: Run** `./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.kyc.PersonalDetailsValidatorTest` — expect PASS

- [ ] **Step 5: Commit** `feat: add PersonalDetailsValidator with unit tests`

---

### Task 2: Error tokens + UiFormHelpers

**Files:**
- Create: `app/src/main/res/drawable/bg_input_error.xml`
- Modify: `colors.xml`, `values-night/colors.xml`
- Modify: `core/UiFormHelpers.kt`

- [ ] **Step 1:** Add `<color name="error">#D32F2F</color>` (day) and a slightly softer night red e.g. `#EF9A9A`.

- [ ] **Step 2:** `bg_input_error.xml` — surface fill + error stroke + `radius_input` (mirror `bg_input_default`).

- [ ] **Step 3:** Helpers:

```kotlin
fun setFieldError(errorView: TextView, container: View?, messageRes: Int?) {
    if (messageRes == null) {
        errorView.visibility = View.GONE
        errorView.text = ""
        container?.setBackgroundResource(R.drawable.bg_input_default)
    } else {
        errorView.visibility = View.VISIBLE
        errorView.setText(messageRes)
        container?.setBackgroundResource(R.drawable.bg_input_error)
    }
}
```

For create-screen EditTexts inside a parent LinearLayout, pass that parent as `container`. For accordion flat EditTexts, pass the EditText itself as container. Gender: only toggle error TextView (no chip background forced to error unless simple).

- [ ] **Step 4: Commit** `feat: add input error drawable and form helper`

---

### Task 3: Create Personal Account screen

**Files:**
- Modify: `activity_create_personal_account.xml`
- Modify: `CreatePersonalAccountViewModel.kt`
- Modify: `CreatePersonalAccountActivity.kt`

- [ ] **Step 1: Layout** — After each field (name container, email container, dobContainer, gender row): add `tvFullNameError`, `tvEmailError`, `tvDobError`, `tvGenderError` (12sp, `@color/error`, `visibility=gone`). `etFullName` `android:maxLength="40"`. Chip Male default `bg_chip_default` (not selected). Give name/email containers ids: `fullNameContainer`, `emailContainer`.

- [ ] **Step 2: ViewModel**

```kotlin
private val _selectedGender = MutableLiveData<Gender?>(null)
private val _fieldErrors = MutableLiveData(PersonalFieldErrors())
val fieldErrors: LiveData<PersonalFieldErrors> = _fieldErrors

fun clearFullNameError() { _fieldErrors.value = _fieldErrors.value?.copy(fullName = null) }
// similarly clearEmailError, clearDobError, clearGenderError

fun onContinueClicked(fullName: String, email: String) {
    val errors = PersonalDetailsValidator.validate(
        fullName, email, _dateOfBirth.value.orEmpty(), _selectedGender.value
    )
    _fieldErrors.value = errors
    if (errors.hasErrors) return
    KycProgressRepository.savePersonal(...)
    _navigateNext.value = true
}
```

Remove Toast-driven `_errorMessage` for these validations (or leave unused).

- [ ] **Step 3: Activity** — Observe `fieldErrors` and apply via helpers. Text watchers / gender / DOB clear respective errors. `renderGender(null)` → all chips default. Hydrate draft gender only if non-null. Remove Toast observer for validation (keep if still used elsewhere — it won’t be).

- [ ] **Step 4: Manual check** — Launch Create Personal Account; Continue empty → four errors; fix progressively; valid → Progress with accordion filled.

- [ ] **Step 5: Commit** `feat: inline personal details validation on create account`

---

### Task 4: KYC Progress Personal accordion

**Files:**
- Modify: `item_kyc_step.xml` (personal form section)
- Modify: `KycProgressViewModel.kt`
- Modify: `KycProgressActivity.kt`

- [ ] **Step 1: Layout** — Under `etPersonalName`, `etPersonalEmail`, `tvPersonalDob`, gender chips: add matching error TextViews; `etPersonalName` maxLength 40.

- [ ] **Step 2: ViewModel**

```kotlin
private val _personalFieldErrors = MutableLiveData(PersonalFieldErrors())
val personalFieldErrors: LiveData<PersonalFieldErrors> = _personalFieldErrors

fun clearPersonalFieldError(field: ...) // or clearPersonalErrors() + partial clears

fun submitPersonal(...) {
    val errors = PersonalDetailsValidator.validate(fullName, email, dob, gender)
    _personalFieldErrors.value = errors
    if (errors.hasErrors) return
    savePersonalDraft(...)
    markCompleted(PERSONAL)
    _personalFieldErrors.value = PersonalFieldErrors()
    _inlineEditStep.value = null
    refresh()
}
```

Remove Patterns/required Toast path for personal.

- [ ] **Step 3: Activity** — After `bindPersonalSection`, apply `personalFieldErrors` to the personal item views when step is PERSONAL. On text/gender/DOB change, clear that error via ViewModel. Observe `personalFieldErrors` to re-render errors without full step rebuild if possible; if steps rebind wipes listeners, re-apply errors at end of `bindPersonalSection` from `viewModel.personalFieldErrors.value`.

- [ ] **Step 4: Verify** — Create → Progress shows saved data; Edit → invalidate email domain → inline error; fix → Submit succeeds.

- [ ] **Step 5: Commit** `feat: validate personal accordion edits with shared rules`

---

### Task 5: Smoke verification

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest --tests com.example.transcilmobileapp.kyc.PersonalDetailsValidatorTest`
- [ ] **Step 2:** Confirm gender unselected on fresh Create screen; allowlisted email required; under-18 blocked; accordion hydrate + edit works.
- [ ] **Step 3:** Final commit if any leftover polish

---

## Self-review

1. Spec coverage: validation rules, inline UX, repository save/hydrate/edit, no gender default, unit tests — all tasked.  
2. No placeholders.  
3. Types: `PersonalFieldErrors` / `PersonalDetailsValidator.validate` consistent across tasks.
