package com.example.transcilmobileapp.kyc

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType

class KycProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val _summaryTitleRes = MutableLiveData<Int>()
    val summaryTitleRes: LiveData<Int> = _summaryTitleRes

    private val _badgeText = MutableLiveData<String>()
    val badgeText: LiveData<String> = _badgeText

    private val _percent = MutableLiveData<Int>()
    val percent: LiveData<Int> = _percent

    private val _steps = MutableLiveData<List<KycStepUi>>()
    val steps: LiveData<List<KycStepUi>> = _steps

    private val _expandedStep = MutableLiveData<KycStep?>()
    val expandedStep: LiveData<KycStep?> = _expandedStep

    private val _inlineEditStep = MutableLiveData<KycStep?>()
    val inlineEditStep: LiveData<KycStep?> = _inlineEditStep

    private val _navigateToStep = MutableLiveData<KycStep?>()
    val navigateToStep: LiveData<KycStep?> = _navigateToStep

    private val _showStubMessage = MutableLiveData<Int?>()
    val showStubMessage: LiveData<Int?> = _showStubMessage

    fun refresh() {
        val journey = KycProgressRepository.currentJourney() ?: return
        _summaryTitleRes.value = when (journey) {
            JourneyType.RENT_EV -> R.string.kyc_progress_rent_title
            JourneyType.THREE_PL -> R.string.kyc_progress_rider_title
        }
        val completed = KycProgressRepository.completedCount()
        val total = KycProgressRepository.totalCount()
        _badgeText.value = getApplication<Application>().getString(
            R.string.kyc_progress_complete_badge,
            completed,
            total
        )
        _percent.value = KycProgressRepository.progressPercent()
        _steps.value = KycProgressRepository.uiSteps()

        val currentExpanded = _expandedStep.value
        val stillValid = currentExpanded != null && KycProgressRepository.canOpen(currentExpanded)
        if (!stillValid) {
            _expandedStep.value = KycProgressRepository.inProgressStep()
            _inlineEditStep.value = null
        }
    }

    fun onHeaderClicked(step: KycStep) {
        if (!KycProgressRepository.canOpen(step)) {
            _showStubMessage.value = R.string.kyc_step_locked
            return
        }
        if (_expandedStep.value == step) {
            _expandedStep.value = null
            _inlineEditStep.value = null
        } else {
            _expandedStep.value = step
            _inlineEditStep.value = null
        }
    }

    fun onPrimaryAction(step: KycStep, status: KycStepStatus) {
        if (!KycProgressRepository.canOpen(step)) {
            _showStubMessage.value = R.string.kyc_step_locked
            return
        }

        if (status == KycStepStatus.COMPLETED && !isInlineEditing(step)) {
            when (step) {
                KycStep.PERSONAL,
                KycStep.ADDRESS,
                KycStep.REFERENCE,
                KycStep.OTHER_DOCS -> {
                    _inlineEditStep.value = step
                    _expandedStep.value = step
                }
                KycStep.AADHAAR -> {
                    // Re-open Aadhaar entry (OTP must be requested again after edit).
                    val draft = KycProgressRepository.aadhaarDraft()
                    saveAadhaarDraft(
                        aadhaarNumber = draft.aadhaarNumber,
                        consent = draft.consent,
                        otpSent = false,
                        otp = ""
                    )
                    _inlineEditStep.value = step
                    _expandedStep.value = step
                    refresh()
                }
                else -> _navigateToStep.value = step
            }
            return
        }

        // Inline submits are handled by dedicated methods from the Activity.
        when (step) {
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.AADHAAR,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS -> Unit
            else -> _navigateToStep.value = step
        }
    }

    fun onSecondaryAction(step: KycStep) {
        if (step != KycStep.OTHER_DOCS) return
        if (!KycProgressRepository.canOpen(step)) {
            _showStubMessage.value = R.string.kyc_step_locked
            return
        }
        _showStubMessage.value = R.string.kyc_attach_stub
    }

    fun savePersonalDraft(fullName: String, email: String, dob: String, gender: Gender?) {
        KycProgressRepository.savePersonal(
            PersonalDraft(
                fullName = fullName.trim(),
                email = email.trim(),
                dateOfBirth = dob.trim(),
                gender = gender
            )
        )
    }

    fun saveAddressDraft(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ) {
        KycProgressRepository.saveAddress(
            AddressDraft(
                line1 = line1.trim(),
                line2 = line2.trim(),
                city = city.trim(),
                state = state.trim(),
                pincode = pincode.filter { it.isDigit() }.take(6)
            )
        )
    }

    fun saveAadhaarDraft(
        aadhaarNumber: String,
        consent: Boolean,
        otpSent: Boolean = KycProgressRepository.aadhaarDraft().otpSent,
        otp: String = KycProgressRepository.aadhaarDraft().otp
    ) {
        KycProgressRepository.saveAadhaar(
            AadhaarDraft(
                aadhaarNumber = aadhaarNumber.filter { it.isDigit() }.take(12),
                consent = consent,
                otpSent = otpSent,
                otp = otp.filter { it.isDigit() }.take(6)
            )
        )
    }

    fun saveReferenceDraft(relation: String, mobile: String) {
        KycProgressRepository.saveReference(relation, mobile.filter { it.isDigit() }.take(10))
    }

    fun saveOtherDocsDraft(documentType: String, documentNumber: String) {
        KycProgressRepository.saveOtherDocs(documentType, documentNumber)
    }

    fun submitPersonal(fullName: String, email: String, dob: String, gender: Gender?) {
        if (fullName.isBlank() || email.isBlank() || dob.isBlank() || gender == null) {
            _showStubMessage.value = R.string.error_required_fields
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _showStubMessage.value = R.string.error_invalid_email
            return
        }
        savePersonalDraft(fullName, email, dob, gender)
        KycProgressRepository.markCompleted(KycStep.PERSONAL)
        _inlineEditStep.value = null
        refresh()
    }

    fun submitAddress(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ) {
        if (line1.isBlank() || city.isBlank() || state.isBlank() || pincode.isBlank()) {
            _showStubMessage.value = R.string.error_required_fields
            return
        }
        if (pincode.filter { it.isDigit() }.length != 6) {
            _showStubMessage.value = R.string.error_invalid_pincode
            return
        }
        saveAddressDraft(line1, line2, city, state, pincode)
        KycProgressRepository.markCompleted(KycStep.ADDRESS)
        _inlineEditStep.value = null
        refresh()
    }

    fun submitAadhaarNumber(aadhaarNumber: String, consent: Boolean) {
        val digits = aadhaarNumber.filter { it.isDigit() }
        if (digits.length != 12) {
            _showStubMessage.value = R.string.error_invalid_aadhaar
            return
        }
        if (!consent) {
            _showStubMessage.value = R.string.error_aadhaar_consent
            return
        }
        saveAadhaarDraft(digits, consent = true, otpSent = true, otp = "")
        _inlineEditStep.value = KycStep.AADHAAR
        _expandedStep.value = KycStep.AADHAAR
        refresh()
    }

    fun submitAadhaarOtp(otp: String) {
        val draft = KycProgressRepository.aadhaarDraft()
        val digits = otp.filter { it.isDigit() }
        if (!draft.otpSent) {
            _showStubMessage.value = R.string.error_invalid_aadhaar
            return
        }
        if (digits.length != 6) {
            _showStubMessage.value = R.string.error_incomplete_otp
            return
        }
        saveAadhaarDraft(
            aadhaarNumber = draft.aadhaarNumber,
            consent = draft.consent,
            otpSent = true,
            otp = digits
        )
        KycProgressRepository.markCompleted(KycStep.AADHAAR)
        _inlineEditStep.value = null
        refresh()
    }

    fun submitReference(relation: String, mobile: String) {
        val digits = mobile.filter { it.isDigit() }
        if (digits.length != 10) {
            _showStubMessage.value = R.string.kyc_error_reference_mobile
            return
        }
        saveReferenceDraft(relation, digits)
        KycProgressRepository.markCompleted(KycStep.REFERENCE)
        _inlineEditStep.value = null
        refresh()
    }

    fun submitOtherDocs(documentType: String, documentNumber: String) {
        val number = documentNumber.trim()
        if (number.length < 4) {
            _showStubMessage.value = R.string.kyc_error_other_doc_number
            return
        }
        saveOtherDocsDraft(documentType, number)
        KycProgressRepository.markCompleted(KycStep.OTHER_DOCS)
        _inlineEditStep.value = null
        refresh()
    }

    fun isInlineEditing(step: KycStep): Boolean = _inlineEditStep.value == step

    fun showStepForm(step: KycStep, status: KycStepStatus): Boolean {
        return when (step) {
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.AADHAAR,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS ->
                status == KycStepStatus.IN_PROGRESS ||
                    status == KycStepStatus.COMPLETED ||
                    isInlineEditing(step)
            else -> false
        }
    }

    fun isFormEditable(step: KycStep, status: KycStepStatus): Boolean =
        status == KycStepStatus.IN_PROGRESS || isInlineEditing(step)

    fun showsConsent(step: KycStep, status: KycStepStatus): Boolean =
        status == KycStepStatus.IN_PROGRESS && step == KycStep.BANK

    fun showsSecondary(step: KycStep, status: KycStepStatus): Boolean =
        step == KycStep.OTHER_DOCS && isFormEditable(step, status)

    fun hintRes(step: KycStep, status: KycStepStatus): Int {
        if (status == KycStepStatus.COMPLETED && !isInlineEditing(step)) {
            return R.string.kyc_accordion_hint_completed
        }
        return when (step) {
            KycStep.PERSONAL -> R.string.kyc_accordion_hint_personal
            KycStep.ADDRESS -> R.string.kyc_accordion_hint_address
            KycStep.AADHAAR -> R.string.kyc_accordion_hint_aadhaar
            KycStep.BANK -> R.string.kyc_accordion_hint_bank
            KycStep.REFERENCE -> R.string.kyc_accordion_hint_reference
            KycStep.OTHER_DOCS -> R.string.kyc_accordion_hint_other_docs
            KycStep.SELFIE -> R.string.kyc_accordion_hint_selfie
            KycStep.PAN -> R.string.kyc_accordion_hint_pan
        }
    }

    fun primaryActionRes(step: KycStep, status: KycStepStatus): Int {
        if (status == KycStepStatus.COMPLETED && !isInlineEditing(step)) {
            return R.string.kyc_action_edit
        }
        return when (step) {
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS,
            KycStep.PAN -> R.string.kyc_action_submit
            KycStep.AADHAAR -> {
                if (KycProgressRepository.aadhaarDraft().otpSent) {
                    R.string.verify_aadhaar_otp
                } else {
                    R.string.verify_aadhaar
                }
            }
            KycStep.BANK -> R.string.kyc_action_verify_digio
            KycStep.SELFIE -> R.string.kyc_action_capture_photo
        }
    }

    fun consentTextRes(step: KycStep): Int = when (step) {
        KycStep.BANK -> R.string.kyc_bank_consent
        else -> R.string.aadhaar_consent
    }

    fun maskedAadhaarSummary(): String {
        val digits = KycProgressRepository.aadhaarDraft().aadhaarNumber.filter { it.isDigit() }
        val last4 = if (digits.length >= 4) digits.takeLast(4) else "XXXX"
        return getApplication<Application>().getString(
            R.string.aadhaar_summary_masked,
            "XXXX XXXX $last4"
        )
    }

    fun onSkipClicked() {
        _showStubMessage.value = R.string.kyc_skip_stub
    }

    fun onContactSupport() {
        _showStubMessage.value = R.string.kyc_support_stub
    }

    fun clearNavigateToStep() {
        _navigateToStep.value = null
    }

    fun clearStubMessage() {
        _showStubMessage.value = null
    }
}
