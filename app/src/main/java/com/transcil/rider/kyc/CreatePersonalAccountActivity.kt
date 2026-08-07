/**
 * Personal-details form — first KYC step after journey selection (name, email, DOB, gender).
 * Saves profile via PATCH /me/profile, marks PERSONAL complete, then opens KycProgressActivity.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.transcil.rider.databinding.ActivityCreatePersonalAccountBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.core.Gender
import com.transcil.rider.core.SegmentedStepper
import com.transcil.rider.core.UiFormHelpers

class CreatePersonalAccountActivity :
    BaseActivity<ActivityCreatePersonalAccountBinding>(ActivityCreatePersonalAccountBinding::inflate) {

    private val viewModel: CreatePersonalAccountViewModel by viewModels()
    // `private val`: formatter created once per Activity instance; UTC avoids timezone drift on DOB.
    private val dobFormat = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.navyHeader.findViewById<TextView>(R.id.headerTitle).setText(R.string.personal_tell_us)
        binding.navyHeader.findViewById<TextView>(R.id.headerSubtitle).setText(R.string.personal_why_we_ask)
        SegmentedStepper.apply(binding.navyHeader, filledCount = 4, navyInactive = true)

        binding.ivBack.setOnClickListener { finish() }
        binding.chipMale.setOnClickListener { viewModel.onGenderSelected(Gender.MALE) }
        binding.chipFemale.setOnClickListener { viewModel.onGenderSelected(Gender.FEMALE) }
        binding.chipOther.setOnClickListener { viewModel.onGenderSelected(Gender.OTHER) }
        binding.dobContainer.setOnClickListener { showDatePicker() }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked(
                binding.etFullName.text.toString(),
                binding.etEmail.text.toString()
            )
        }

        UiFormHelpers.bindFocusHighlight(binding.etFullName)
        UiFormHelpers.bindFocusHighlight(binding.etEmail)

        binding.etFullName.doAfterTextChanged { viewModel.clearFullNameError() }
        binding.etEmail.doAfterTextChanged { viewModel.clearEmailError() }

        viewModel.load()
        viewModel.prefill.observe(this) { prefill ->
            if (prefill == null) return@observe
            if (prefill.fullName.isNotBlank()) binding.etFullName.setText(prefill.fullName)
            if (prefill.email.isNotBlank()) binding.etEmail.setText(prefill.email)
        }

        viewModel.selectedGender.observe(this, ::renderGender)
        viewModel.dateOfBirth.observe(this) { value ->
            if (!value.isNullOrBlank()) {
                binding.tvDob.text = value
                binding.tvDob.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                binding.dobContainer.setBackgroundResource(R.drawable.bg_input_focused)
            }
        }
        viewModel.fieldErrors.observe(this, ::renderFieldErrors)
        viewModel.isLoading.observe(this) { loading ->
            binding.btnContinue.isEnabled = loading != true
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                FeedbackUi.toast(this, message)
                viewModel.clearError()
            }
        }
        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                KycFlowNavigator.openProgress(this)
            }
        }
    }

    private fun renderFieldErrors(errors: PersonalFieldErrors?) {
        val value = errors ?: PersonalFieldErrors()
        UiFormHelpers.setFieldError(binding.tvFullNameError, binding.fullNameContainer, value.fullName)
        UiFormHelpers.setFieldError(binding.tvEmailError, binding.emailContainer, value.email)
        UiFormHelpers.setFieldError(binding.tvDobError, binding.dobContainer, value.dateOfBirth)
        if (value.dateOfBirth == null && !viewModel.dateOfBirth.value.isNullOrBlank()) {
            binding.dobContainer.setBackgroundResource(R.drawable.bg_input_focused)
        }
        UiFormHelpers.setFieldError(binding.tvGenderError, null, value.gender)
    }

    /** `private fun`: highlights selected gender chip and resets others to default background. */
    private fun renderGender(gender: Gender?) {
        binding.chipMale.setBackgroundResource(
            if (gender == Gender.MALE) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
        binding.chipFemale.setBackgroundResource(
            if (gender == Gender.FEMALE) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
        binding.chipOther.setBackgroundResource(
            if (gender == Gender.OTHER) R.drawable.bg_chip_selected else R.drawable.bg_chip_default
        )
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.dob_label)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            viewModel.onDateOfBirthSelected(dobFormat.format(Date(millis)))
        }
        picker.show(supportFragmentManager, "dob_picker")
    }
}
