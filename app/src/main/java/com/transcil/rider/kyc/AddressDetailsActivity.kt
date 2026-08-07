/**
 * Present-address form screen — step 2 in the Rent-EV KYC path (Address before Aadhaar).
 * Binds state/city spinners, validates input, saves to the server, then returns to KycProgressActivity.
 */
package com.transcil.rider.kyc

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.core.UiFormHelpers
import com.transcil.rider.data.model.onboarding.StateOption
import com.transcil.rider.databinding.ActivityAddressDetailsBinding

class AddressDetailsActivity :
    BaseActivity<ActivityAddressDetailsBinding>(ActivityAddressDetailsBinding::inflate) {

    // `by viewModels()`: Activity-scoped ViewModel survives config changes (rotation).
    private val viewModel: AddressDetailsViewModel by viewModels()
    // `private var`: toggled while programmatically updating the spinner to ignore spurious callbacks.
    private var bindingInProgress = false
    private var stateOptions: List<StateOption> = emptyList()

    /** `override fun`: Activity lifecycle entry — wire ViewBinding views to ViewModel LiveData observers. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UiFormHelpers.bindStepProgress(binding.stepProgress, activeStep = 2)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine1)
        UiFormHelpers.bindFocusHighlight(binding.etAddressLine2)

        setupStateSpinner()
        viewModel.load()

        // ViewBinding: `binding` comes from BaseActivity — type-safe access to layout views.
        binding.ivBack.setOnClickListener { finish() }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked(
                line1 = binding.etAddressLine1.text.toString(),
                line2 = binding.etAddressLine2.text.toString(),
                city = binding.etCity.text.toString(),
                stateOption = selectedStateOption(),
                pincode = binding.etPincode.text.toString(),
            )
        }

        binding.etAddressLine1.doAfterTextChanged { viewModel.clearLine1Error() }
        binding.etAddressLine2.doAfterTextChanged { viewModel.clearLine2Error() }
        binding.etCity.doAfterTextChanged { viewModel.clearCityError() }
        binding.etPincode.doAfterTextChanged { viewModel.clearPincodeError() }

        viewModel.states.observe(this, ::bindStates)
        viewModel.cities.observe(this, ::bindCities)
        viewModel.prefill.observe(this, ::applyPrefill)
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

    private fun setupStateSpinner() {
        binding.spinnerState.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (bindingInProgress) return
                    viewModel.onStateSelected(stateOptions.getOrNull(position))
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun bindStates(options: List<StateOption>?) {
        val rows = listOf(StateOption(code = "", name = IndianStates.PLACEHOLDER)) +
            (options ?: emptyList())
        stateOptions = rows
        bindingInProgress = true
        binding.spinnerState.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            rows,
        )
        viewModel.prefill.value?.let { applyStateSelection(it.state) }
        bindingInProgress = false
    }

    private fun bindCities(cities: List<String>?) {
        binding.etCity.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                cities.orEmpty(),
            )
        )
    }

    private fun applyPrefill(prefill: AddressPrefill?) {
        val value = prefill ?: return
        bindingInProgress = true
        if (value.line1.isNotBlank()) binding.etAddressLine1.setText(value.line1)
        if (value.line2.isNotBlank()) binding.etAddressLine2.setText(value.line2)
        if (value.city.isNotBlank()) binding.etCity.setText(value.city)
        if (value.pincode.isNotBlank()) binding.etPincode.setText(value.pincode)
        applyStateSelection(value.state)
        bindingInProgress = false
        selectedStateOption()?.let { viewModel.onStateSelected(it) }
    }

    private fun applyStateSelection(state: String) {
        if (state.isBlank() || stateOptions.isEmpty()) return
        val index = stateOptions.indexOfFirst {
            it.code.equals(state, ignoreCase = true) || it.name.equals(state, ignoreCase = true)
        }
        if (index >= 0) binding.spinnerState.setSelection(index)
    }

    private fun selectedStateOption(): StateOption? {
        val position = binding.spinnerState.selectedItemPosition
        return stateOptions.getOrNull(position)
    }

    private fun renderFieldErrors(errors: AddressFieldErrors?) {
        val value = errors ?: AddressFieldErrors()
        UiFormHelpers.setFieldError(
            binding.tvAddressLine1Error,
            binding.addressLine1Container,
            value.line1,
        )
        UiFormHelpers.setFieldError(
            binding.tvAddressLine2Error,
            binding.addressLine2Container,
            value.line2,
        )
        UiFormHelpers.setFieldError(binding.tvCityError, binding.etCity, value.city)
        UiFormHelpers.setFieldError(binding.tvStateError, binding.spinnerState, value.state)
        UiFormHelpers.setFieldError(binding.tvPincodeError, binding.etPincode, value.pincode)
    }
}
