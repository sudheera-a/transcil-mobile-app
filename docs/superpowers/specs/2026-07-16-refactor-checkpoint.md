# Codebase structure (post-refactor)

```
com.example.transcilmobileapp
├── core/       BaseActivity, BaseViewModel, UiFormHelpers, models, NavExtras, KycNavigator
├── splash/     MainActivity
├── onboarding/ Onboarding 1–4
├── auth/       Welcome, Verify OTP
├── journey/    Choose Journey
└── kyc/        Personal → Address → Aadhaar → PAN → Bank → Pending/Approved
```

**Flow end:** Bank → `KycNavigator.openAfterSubmission` → Pending (stub).  
**Later API:** call `KycNavigator.openForStatus(context, APPROVED|PENDING)`.

**Paused before:** Home/Dashboard, 3PL form, real networking.
