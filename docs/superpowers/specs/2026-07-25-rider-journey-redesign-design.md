# Rider Journey Redesign — Design Checklist

Source: `Transcil Rider Journey.html` (redesigned / right phone). Tokens: navy `#20283A`, green `#22B45B`, radii 20/16/14, Sora titles + Manrope body.

| Screen | Primary files | Acceptance |
|--------|---------------|------------|
| Splash | `activity_main.xml`, `MainActivity` | Navy, bolt motion, tagline, progress, version |
| Onboarding ×3 | `OnboardingActivity` | Full-bleed hero, Skip pill, segments, Next |
| Login | `activity_welcome.xml` | Navy header, +91 field, terms, encryption |
| OTP | `activity_verify_otp.xml` | Number+Edit, boxes, timer, auto-detect |
| Choose Journey | `activity_choose_journey.xml` | Radio tick cards, Delivery Rider copy |
| Personal Details | `activity_create_personal_account.xml` | Gender fill, DOB, why-we-ask, inline errors |
| KYC Progress | `activity_kyc_progress.xml` | Ring, Almost there, grouped steps |
| KYC panels | KYC layouts / accordion | Digio consent, selfie guide, doc slots |
| Vehicles | `VehiclesFragment` | Horizontal cards, filters, Select this EV |
| Rental Plans | `RentalPlansFragment` | Expandable plans, Review & pay |
| Payments | `payment/*` | Review → autopay → pending/success/fail → methods |
| Dashboard | `fragment_home_dashboard.xml` | Hero + Pay + earnings card |
| Nearby Hubs | `fragment_nearby_hubs.xml` | Map + list, availability states |
| Profile | `fragment_profile.xml` | Navy identity, VERIFIED, quiet menu |
| Wallet | `fragment_wallet.xml` | Balance, Add/Pay, week list |
| Settings | `fragment_settings.xml` | 3 notif toggles, prefs, delete |
| Help | `HelpFragment` | Search, roadside, channels, topics |
| Feedback | `FeedbackUi` | Severity dialogs + toasts + field errors |
