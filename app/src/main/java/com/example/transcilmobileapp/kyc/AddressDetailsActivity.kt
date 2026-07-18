package com.example.transcilmobileapp.kyc

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.example.transcilmobileapp.databinding.ActivityAddressDetailsBinding

import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.UiFormHelpers

class AddressDetailsActivity :
    BaseActivity<ActivityAddressDetailsBinding>(ActivityAddressDetailsBinding::inflate) {

    private val viewModel: AddressDetailsViewModel by viewModels()
    private var bindingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 2)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine1)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine2)

        setupStateSpinner()
        hydrateFromDraft()

        binding.ivBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked(
                line1 = binding.etAddressLine1.text.toString(),
                line2 = binding.etAddressLine2.text.toString(),
                city = binding.etCity.text.toString(),
                state = selectedState(),
                pincode = binding.etPincode.text.toString()
            )
        }

        binding.etAddressLine1.doAfterTextChanged { viewModel.clearLine1Error() }
        binding.etAddressLine2.doAfterTextChanged { viewModel.clearLine2Error() }
        binding.etCity.doAfterTextChanged { viewModel.clearCityError() }
        binding.etPincode.doAfterTextChanged { viewModel.clearPincodeError() }

        viewModel.fieldErrors.observe(this, ::renderFieldErrors)
        viewModel.navigateNext.observe(this) { go ->
            if (go == true) {
                KycProgressRepository.markCompleted(KycStep.ADDRESS)
                KycFlowNavigator.openProgress(this)
            }
        }
    }

    private fun setupStateSpinner() {
        binding.spinnerState.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            IndianStates.ALL_WITH_PLACEHOLDER
        )
        binding.spinnerState.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (!bindingInProgress) {
                        viewModel.clearStateError()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun hydrateFromDraft() {
        val draft = KycProgressRepository.addressDraft()
        bindingInProgress = true
        if (draft.line1.isNotBlank()) binding.etAddressLine1.setText(draft.line1)
        if (draft.line2.isNotBlank()) binding.etAddressLine2.setText(draft.line2)
        if (draft.city.isNotBlank()) binding.etCity.setText(draft.city)
        if (draft.pincode.isNotBlank()) binding.etPincode.setText(draft.pincode)
        binding.spinnerState.setSelection(IndianStates.indexOf(draft.state))
        bindingInProgress = false
    }

    private fun selectedState(): String {
        val selected = binding.spinnerState.selectedItem?.toString().orEmpty()
        return if (selected == IndianStates.PLACEHOLDER) "" else selected
    }

    private fun renderFieldErrors(errors: AddressFieldErrors?) {
        val value = errors ?: AddressFieldErrors()
        UiFormHelpers.setFieldError(
            binding.tvAddressLine1Error,
            binding.addressLine1Container,
            value.line1
        )
        UiFormHelpers.setFieldError(
            binding.tvAddressLine2Error,
            binding.addressLine2Container,
            value.line2
        )
        UiFormHelpers.setFieldError(binding.tvCityError, binding.etCity, value.city)
        UiFormHelpers.setFieldError(binding.tvStateError, binding.spinnerState, value.state)
        UiFormHelpers.setFieldError(binding.tvPincodeError, binding.etPincode, value.pincode)
    }
}
