/**
 * Unit tests for [PaymentViewModel]: review → success payment flow state transitions.
 */
package com.transcil.rider.payment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.transcil.rider.home.PlanType
import com.transcil.rider.home.VehicleModelId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PaymentViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun flow_reviewToSuccess() {
        val vm = PaymentViewModel()
        vm.bind(VehicleModelId.ELLOD_ELITE, PlanType.MONTHLY)
        assertEquals(PaymentStep.REVIEW, vm.step.value)
        vm.payNow()
        assertEquals(PaymentStep.AUTOPAY, vm.step.value)
        vm.authoriseMandate()
        assertEquals(PaymentStep.PENDING, vm.step.value)
        vm.checkStatus(true)
        assertEquals(PaymentStep.SUCCESS, vm.step.value)
    }
}
