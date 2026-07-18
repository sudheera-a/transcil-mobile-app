package com.example.transcilmobileapp.kyc

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.transcilmobileapp.databinding.ActivityKycProgressBinding
import com.example.transcilmobileapp.databinding.ItemKycStepBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.Gender
import com.example.transcilmobileapp.core.JourneyType
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.core.NavExtras
import com.example.transcilmobileapp.core.UiFormHelpers

class KycProgressActivity :
    BaseActivity<ActivityKycProgressBinding>(ActivityKycProgressBinding::inflate) {

    private val viewModel: KycProgressViewModel by viewModels()
    private val dobFormat = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val boundSteps = linkedMapOf<KycStep, ItemKycStepBinding>()
    private var bindingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.ivBack.setOnClickListener {
            persistAllVisibleDrafts()
            finish()
        }
        binding.tvSkip.setOnClickListener { viewModel.onSkipClicked() }
        binding.tvContactSupport.setOnClickListener { viewModel.onContactSupport() }

        viewModel.summaryTitleRes.observe(this) { resId ->
            if (resId != null) binding.tvSummaryTitle.setText(resId)
        }
        viewModel.badgeText.observe(this) { text ->
            binding.tvBadge.text = text
        }
        viewModel.percent.observe(this) { percent ->
            binding.tvPercent.text = getString(R.string.kyc_progress_percent_format, percent ?: 0)
            updateProgressFill(percent ?: 0)
        }
        viewModel.steps.observe(this) { renderSteps(it.orEmpty()) }
        viewModel.expandedStep.observe(this) { renderSteps(viewModel.steps.value.orEmpty()) }
        viewModel.inlineEditStep.observe(this) { renderSteps(viewModel.steps.value.orEmpty()) }
        viewModel.navigateToStep.observe(this) { step ->
            if (step != null) {
                openStep(step)
                viewModel.clearNavigateToStep()
            }
        }
        viewModel.showStubMessage.observe(this) { resId ->
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                viewModel.clearStubMessage()
            }
        }
        viewModel.navigateToHome.observe(this) { go ->
            if (go == true) {
                persistAllVisibleDrafts()
                KycNavigator.openHomeDashboard(this, KycStatus.PENDING)
                viewModel.clearNavigateToHome()
                finish()
            }
        }
        viewModel.personalFieldErrors.observe(this) { errors ->
            boundSteps[KycStep.PERSONAL]?.let { applyPersonalFieldErrors(it, errors) }
        }
        viewModel.addressFieldErrors.observe(this) { errors ->
            boundSteps[KycStep.ADDRESS]?.let { applyAddressFieldErrors(it, errors) }
        }
        viewModel.bankFieldErrors.observe(this) { errors ->
            boundSteps[KycStep.BANK]?.let { applyBankFieldErrors(it, errors) }
        }
        viewModel.otherDocsFieldErrors.observe(this) { errors ->
            boundSteps[KycStep.OTHER_DOCS]?.let { applyOtherDocsFieldErrors(it, errors) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onPause() {
        persistAllVisibleDrafts()
        super.onPause()
    }

    private fun updateProgressFill(percent: Int) {
        binding.progressTrack.post {
            val trackWidth = binding.progressTrack.width
            val fillWidth = (trackWidth * percent.coerceIn(0, 100) / 100f).toInt()
            val params = binding.progressFill.layoutParams
            params.width = fillWidth
            binding.progressFill.layoutParams = params
        }
    }

    private fun renderSteps(steps: List<KycStepUi>) {
        persistAllVisibleDrafts()
        binding.llSteps.removeAllViews()
        boundSteps.clear()
        val expanded = viewModel.expandedStep.value
        steps.forEachIndexed { index, stepUi ->
            val itemBinding = ItemKycStepBinding.inflate(layoutInflater, binding.llSteps, false)
            bindStepRow(itemBinding, stepUi, expanded == stepUi.step)
            boundSteps[stepUi.step] = itemBinding
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) {
                lp.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
            }
            binding.llSteps.addView(itemBinding.root, lp)
        }
    }

    private fun bindStepRow(
        itemBinding: ItemKycStepBinding,
        stepUi: KycStepUi,
        isExpanded: Boolean
    ) {
        val status = stepUi.status
        itemBinding.tvStepTitle.setText(stepUi.titleRes)
        itemBinding.tvExpandHint.setText(viewModel.hintRes(stepUi.step, status))
        itemBinding.btnPrimary.setText(viewModel.primaryActionRes(stepUi.step, status))

        when (status) {
            KycStepStatus.COMPLETED -> {
                itemBinding.cardRoot.setBackgroundResource(
                    if (isExpanded) R.drawable.bg_kyc_step_active else R.drawable.bg_kyc_step_default
                )
                itemBinding.ivStepIcon.setImageResource(R.drawable.ic_kyc_step_done)
                itemBinding.tvStepSubtitle.text =
                    stepUi.subtitle ?: getString(R.string.kyc_step_completed_just_now)
            }
            KycStepStatus.IN_PROGRESS -> {
                itemBinding.cardRoot.setBackgroundResource(R.drawable.bg_kyc_step_active)
                itemBinding.ivStepIcon.setImageResource(R.drawable.ic_kyc_step_progress)
                itemBinding.tvStepSubtitle.setText(R.string.kyc_step_in_progress)
            }
            KycStepStatus.PENDING -> {
                itemBinding.cardRoot.setBackgroundResource(R.drawable.bg_kyc_step_default)
                itemBinding.ivStepIcon.setImageResource(R.drawable.ic_kyc_step_pending)
                itemBinding.tvStepSubtitle.setText(R.string.kyc_step_pending)
            }
        }

        itemBinding.expandBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
        itemBinding.ivChevron.rotation = if (isExpanded) 180f else 0f
        itemBinding.headerRow.setOnClickListener {
            persistDraftFrom(itemBinding, stepUi.step)
            viewModel.onHeaderClicked(stepUi.step)
        }

        if (!isExpanded) return

        itemBinding.consentRow.visibility = View.GONE
        itemBinding.personalForm.visibility = View.GONE
        itemBinding.addressForm.visibility = View.GONE
        itemBinding.bankForm.visibility = View.GONE
        itemBinding.aadhaarForm.visibility = View.GONE
        itemBinding.referenceForm.visibility = View.GONE
        itemBinding.otherDocsForm.visibility = View.GONE
        itemBinding.tvCompletedSummary.visibility = View.GONE

        when (stepUi.step) {
            KycStep.PERSONAL -> bindPersonalSection(itemBinding, status)
            KycStep.ADDRESS -> bindAddressSection(itemBinding, status)
            KycStep.BANK -> bindBankSection(itemBinding, status)
            KycStep.AADHAAR -> bindAadhaarSection(itemBinding, status)
            KycStep.REFERENCE -> bindReferenceSection(itemBinding, status)
            KycStep.OTHER_DOCS -> bindOtherDocsSection(itemBinding, status)
            else -> Unit
        }

        val showSecondary = viewModel.showsSecondary(stepUi.step, status)
        itemBinding.btnSecondary.visibility = if (showSecondary) View.VISIBLE else View.GONE
        if (showSecondary) {
            itemBinding.btnSecondary.setText(R.string.kyc_action_capture_attach)
        }

        itemBinding.btnSecondary.setOnClickListener { viewModel.onSecondaryAction(stepUi.step) }
        itemBinding.btnPrimary.setOnClickListener {
            handlePrimaryClick(itemBinding, stepUi)
        }
    }

    private fun handlePrimaryClick(itemBinding: ItemKycStepBinding, stepUi: KycStepUi) {
        val status = stepUi.status
        val editable = viewModel.isFormEditable(stepUi.step, status)
        when {
            stepUi.step == KycStep.PERSONAL && editable -> {
                val gender = itemBinding.personalForm.tag as? Gender
                viewModel.submitPersonal(
                    itemBinding.etPersonalName.text?.toString().orEmpty(),
                    itemBinding.etPersonalEmail.text?.toString().orEmpty(),
                    itemBinding.tvPersonalDob.text?.toString().orEmpty(),
                    gender
                )
            }
            stepUi.step == KycStep.ADDRESS && editable -> {
                viewModel.submitAddress(
                    itemBinding.etAddressLine1.text?.toString().orEmpty(),
                    itemBinding.etAddressLine2.text?.toString().orEmpty(),
                    itemBinding.etAddressCity.text?.toString().orEmpty(),
                    selectedAddressState(itemBinding),
                    itemBinding.etAddressPincode.text?.toString().orEmpty()
                )
            }
            stepUi.step == KycStep.BANK && editable -> {
                viewModel.submitBank(
                    itemBinding.etBankHolderName.text?.toString().orEmpty(),
                    itemBinding.etBankAccountNumber.text?.toString().orEmpty(),
                    itemBinding.etBankConfirmAccount.text?.toString().orEmpty(),
                    itemBinding.etBankIfsc.text?.toString().orEmpty(),
                    itemBinding.cbBankConsent.isChecked
                )
            }
            stepUi.step == KycStep.AADHAAR && editable -> {
                persistDraftFrom(itemBinding, KycStep.AADHAAR)
                val draft = KycProgressRepository.aadhaarDraft()
                if (draft.otpSent) {
                    viewModel.submitAadhaarOtp(readAadhaarOtp(itemBinding))
                } else {
                    viewModel.submitAadhaarNumber(
                        itemBinding.etAadhaarNumber.text?.toString().orEmpty(),
                        itemBinding.cbAadhaarConsent.isChecked
                    )
                }
            }
            stepUi.step == KycStep.REFERENCE && editable -> {
                viewModel.submitReference(
                    itemBinding.spinnerRelation.selectedItem?.toString().orEmpty(),
                    itemBinding.etReferenceMobile.text?.toString().orEmpty()
                )
            }
            stepUi.step == KycStep.OTHER_DOCS && editable -> {
                viewModel.submitOtherDocs(
                    itemBinding.spinnerDocument.selectedItem?.toString().orEmpty(),
                    itemBinding.etDocNumber.text?.toString().orEmpty()
                )
            }
            else -> viewModel.onPrimaryAction(stepUi.step, status)
        }
    }

    private fun bindPersonalSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.PERSONAL, status)) return
        itemBinding.personalForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.PERSONAL, status)
        val draft = KycProgressRepository.personalDraft()

        bindingInProgress = true
        itemBinding.etPersonalName.setText(draft.fullName)
        itemBinding.etPersonalEmail.setText(draft.email)
        itemBinding.tvPersonalDob.text = draft.dateOfBirth.ifBlank { null }
        if (draft.dateOfBirth.isNotBlank()) {
            itemBinding.tvPersonalDob.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
        renderPersonalGender(itemBinding, draft.gender)
        itemBinding.personalForm.tag = draft.gender
        bindingInProgress = false

        setEditable(itemBinding.etPersonalName, editable)
        setEditable(itemBinding.etPersonalEmail, editable)
        itemBinding.tvPersonalDob.isClickable = editable
        itemBinding.tvPersonalDob.isEnabled = editable
        itemBinding.chipPersonalMale.isClickable = editable
        itemBinding.chipPersonalFemale.isClickable = editable
        itemBinding.chipPersonalOther.isClickable = editable

        if (editable) {
            watchText(itemBinding.etPersonalName) {
                viewModel.clearPersonalFullNameError()
                persistDraftFrom(itemBinding, KycStep.PERSONAL)
            }
            watchText(itemBinding.etPersonalEmail) {
                viewModel.clearPersonalEmailError()
                persistDraftFrom(itemBinding, KycStep.PERSONAL)
            }
            itemBinding.tvPersonalDob.setOnClickListener { showDobPicker(itemBinding) }
            itemBinding.chipPersonalMale.setOnClickListener {
                itemBinding.personalForm.tag = Gender.MALE
                renderPersonalGender(itemBinding, Gender.MALE)
                viewModel.clearPersonalGenderError()
                persistDraftFrom(itemBinding, KycStep.PERSONAL)
            }
            itemBinding.chipPersonalFemale.setOnClickListener {
                itemBinding.personalForm.tag = Gender.FEMALE
                renderPersonalGender(itemBinding, Gender.FEMALE)
                viewModel.clearPersonalGenderError()
                persistDraftFrom(itemBinding, KycStep.PERSONAL)
            }
            itemBinding.chipPersonalOther.setOnClickListener {
                itemBinding.personalForm.tag = Gender.OTHER
                renderPersonalGender(itemBinding, Gender.OTHER)
                viewModel.clearPersonalGenderError()
                persistDraftFrom(itemBinding, KycStep.PERSONAL)
            }
        } else {
            itemBinding.tvPersonalDob.setOnClickListener(null)
        }

        applyPersonalFieldErrors(itemBinding, viewModel.personalFieldErrors.value)
    }

    private fun applyPersonalFieldErrors(
        itemBinding: ItemKycStepBinding,
        errors: PersonalFieldErrors?
    ) {
        if (itemBinding.personalForm.visibility != View.VISIBLE) return
        val value = errors ?: PersonalFieldErrors()
        UiFormHelpers.setFieldError(
            itemBinding.tvPersonalNameError,
            itemBinding.etPersonalName,
            value.fullName
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvPersonalEmailError,
            itemBinding.etPersonalEmail,
            value.email
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvPersonalDobError,
            itemBinding.tvPersonalDob,
            value.dateOfBirth
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvPersonalGenderError,
            null,
            value.gender
        )
    }

    private fun renderPersonalGender(itemBinding: ItemKycStepBinding, gender: Gender?) {
        itemBinding.chipPersonalMale.setBackgroundResource(
            if (gender == Gender.MALE) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
        itemBinding.chipPersonalFemale.setBackgroundResource(
            if (gender == Gender.FEMALE) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
        itemBinding.chipPersonalOther.setBackgroundResource(
            if (gender == Gender.OTHER) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
    }

    private fun showDobPicker(itemBinding: ItemKycStepBinding) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.dob_label)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val value = dobFormat.format(Date(millis))
            itemBinding.tvPersonalDob.text = value
            itemBinding.tvPersonalDob.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            viewModel.clearPersonalDobError()
            persistDraftFrom(itemBinding, KycStep.PERSONAL)
        }
        picker.show(supportFragmentManager, "kyc_personal_dob")
    }

    private fun bindAddressSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.ADDRESS, status)) return
        itemBinding.addressForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.ADDRESS, status)
        val draft = KycProgressRepository.addressDraft()

        itemBinding.spinnerAddressState.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            IndianStates.ALL_WITH_PLACEHOLDER
        )

        bindingInProgress = true
        itemBinding.etAddressLine1.setText(draft.line1)
        itemBinding.etAddressLine2.setText(draft.line2)
        itemBinding.etAddressCity.setText(draft.city)
        itemBinding.etAddressPincode.setText(draft.pincode)
        itemBinding.spinnerAddressState.setSelection(IndianStates.indexOf(draft.state))
        bindingInProgress = false

        setEditable(itemBinding.etAddressLine1, editable)
        setEditable(itemBinding.etAddressLine2, editable)
        setEditable(itemBinding.etAddressCity, editable)
        setEditable(itemBinding.etAddressPincode, editable)
        itemBinding.spinnerAddressState.isEnabled = editable

        if (editable) {
            watchText(itemBinding.etAddressLine1) {
                viewModel.clearAddressLine1Error()
                persistDraftFrom(itemBinding, KycStep.ADDRESS)
            }
            watchText(itemBinding.etAddressLine2) {
                viewModel.clearAddressLine2Error()
                persistDraftFrom(itemBinding, KycStep.ADDRESS)
            }
            watchText(itemBinding.etAddressCity) {
                viewModel.clearAddressCityError()
                persistDraftFrom(itemBinding, KycStep.ADDRESS)
            }
            watchText(itemBinding.etAddressPincode) {
                viewModel.clearAddressPincodeError()
                persistDraftFrom(itemBinding, KycStep.ADDRESS)
            }
            itemBinding.spinnerAddressState.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (!bindingInProgress) {
                            viewModel.clearAddressStateError()
                            persistDraftFrom(itemBinding, KycStep.ADDRESS)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
        } else {
            itemBinding.spinnerAddressState.onItemSelectedListener = null
        }

        applyAddressFieldErrors(itemBinding, viewModel.addressFieldErrors.value)
    }

    private fun bindBankSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.BANK, status)) return
        itemBinding.bankForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.BANK, status)
        val draft = KycProgressRepository.bankDraft()

        bindingInProgress = true
        itemBinding.etBankHolderName.setText(draft.holderName)
        itemBinding.etBankAccountNumber.setText(draft.accountNumber)
        itemBinding.etBankConfirmAccount.setText(draft.confirmAccountNumber)
        itemBinding.etBankIfsc.setText(draft.ifsc)
        itemBinding.cbBankConsent.isChecked = draft.consent
        bindingInProgress = false

        setEditable(itemBinding.etBankHolderName, editable)
        setEditable(itemBinding.etBankAccountNumber, editable)
        setEditable(itemBinding.etBankConfirmAccount, editable)
        setEditable(itemBinding.etBankIfsc, editable)
        itemBinding.cbBankConsent.isEnabled = editable

        if (editable) {
            watchText(itemBinding.etBankHolderName) {
                viewModel.clearBankHolderError()
                persistDraftFrom(itemBinding, KycStep.BANK)
            }
            watchText(itemBinding.etBankAccountNumber) {
                viewModel.clearBankAccountError()
                persistDraftFrom(itemBinding, KycStep.BANK)
            }
            watchText(itemBinding.etBankConfirmAccount) {
                viewModel.clearBankConfirmError()
                persistDraftFrom(itemBinding, KycStep.BANK)
            }
            watchText(itemBinding.etBankIfsc) {
                viewModel.clearBankIfscError()
                persistDraftFrom(itemBinding, KycStep.BANK)
            }
            itemBinding.cbBankConsent.setOnCheckedChangeListener { _, _ ->
                if (!bindingInProgress) {
                    viewModel.clearBankConsentError()
                    persistDraftFrom(itemBinding, KycStep.BANK)
                }
            }
        } else {
            itemBinding.cbBankConsent.setOnCheckedChangeListener(null)
        }

        applyBankFieldErrors(itemBinding, viewModel.bankFieldErrors.value)
    }

    private fun applyBankFieldErrors(itemBinding: ItemKycStepBinding, errors: BankFieldErrors?) {
        if (itemBinding.bankForm.visibility != View.VISIBLE) return
        val value = errors ?: BankFieldErrors()
        UiFormHelpers.setFieldError(
            itemBinding.tvBankHolderError,
            itemBinding.etBankHolderName,
            value.holderName
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvBankAccountError,
            itemBinding.etBankAccountNumber,
            value.accountNumber
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvBankConfirmError,
            itemBinding.etBankConfirmAccount,
            value.confirmAccountNumber
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvBankIfscError,
            itemBinding.etBankIfsc,
            value.ifsc
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvBankConsentError,
            null,
            value.consent
        )
    }

    private fun selectedAddressState(itemBinding: ItemKycStepBinding): String {
        val selected = itemBinding.spinnerAddressState.selectedItem?.toString().orEmpty()
        return if (selected == IndianStates.PLACEHOLDER) "" else selected
    }

    private fun applyAddressFieldErrors(
        itemBinding: ItemKycStepBinding,
        errors: AddressFieldErrors?
    ) {
        if (itemBinding.addressForm.visibility != View.VISIBLE) return
        val value = errors ?: AddressFieldErrors()
        UiFormHelpers.setFieldError(
            itemBinding.tvAddressLine1Error,
            itemBinding.etAddressLine1,
            value.line1
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvAddressLine2Error,
            itemBinding.etAddressLine2,
            value.line2
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvAddressCityError,
            itemBinding.etAddressCity,
            value.city
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvAddressStateError,
            itemBinding.spinnerAddressState,
            value.state
        )
        UiFormHelpers.setFieldError(
            itemBinding.tvAddressPincodeError,
            itemBinding.etAddressPincode,
            value.pincode
        )
    }

    private fun bindAadhaarSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.AADHAAR, status)) return
        itemBinding.aadhaarForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.AADHAAR, status)
        val draft = KycProgressRepository.aadhaarDraft()
        val showCompletedSummary = status == KycStepStatus.COMPLETED && !editable

        if (showCompletedSummary) {
            itemBinding.tvCompletedSummary.visibility = View.VISIBLE
            itemBinding.tvCompletedSummary.text = viewModel.maskedAadhaarSummary()
        }

        bindingInProgress = true
        itemBinding.etAadhaarNumber.setText(formatAadhaar(draft.aadhaarNumber))
        itemBinding.cbAadhaarConsent.isChecked = draft.consent
        writeAadhaarOtp(itemBinding, draft.otp)
        bindingInProgress = false

        val showOtp = draft.otpSent && editable
        itemBinding.aadhaarOtpSection.visibility = if (showOtp) View.VISIBLE else View.GONE
        itemBinding.aadhaarOtpInfoRow.visibility =
            if (editable && !draft.otpSent) View.VISIBLE else View.GONE

        setEditable(itemBinding.etAadhaarNumber, editable && !draft.otpSent)
        itemBinding.cbAadhaarConsent.isEnabled = editable && !draft.otpSent
        aadhaarOtpFields(itemBinding).forEach { setEditable(it, editable && draft.otpSent) }

        if (editable) {
            watchText(itemBinding.etAadhaarNumber) {
                if (bindingInProgress) return@watchText
                val digits = itemBinding.etAadhaarNumber.text?.toString().orEmpty()
                    .filter { it.isDigit() }.take(12)
                val formatted = formatAadhaar(digits)
                if (itemBinding.etAadhaarNumber.text?.toString() != formatted) {
                    bindingInProgress = true
                    val cursor = formatted.length
                    itemBinding.etAadhaarNumber.setText(formatted)
                    itemBinding.etAadhaarNumber.setSelection(cursor.coerceAtMost(formatted.length))
                    bindingInProgress = false
                }
                persistDraftFrom(itemBinding, KycStep.AADHAAR)
            }
            itemBinding.cbAadhaarConsent.setOnCheckedChangeListener { _, _ ->
                if (!bindingInProgress) persistDraftFrom(itemBinding, KycStep.AADHAAR)
            }
            if (draft.otpSent) {
                wireAadhaarOtpNavigation(itemBinding)
            }
        } else {
            itemBinding.cbAadhaarConsent.setOnCheckedChangeListener(null)
        }
    }

    private fun bindReferenceSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.REFERENCE, status)) return
        itemBinding.referenceForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.REFERENCE, status)
        val relations = resources.getStringArray(R.array.kyc_relation_options)
        itemBinding.spinnerRelation.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            relations
        )
        val draft = KycProgressRepository.referenceDraft()
        val relationIndex = relations.indexOf(draft.relation).takeIf { it >= 0 } ?: 0

        bindingInProgress = true
        itemBinding.spinnerRelation.setSelection(relationIndex)
        itemBinding.etReferenceMobile.setText(draft.mobile)
        bindingInProgress = false

        itemBinding.spinnerRelation.isEnabled = editable
        setEditable(itemBinding.etReferenceMobile, editable)
        if (editable) {
            watchText(itemBinding.etReferenceMobile) {
                persistDraftFrom(itemBinding, KycStep.REFERENCE)
            }
            itemBinding.spinnerRelation.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (!bindingInProgress) {
                            persistDraftFrom(itemBinding, KycStep.REFERENCE)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
        }
    }

    private fun bindOtherDocsSection(itemBinding: ItemKycStepBinding, status: KycStepStatus) {
        if (!viewModel.showStepForm(KycStep.OTHER_DOCS, status)) return
        itemBinding.otherDocsForm.visibility = View.VISIBLE
        val editable = viewModel.isFormEditable(KycStep.OTHER_DOCS, status)
        val docs = resources.getStringArray(R.array.kyc_other_doc_options)
        itemBinding.spinnerDocument.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            docs
        )
        val draft = KycProgressRepository.otherDocsDraft()
        val docIndex = docs.indexOf(draft.documentType).takeIf { it >= 0 } ?: 0

        bindingInProgress = true
        itemBinding.spinnerDocument.setSelection(docIndex)
        itemBinding.etDocNumber.setText(draft.documentNumber)
        bindingInProgress = false

        applyOtherDocTypeUi(itemBinding, docs[docIndex])
        itemBinding.spinnerDocument.isEnabled = editable
        setEditable(itemBinding.etDocNumber, editable)
        if (editable) {
            watchText(itemBinding.etDocNumber) {
                viewModel.clearOtherDocsNumberError()
                persistDraftFrom(itemBinding, KycStep.OTHER_DOCS)
            }
            itemBinding.spinnerDocument.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        applyOtherDocTypeUi(itemBinding, docs[position])
                        viewModel.clearOtherDocsNumberError()
                        if (!bindingInProgress) {
                            persistDraftFrom(itemBinding, KycStep.OTHER_DOCS)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
        }

        applyOtherDocsFieldErrors(itemBinding, viewModel.otherDocsFieldErrors.value)
    }

    private fun applyOtherDocTypeUi(itemBinding: ItemKycStepBinding, docLabel: String) {
        itemBinding.tvDocNumberLabel.text = getString(R.string.kyc_other_doc_number_label, docLabel)
        val type = OtherDocumentType.fromLabel(docLabel) ?: return
        itemBinding.etDocNumber.hint = getString(OtherDocsValidator.hintRes(type))
        itemBinding.tvDocNumberHelper.setText(OtherDocsValidator.helperRes(type))
        itemBinding.etDocNumber.filters =
            arrayOf(InputFilter.LengthFilter(OtherDocsValidator.maxLength(type)))
        val current = itemBinding.etDocNumber.text?.toString().orEmpty()
        val max = OtherDocsValidator.maxLength(type)
        if (current.length > max) {
            itemBinding.etDocNumber.setText(current.take(max))
        }
    }

    private fun applyOtherDocsFieldErrors(
        itemBinding: ItemKycStepBinding,
        errors: OtherDocsFieldErrors?
    ) {
        if (itemBinding.otherDocsForm.visibility != View.VISIBLE) return
        val value = errors ?: OtherDocsFieldErrors()
        UiFormHelpers.setFieldError(
            itemBinding.tvDocNumberError,
            itemBinding.etDocNumber,
            value.documentNumber ?: value.documentType
        )
    }

    private fun persistAllVisibleDrafts() {
        boundSteps.forEach { (step, itemBinding) ->
            persistDraftFrom(itemBinding, step)
        }
    }

    private fun persistDraftFrom(itemBinding: ItemKycStepBinding, step: KycStep) {
        if (bindingInProgress) return
        when (step) {
            KycStep.PERSONAL -> {
                if (itemBinding.personalForm.visibility != View.VISIBLE) return
                viewModel.savePersonalDraft(
                    itemBinding.etPersonalName.text?.toString().orEmpty(),
                    itemBinding.etPersonalEmail.text?.toString().orEmpty(),
                    itemBinding.tvPersonalDob.text?.toString().orEmpty(),
                    itemBinding.personalForm.tag as? Gender
                )
            }
            KycStep.ADDRESS -> {
                if (itemBinding.addressForm.visibility != View.VISIBLE) return
                viewModel.saveAddressDraft(
                    itemBinding.etAddressLine1.text?.toString().orEmpty(),
                    itemBinding.etAddressLine2.text?.toString().orEmpty(),
                    itemBinding.etAddressCity.text?.toString().orEmpty(),
                    selectedAddressState(itemBinding),
                    itemBinding.etAddressPincode.text?.toString().orEmpty()
                )
            }
            KycStep.BANK -> {
                if (itemBinding.bankForm.visibility != View.VISIBLE) return
                viewModel.saveBankDraft(
                    itemBinding.etBankHolderName.text?.toString().orEmpty(),
                    itemBinding.etBankAccountNumber.text?.toString().orEmpty(),
                    itemBinding.etBankConfirmAccount.text?.toString().orEmpty(),
                    itemBinding.etBankIfsc.text?.toString().orEmpty(),
                    itemBinding.cbBankConsent.isChecked
                )
            }
            KycStep.AADHAAR -> {
                if (itemBinding.aadhaarForm.visibility != View.VISIBLE) return
                val existing = KycProgressRepository.aadhaarDraft()
                viewModel.saveAadhaarDraft(
                    aadhaarNumber = itemBinding.etAadhaarNumber.text?.toString().orEmpty(),
                    consent = itemBinding.cbAadhaarConsent.isChecked,
                    otpSent = existing.otpSent,
                    otp = if (existing.otpSent) readAadhaarOtp(itemBinding) else existing.otp
                )
            }
            KycStep.REFERENCE -> {
                if (itemBinding.referenceForm.visibility != View.VISIBLE) return
                viewModel.saveReferenceDraft(
                    itemBinding.spinnerRelation.selectedItem?.toString().orEmpty(),
                    itemBinding.etReferenceMobile.text?.toString().orEmpty()
                )
            }
            KycStep.OTHER_DOCS -> {
                if (itemBinding.otherDocsForm.visibility != View.VISIBLE) return
                viewModel.saveOtherDocsDraft(
                    itemBinding.spinnerDocument.selectedItem?.toString().orEmpty(),
                    itemBinding.etDocNumber.text?.toString().orEmpty()
                )
            }
            else -> Unit
        }
    }

    private fun aadhaarOtpFields(itemBinding: ItemKycStepBinding): List<EditText> = listOf(
        itemBinding.etAadhaarOtp1,
        itemBinding.etAadhaarOtp2,
        itemBinding.etAadhaarOtp3,
        itemBinding.etAadhaarOtp4,
        itemBinding.etAadhaarOtp5,
        itemBinding.etAadhaarOtp6
    )

    private fun readAadhaarOtp(itemBinding: ItemKycStepBinding): String =
        aadhaarOtpFields(itemBinding).joinToString("") { it.text?.toString().orEmpty() }

    private fun writeAadhaarOtp(itemBinding: ItemKycStepBinding, otp: String) {
        val digits = otp.filter { it.isDigit() }.padEnd(6, ' ').take(6)
        aadhaarOtpFields(itemBinding).forEachIndexed { index, field ->
            val ch = digits.getOrNull(index)
            field.setText(if (ch != null && ch.isDigit()) ch.toString() else "")
        }
    }

    private fun wireAadhaarOtpNavigation(itemBinding: ItemKycStepBinding) {
        val fields = aadhaarOtpFields(itemBinding)
        fields.forEachIndexed { index, field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (bindingInProgress) return
                    if (s?.length == 1 && index < fields.lastIndex) {
                        fields[index + 1].requestFocus()
                    }
                    persistDraftFrom(itemBinding, KycStep.AADHAAR)
                }
            })
        }
    }

    private fun formatAadhaar(digits: String): String {
        val clean = digits.filter { it.isDigit() }.take(12)
        return buildString {
            clean.forEachIndexed { index, c ->
                if (index > 0 && index % 4 == 0) append(' ')
                append(c)
            }
        }
    }

    private fun watchText(editText: EditText, onChanged: () -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!bindingInProgress) onChanged()
            }
        })
    }

    private fun setEditable(editText: EditText, editable: Boolean) {
        editText.isEnabled = editable
        editText.isFocusable = editable
        editText.isFocusableInTouchMode = editable
    }

    private fun openStep(step: KycStep) {
        val journey = KycProgressRepository.currentJourney()
        when (step) {
            KycStep.PAN -> startStepActivity(PanVerificationActivity::class.java, journey)
            KycStep.SELFIE -> startStepActivity(SelfieVerificationActivity::class.java, journey)
            KycStep.BANK,
            KycStep.AADHAAR,
            KycStep.PERSONAL,
            KycStep.ADDRESS,
            KycStep.REFERENCE,
            KycStep.OTHER_DOCS -> Unit
        }
    }

    private fun startStepActivity(activityClass: Class<*>, journey: JourneyType?) {
        val intent = Intent(this, activityClass)
        if (journey != null) {
            intent.putExtra(NavExtras.JOURNEY_TYPE, journey.name)
        }
        startActivity(intent)
    }
}
