/**
 * ViewModel for selfie capture/review during KYC — uploads image to S3, submits doc, refreshes onboarding.
 * Drives a two-state UI (CAPTURE vs REVIEW) and navigates to approved/pending screens on success.
 */
package com.transcil.rider.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.transcil.rider.R
import com.transcil.rider.core.KycStatus
import com.transcil.rider.repository.KycDocumentRepository
import com.transcil.rider.repository.OnboardingRepository
import kotlinx.coroutines.launch

/** `enum class`: fixed set of UI modes — safer than string constants for when/coverage. */
enum class SelfieUiState {
    CAPTURE,
    REVIEW
}

class SelfieVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val kycDocumentRepository = KycDocumentRepository()
    private val onboardingRepository = OnboardingRepository()

    private var imageBytes: ByteArray? = null
    private var contentType: String = "image/jpeg"

    private val _uiState = MutableLiveData(SelfieUiState.CAPTURE)
    val uiState: LiveData<SelfieUiState> = _uiState

    private val _openDocumentsStatus = MutableLiveData<KycStatus?>()
    val openDocumentsStatus: LiveData<KycStatus?> = _openDocumentsStatus

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _rejectionReason = MutableLiveData<String?>()
    val rejectionReason: LiveData<String?> = _rejectionReason

    fun onCaptured(bytes: ByteArray, mime: String = "image/jpeg") {
        if (bytes.isEmpty()) {
            _toastMessage.value = getApplication<Application>().getString(R.string.kyc_attach_required)
            return
        }
        imageBytes = bytes
        contentType = mime.ifBlank { "image/jpeg" }
        _uiState.value = SelfieUiState.REVIEW
    }

    fun onRetake() {
        imageBytes = null
        _uiState.value = SelfieUiState.CAPTURE
    }

    fun onContinue() {
        if (_uiState.value != SelfieUiState.REVIEW) return
        val bytes = imageBytes
        if (bytes == null || bytes.isEmpty()) {
            _toastMessage.value = getApplication<Application>().getString(R.string.kyc_attach_required)
            return
        }
        val holder = KycProgressRepository.personalDraft().fullName.trim().ifBlank { "Rider" }
        // [viewModelScope.launch]: starts a coroutine tied to ViewModel lifecycle; call suspend repos inside.
        viewModelScope.launch {
            kycDocumentRepository.uploadAndSubmit(
                docType = "selfie",
                contentType = contentType,
                bytes = bytes,
                docNumber = "SELFIE",
                holderName = holder,
            ).onSuccess {
                onboardingRepository.getOnboarding()
                    .onSuccess { data ->
                        OnboardingSync.apply(data)
                        _openDocumentsStatus.value = when {
                            data.documents?.verified == true -> KycStatus.APPROVED
                            else -> KycStatus.PENDING
                        }
                    }
                    .onFailure {
                        _openDocumentsStatus.value = KycStatus.PENDING
                    }
            }.onFailure { e ->
                val msg = e.message.orEmpty()
                if (msg.contains("CONFLICT_KYC_DOC_PENDING")) {
                    kycDocumentRepository.listDocuments().onSuccess { list ->
                        _rejectionReason.value =
                            kycDocumentRepository.latestRejectionReason(list, "selfie")
                        _toastMessage.value = _rejectionReason.value
                            ?: getApplication<Application>().getString(R.string.kyc_doc_conflict)
                    }
                } else if (msg.startsWith("S3_UPLOAD_FAILED")) {
                    imageBytes = null
                    _uiState.value = SelfieUiState.CAPTURE
                    _toastMessage.value =
                        getApplication<Application>().getString(R.string.kyc_upload_failed_retry)
                } else {
                    _toastMessage.value = e.message
                        ?: getApplication<Application>().getString(R.string.kyc_upload_failed_retry)
                }
            }
        }
    }

    fun clearOpenDocumentsStatus() {
        _openDocumentsStatus.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
