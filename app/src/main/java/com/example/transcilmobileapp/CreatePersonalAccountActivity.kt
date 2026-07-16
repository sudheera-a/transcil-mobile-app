package com.example.transcilmobileapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.transcilmobileapp.databinding.ActivityCreatePersonalAccountBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CreatePersonalAccountActivity :
    BaseActivity<ActivityCreatePersonalAccountBinding>(ActivityCreatePersonalAccountBinding::inflate) {

    private val viewModel: CreatePersonalAccountViewModel by viewModels()
    private val dobFormat = SimpleDateFormat("dd - MM - yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        bindFocusHighlight(binding.etFullName)
        bindFocusHighlight(binding.etEmail)

        viewModel.selectedGender.observe(this, ::renderGender)
        viewModel.dateOfBirth.observe(this) { value ->
            if (!value.isNullOrBlank()) {
                binding.tvDob.text = value
                binding.tvDob.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                binding.dobContainer.setBackgroundResource(R.drawable.bg_input_focused)
            }
        }
        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                Toast.makeText(this, R.string.personal_account_saved_stub, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindFocusHighlight(editText: android.widget.EditText) {
        val container = editText.parent as android.view.View
        editText.setOnFocusChangeListener { _, hasFocus ->
            container.setBackgroundResource(
                if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input_default
            )
        }
    }

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
