# Address + Aadhaar Steps Implementation Plan

> **For agentic workers:** Use executing-plans / implement task-by-task.

**Goal:** Add Steps 2–3 of personal onboarding: Address Details, Aadhaar Verification, Aadhaar OTP.

**Architecture:** Three Activities + ViewModels; shared `include_step_progress`; reuse input/button drawables; Intent nav from existing Create Personal Account.

**Tech Stack:** Kotlin, XML Views, ViewBinding, Material3, LiveData.

## Flow

```
Create Personal Account (step 1, existing)
  → Address Details (step 2 of 4)
  → Aadhaar Verification (step 3) — Skip stubs past KYC for now
       → Aadhaar OTP (still step 3)
            → stub “step 4 coming soon”
```

## Tasks

1. Shared step progress include + strings/icons/info-box drawables
2. Address Details screen + wire Personal Account Continue
3. Aadhaar Verification + Aadhaar OTP screens + Manifest
4. assembleDebug + commit

## Constraints

- Tokens only (dimens/colors); strings.xml; mesh background
- Validation in ViewModels; no real UIDAI API
- Skip on Aadhaar → Toast stub (no step 4 UI yet)
