/**
 * ViewModel for the rental payment demo flow — multi-step state machine (review → autopay → success).
 * Computes fee breakdown from RentalCatalog; no real payment gateway integration yet.
 */
package com.transcil.rider.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.transcil.rider.core.BaseViewModel
import com.transcil.rider.home.PlanType
import com.transcil.rider.home.RentalCatalog
import com.transcil.rider.home.VehicleModelId

class PaymentViewModel : BaseViewModel() {
    private val _step = MutableLiveData(PaymentStep.REVIEW)
    val step: LiveData<PaymentStep> = _step

    private val _breakdown = MutableLiveData<PaymentBreakdown>()
    val breakdown: LiveData<PaymentBreakdown> = _breakdown

    // `private lateinit var`: set once in bind() before any other method uses modelId/plan.
    private lateinit var modelId: VehicleModelId
    private lateinit var plan: PlanType

    fun bind(modelId: VehicleModelId, plan: PlanType) {
        this.modelId = modelId
        this.plan = plan
        _breakdown.value = PaymentBreakdown(
            rentPaise = RentalCatalog.pricePaise(modelId, plan),
            onboardingPaise = RentalCatalog.ONBOARDING_FEE_PAISE,
            depositPaise = RentalCatalog.SECURITY_DEPOSIT_PAISE,
        )
        _step.value = PaymentStep.REVIEW
    }

    fun payNow() {
        _step.value = PaymentStep.AUTOPAY
    }

    fun authoriseMandate() {
        _step.value = PaymentStep.PENDING
    }

    fun payManually() {
        _step.value = PaymentStep.SUCCESS
    }

    fun checkStatus(success: Boolean = true) {
        _step.value = if (success) PaymentStep.SUCCESS else PaymentStep.FAILURE
    }

    fun openMethods() {
        _step.value = PaymentStep.METHODS
    }

    fun retry() {
        _step.value = PaymentStep.REVIEW
    }

    fun maxAutopayPaise(): Long = RentalCatalog.pricePaise(modelId, plan)
}
