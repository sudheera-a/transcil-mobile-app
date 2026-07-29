package com.example.transcilmobileapp.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.data.model.onboarding.OnboardingData
import com.example.transcilmobileapp.repository.DigioKycRepository
import com.example.transcilmobileapp.repository.KycDocumentRepository
import com.example.transcilmobileapp.repository.OnboardingRepository
import kotlinx.coroutines.launch

class KycProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val onboardingRepository = OnboardingRepository()
    private val kycDocumentRepository = KycDocumentRepository()

    private var pendingDocBytes: ByteArray? = null
    private var pendingDocContentType: String = "image/jpeg"

    private val _summaryTitleRes = MutableLiveData<Int>()
    val summaryTitleRes: LiveData<Int> = _summaryTitleRes

    private val _badgeText = MutableLiveData<String>()
    val badgeText: LiveData<String> = _badgeText

    private val _stepsCountText = MutableLiveData<String>()
    val stepsCountText: LiveData<String> = _stepsCountText

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

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _openDigioUrl = MutableLiveData<String?>()
    val openDigioUrl: LiveData<String?> = _openDigioUrl

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> = _navigateToHome

    private val _openDocumentsStatus = MutableLiveData<KycStatus?>()
    val openDocumentsStatus: LiveData<KycStatus?> = _openDocumentsStatus

    private val _pickOtherDocsFile = MutableLiveData<Boolean>()
    val pickOtherDocsFile: LiveData<Boolean> = _pickOtherDocsFile

    private val _otherDocsFileLabel = MutableLiveData<String?>()
    val otherDocsFileLabel: LiveData<String?> = _otherDocsFileLabel

    private val _personalFieldErrors = MutableLiveData(PersonalFieldErrors())
    val personalFieldErrors: LiveData<PersonalFieldErrors> = _personalFieldErrors

    private val _addressFieldErrors = MutableLiveData(AddressFieldErrors())
    val addressFieldErrors: LiveData<AddressFieldErrors> = _addressFieldErrors

    private val _bankFieldErrors = MutableLiveData(BankFieldErrors())
    val bankFieldErrors: LiveData<BankFieldErrors> = _bankFieldErrors

    private val _otherDocsFieldErrors = MutableLiveData(OtherDocsFieldErrors())
    val otherDocsFieldErrors: LiveData<OtherDocsFieldErrors> = _otherDocsFieldErrors

    /**
     * @param allowStatusRedirect when false (Profile → Documents browse), stay on progress
     * even if the server already reports verified / in review. Completion flows keep this true.
     */
    fun refresh(allowStatusRedirect: Boolean = true) {
        viewModelScope.launch {
            onboardingRepository.getOnboarding()
                .onSuccess { data ->
                    OnboardingSync.apply(data)
                    if (allowStatusRedirect) {
                        statusRedirectFor(data)?.let { _openDocumentsStatus.value = it }
                    }
                }
            // Fallback when onboarding step fields omit reference contact details.
            onboardingRepository.getReference()
                .onSuccess { ref ->
                    OnboardingSync.applyReference(ref.relation, ref.mobileE164)
                }
            renderLocal()
        }
    }

    companion object {
        /** Pure gate used by [refresh]; null means stay on KYC progress. */
        fun statusRedirectFor(
            verified: Boolean,
            allComplete: Boolean,
            documentsOverall: String?,
        ): KycStatus? = when {
            verified -> KycStatus.APPROVED
            allComplete && documentsOverall.equals("in_progress", ignoreCase = true) ->
                KycStatus.PENDING
            else -> null
        }

        fun statusRedirectFor(data: OnboardingData): KycStatus? =
            statusRedirectFor(
                verified = data.documents?.verified == true,
                allComplete = data.allComplete,
                documentsOverall = data.documents?.overall,
            )

        /**
         * Show the API dropdown form while the user still needs to upload.
         * Lock only after a real submit (draft present + complete / server in_progress).
         */
        fun isOtherDocsFormEditable(
            status: KycStepStatus,
            hasSubmittedDraft: Boolean,
            inlineEditing: Boolean,
        ): Boolean {
            if (inlineEditing) return true
            if (hasSubmittedDraft) return false
            return status == KycStepStatus.IN_PROGRESS
        }
    }

    private fun renderLocal() {
        val journey = KycProgressRepository.currentJourney() ?: return
        _summaryTitleRes.value = when (journey) {
            JourneyType.RENT_EV -> R.string.kyc_progress_rent_title
            JourneyType.THREE_PL -> R.string.kyc_progress_rider_title
        }
        val completed = KycProgressRepository.completedCount()
        val total = KycProgressRepository.totalCount()
        val app = getApplication<Application>()
        _badgeText.value = app.getString(R.string.kyc_progress_fraction, completed, total)
        _stepsCountText.value = app.getString(R.string.kyc_progress_of, completed, total)
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
                KycStep.BANK,
                KycStep.REFERENCE,
                KycStep.OTHER_DOCS -> {
                    _inlineEditStep.value = step
                    _expandedStep.value = step
                }
                KycStep.AADHAAR -> {
                    // Re-open Aadhaar entry for Digio re-verify after edit.
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
            KycStep.BANK,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS -> Unit
            else -> _navigateToStep.value = step
        }
    }

    fun onSecondaryAction(step: KycStep) {
        // Other docs uses a dedicated attach button inside the form.
    }

    fun onAttachOtherDocs() {
        if (!KycProgressRepository.canOpen(KycStep.OTHER_DOCS)) {
            _showStubMessage.value = R.string.kyc_step_locked
            return
        }
        _pickOtherDocsFile.value = true
    }

    fun clearPickOtherDocsFile() {
        _pickOtherDocsFile.value = false
    }

    fun setPendingOtherDocsFile(
        bytes: ByteArray,
        contentType: String,
        displayName: String? = null,
    ) {
        if (bytes.isEmpty()) {
            _showStubMessage.value = R.string.kyc_attach_required
            return
        }
        val mime = contentType.ifBlank { "image/jpeg" }
        pendingDocBytes = bytes
        pendingDocContentType = mime
        _otherDocsFileLabel.value = attachmentLabel(bytes, mime, displayName)
        _toastMessage.value = getApplication<Application>().getString(R.string.kyc_attach_ready)
    }

    private fun attachmentLabel(
        bytes: ByteArray,
        contentType: String,
        displayName: String?,
    ): String {
        val name = displayName?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: when {
                contentType.startsWith("image/") -> "photo.jpg"
                contentType == "application/pdf" -> "document.pdf"
                else -> "document"
            }
        val kb = ((bytes.size + 1023) / 1024).coerceAtLeast(1)
        return getApplication<Application>().getString(R.string.kyc_attach_label, name, kb)
    }

    fun clearOtherDocsAttachment() {
        pendingDocBytes = null
        pendingDocContentType = "image/jpeg"
        _otherDocsFileLabel.value = null
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
        val type = OtherDocumentType.fromLabel(documentType)
        val normalized = if (type != null) {
            OtherDocsValidator.normalize(type, documentNumber)
        } else {
            documentNumber.trim()
        }
        KycProgressRepository.saveOtherDocs(documentType, normalized)
    }

    fun saveBankDraft(
        holderName: String,
        accountNumber: String,
        confirmAccountNumber: String,
        ifsc: String,
        consent: Boolean
    ) {
        KycProgressRepository.saveBank(
            BankDraft(
                holderName = holderName.trim(),
                accountNumber = accountNumber.filter { it.isDigit() }.take(18),
                confirmAccountNumber = confirmAccountNumber.filter { it.isDigit() }.take(18),
                ifsc = ifsc.trim().uppercase().take(11),
                consent = consent
            )
        )
    }

    fun clearBankHolderError() = clearBank { it.copy(holderName = null) }
    fun clearBankAccountError() = clearBank { it.copy(accountNumber = null) }
    fun clearBankConfirmError() = clearBank { it.copy(confirmAccountNumber = null) }
    fun clearBankIfscError() = clearBank { it.copy(ifsc = null) }
    fun clearBankConsentError() = clearBank { it.copy(consent = null) }

    private fun clearBank(transform: (BankFieldErrors) -> BankFieldErrors) {
        val current = _bankFieldErrors.value ?: BankFieldErrors()
        _bankFieldErrors.value = transform(current)
    }

    fun submitBank(
        holderName: String,
        accountNumber: String,
        confirmAccountNumber: String,
        ifsc: String,
        consent: Boolean
    ) {
        val errors = BankDetailsValidator.validate(
            holderName,
            accountNumber,
            confirmAccountNumber,
            ifsc,
            consent
        )
        _bankFieldErrors.value = errors
        if (errors.hasErrors) return

        // ponytail: no bank-verify API yet — local IFSC/format gate only; restore Digio when API lands.
        saveBankDraft(holderName, accountNumber, confirmAccountNumber, ifsc, consent)
        KycProgressRepository.markCompletedLocalOnly(
            KycStep.BANK,
            OnboardingSync.formatCompletedNow(),
        )
        _bankFieldErrors.value = BankFieldErrors()
        _inlineEditStep.value = null
        refresh()
    }

    fun clearPersonalFullNameError() {
        val current = _personalFieldErrors.value ?: PersonalFieldErrors()
        if (current.fullName != null) {
            _personalFieldErrors.value = current.copy(fullName = null)
        }
    }

    fun clearPersonalEmailError() {
        val current = _personalFieldErrors.value ?: PersonalFieldErrors()
        if (current.email != null) {
            _personalFieldErrors.value = current.copy(email = null)
        }
    }

    fun clearPersonalDobError() {
        val current = _personalFieldErrors.value ?: PersonalFieldErrors()
        if (current.dateOfBirth != null) {
            _personalFieldErrors.value = current.copy(dateOfBirth = null)
        }
    }

    fun clearPersonalGenderError() {
        val current = _personalFieldErrors.value ?: PersonalFieldErrors()
        if (current.gender != null) {
            _personalFieldErrors.value = current.copy(gender = null)
        }
    }

    fun submitPersonal(fullName: String, email: String, dob: String, gender: Gender?) {
        val errors = PersonalDetailsValidator.validate(fullName, email, dob, gender)
        _personalFieldErrors.value = errors
        if (errors.hasErrors || gender == null) return

        viewModelScope.launch {
            onboardingRepository.patchProfile(fullName, email, dob, gender)
                .onSuccess {
                    savePersonalDraft(fullName, email, dob, gender)
                    _personalFieldErrors.value = PersonalFieldErrors()
                    _inlineEditStep.value = null
                    refresh()
                }
                .onFailure { e ->
                    _toastMessage.value = e.message ?: "Failed to save profile"
                }
        }
    }

    fun clearAddressLine1Error() = clearAddress { it.copy(line1 = null) }
    fun clearAddressLine2Error() = clearAddress { it.copy(line2 = null) }
    fun clearAddressCityError() = clearAddress { it.copy(city = null) }
    fun clearAddressStateError() = clearAddress { it.copy(state = null) }
    fun clearAddressPincodeError() = clearAddress { it.copy(pincode = null) }

    private fun clearAddress(transform: (AddressFieldErrors) -> AddressFieldErrors) {
        val current = _addressFieldErrors.value ?: AddressFieldErrors()
        _addressFieldErrors.value = transform(current)
    }

    fun submitAddress(
        line1: String,
        line2: String,
        city: String,
        state: String,
        pincode: String
    ) {
        val errors = AddressDetailsValidator.validate(line1, line2, city, state, pincode)
        _addressFieldErrors.value = errors
        if (errors.hasErrors) return

        viewModelScope.launch {
            onboardingRepository.putAddress(line1, line2, city, state, pincode)
                .onSuccess {
                    saveAddressDraft(line1, line2, city, state, pincode)
                    _addressFieldErrors.value = AddressFieldErrors()
                    _inlineEditStep.value = null
                    refresh()
                }
                .onFailure { e ->
                    _toastMessage.value = e.message ?: "Failed to save address"
                }
        }
    }

    fun startDigioFromAadhaar(aadhaarNumber: String, consent: Boolean) {
        val digits = aadhaarNumber.filter { it.isDigit() }
        if (digits.length != 12) {
            _showStubMessage.value = R.string.error_invalid_aadhaar
            return
        }
        if (!consent) {
            _showStubMessage.value = R.string.error_aadhaar_consent
            return
        }
        saveAadhaarDraft(digits, consent = true, otpSent = false, otp = "")
        launchDigio()
    }

    private fun launchDigio() {
        val name = KycProgressRepository.personalDraft().fullName.trim()
        if (name.isBlank() || name.any { it.isDigit() }) {
            _showStubMessage.value = R.string.kyc_digio_need_personal_name
            return
        }
        viewModelScope.launch {
            DigioKycRepository().start(name)
                .onSuccess { _openDigioUrl.value = it.gatewayUrl }
                .onFailure {
                    _toastMessage.value = it.message?.takeIf { msg -> msg.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.kyc_digio_failed)
                }
        }
    }

    fun submitReference(relation: String, mobile: String) {
        val digits = mobile.filter { it.isDigit() }
        if (digits.length != 10) {
            _showStubMessage.value = R.string.kyc_error_reference_mobile
            return
        }
        saveReferenceDraft(relation, digits)
        viewModelScope.launch {
            onboardingRepository.putReference(relation, digits)
                .onSuccess {
                    _inlineEditStep.value = null
                    refresh()
                }
                .onFailure { e ->
                    _toastMessage.value = e.message ?: "Failed to save reference"
                }
        }
    }

    fun clearOtherDocsNumberError() {
        val current = _otherDocsFieldErrors.value ?: OtherDocsFieldErrors()
        if (current.documentNumber != null || current.documentType != null) {
            _otherDocsFieldErrors.value = OtherDocsFieldErrors()
        }
    }

    fun submitOtherDocs(documentType: String, documentNumber: String) {
        val errors = OtherDocsValidator.validate(documentType, documentNumber)
        _otherDocsFieldErrors.value = errors
        if (errors.hasErrors) return

        val type = OtherDocumentType.fromLabel(documentType) ?: return
        val docType = KycDocumentRepository.apiDocType(documentType) ?: return
        val normalized = OtherDocsValidator.normalize(type, documentNumber)
        val bytes = pendingDocBytes
        if (bytes == null || bytes.isEmpty()) {
            _showStubMessage.value = R.string.kyc_attach_required
            return
        }
        val holder = KycProgressRepository.personalDraft().fullName.trim().ifBlank { "Rider" }
        val mime = pendingDocContentType
        saveOtherDocsDraft(documentType, normalized)
        viewModelScope.launch {
            kycDocumentRepository.uploadAndSubmit(
                docType = docType,
                contentType = mime,
                bytes = bytes,
                docNumber = normalized,
                holderName = holder,
            ).onFailure { e ->
                handleOtherDocsUploadFailure(e, docType)
                return@launch
            }
            finishOtherDocsSubmitted()
        }
    }

    private fun finishOtherDocsSubmitted() {
        // Onboarding may keep other_docs as in_progress until review — don't leave an empty form.
        KycProgressRepository.markCompletedLocalOnly(
            KycStep.OTHER_DOCS,
            OnboardingSync.formatCompletedNow(),
        )
        clearOtherDocsAttachment()
        _otherDocsFieldErrors.value = OtherDocsFieldErrors()
        _inlineEditStep.value = null
        refresh()
    }

    private fun handleOtherDocsUploadFailure(e: Throwable, docType: String) {
        val msg = e.message.orEmpty()
        if (msg.contains("CONFLICT_KYC_DOC_PENDING")) {
            // Already pending review — treat as submitted so UI doesn't re-ask for files.
            viewModelScope.launch {
                val rejected = kycDocumentRepository.listDocuments().getOrNull()?.let { list ->
                    kycDocumentRepository.latestRejectionReason(list, docType)
                }
                if (rejected != null) {
                    _toastMessage.value = rejected
                    refresh()
                } else {
                    finishOtherDocsSubmitted()
                }
            }
        } else if (msg.startsWith("S3_UPLOAD_FAILED")) {
            // Keep attachments so user can retry without re-picking.
            _toastMessage.value =
                getApplication<Application>().getString(R.string.kyc_upload_failed_retry)
        } else {
            _toastMessage.value = e.message
                ?: getApplication<Application>().getString(R.string.kyc_upload_failed_retry)
        }
    }

    fun isInlineEditing(step: KycStep): Boolean = _inlineEditStep.value == step

    fun showStepForm(step: KycStep, status: KycStepStatus): Boolean {
        return when (step) {
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.AADHAAR,
            KycStep.BANK,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS ->
                status == KycStepStatus.IN_PROGRESS ||
                    status == KycStepStatus.COMPLETED ||
                    isInlineEditing(step)
            else -> false
        }
    }

    fun isFormEditable(step: KycStep, status: KycStepStatus): Boolean {
        if (step == KycStep.OTHER_DOCS) {
            val draft = KycProgressRepository.otherDocsDraft()
            val locked = draft.documentType.isNotBlank() &&
                draft.documentNumber.isNotBlank() &&
                (status == KycStepStatus.COMPLETED ||
                    KycProgressRepository.isServerInProgress(step))
            return isOtherDocsFormEditable(
                status = status,
                hasSubmittedDraft = locked,
                inlineEditing = isInlineEditing(step),
            )
        }
        return status == KycStepStatus.IN_PROGRESS || isInlineEditing(step)
    }

    fun showsConsent(step: KycStep, status: KycStepStatus): Boolean = false

    fun showsSecondary(step: KycStep, status: KycStepStatus): Boolean = false

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
        if (status == KycStepStatus.PENDING) {
            return R.string.kyc_action_start
        }
        return when (step) {
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS,
            KycStep.PAN -> R.string.kyc_action_submit
            KycStep.AADHAAR -> R.string.aadhaar_verify_digio
            KycStep.BANK -> R.string.kyc_action_submit
            KycStep.SELFIE -> R.string.selfie_take_cta
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
        _navigateToHome.value = true
    }

    fun onContactSupport() {
        _showStubMessage.value = R.string.kyc_support_stub
    }

    fun clearNavigateToStep() {
        _navigateToStep.value = null
    }

    fun clearNavigateToHome() {
        _navigateToHome.value = false
    }

    fun clearStubMessage() {
        _showStubMessage.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearOpenDigioUrl() {
        _openDigioUrl.value = null
    }

    fun clearOpenDocumentsStatus() {
        _openDocumentsStatus.value = null
    }
}
