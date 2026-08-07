/**
 * Payment checkout demo — walks through review, UPI autopay mandate, pending, success, and failure panels.
 * Reached from RentalPlansActivity after plan selection; clears task stack to home on completion.
 */
package com.transcil.rider.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.core.KycStatus
import com.transcil.rider.databinding.ActivityPaymentBinding
import com.transcil.rider.home.HomeDashboardActivity
import com.transcil.rider.home.PlanType
import com.transcil.rider.home.VehicleModelId

class PaymentActivity : BaseActivity<ActivityPaymentBinding>(ActivityPaymentBinding::inflate) {

    private val viewModel: PaymentViewModel by viewModels()

    /** `override fun`: parse vehicle/plan from intent, bind ViewModel, toggle step panels. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intent extras: enum names serialized as strings, parsed safely with runCatching.
        val model = intent.getStringExtra(EXTRA_MODEL)
            ?.let { runCatching { VehicleModelId.valueOf(it) }.getOrNull() }
            ?: VehicleModelId.ELLOD_ELITE
        val plan = intent.getStringExtra(EXTRA_PLAN)
            ?.let { runCatching { PlanType.valueOf(it) }.getOrNull() }
            ?: PlanType.MONTHLY
        viewModel.bind(model, plan)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnPay.setOnClickListener { viewModel.payNow() }
        binding.btnAuthorise.setOnClickListener { viewModel.authoriseMandate() }
        binding.btnPayManual.setOnClickListener { viewModel.payManually() }
        binding.btnCheckStatus.setOnClickListener { viewModel.checkStatus(true) }
        binding.btnFailDemo.setOnClickListener { viewModel.checkStatus(false) }
        binding.btnBackHome.setOnClickListener { goHome() }
        binding.btnStartRiding.setOnClickListener { goHome() }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.btnMethods.setOnClickListener { viewModel.openMethods() }
        binding.btnCancelAutopay.setOnClickListener {
            FeedbackUi.toast(this, getString(R.string.payment_autopay_cancelled))
        }

        viewModel.breakdown.observe(this) { b ->
            binding.tvRent.text = money(b.rentPaise)
            binding.tvOnboarding.text = money(b.onboardingPaise)
            binding.tvDeposit.text = money(b.depositPaise)
            binding.tvTotal.text = money(b.totalPaise)
            binding.btnPay.text = getString(R.string.payment_pay_amount, b.totalPaise / 100)
            binding.tvAutopayMax.text = getString(
                R.string.payment_debit_up_to,
                money(viewModel.maxAutopayPaise()),
            )
        }
        viewModel.step.observe(this, ::showStep)
    }

    /** `private fun`: shows one panel at a time based on PaymentStep enum from ViewModel. */
    private fun showStep(step: PaymentStep) {
        binding.panelReview.isVisible = step == PaymentStep.REVIEW
        binding.panelAutopay.isVisible = step == PaymentStep.AUTOPAY
        binding.panelPending.isVisible = step == PaymentStep.PENDING
        binding.panelSuccess.isVisible = step == PaymentStep.SUCCESS
        binding.panelFailure.isVisible = step == PaymentStep.FAILURE
        binding.panelMethods.isVisible = step == PaymentStep.METHODS
        binding.tvTitle.text = when (step) {
            PaymentStep.REVIEW -> getString(R.string.payment_review_title)
            PaymentStep.AUTOPAY -> getString(R.string.payment_autopay_title)
            PaymentStep.PENDING -> getString(R.string.payment_pending_title)
            PaymentStep.SUCCESS -> getString(R.string.payment_success_title)
            PaymentStep.FAILURE -> getString(R.string.payment_failure_title)
            PaymentStep.METHODS -> getString(R.string.payment_methods_title)
        }
        binding.tvSubtitle.text = when (step) {
            PaymentStep.REVIEW -> getString(R.string.payment_review_subtitle)
            PaymentStep.AUTOPAY -> getString(R.string.payment_autopay_headline)
            PaymentStep.PENDING -> getString(R.string.payment_pending_body)
            PaymentStep.SUCCESS -> getString(R.string.payment_success_body)
            PaymentStep.FAILURE -> getString(R.string.payment_failure_body)
            PaymentStep.METHODS -> getString(R.string.payment_method_upi_demo)
        }
    }

    private fun money(paise: Long) = getString(R.string.plans_price_format, paise / 100)

    private fun goHome() {
        startActivity(
            HomeDashboardActivity.createIntent(this, KycStatus.APPROVED).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        finish()
    }

    /** `companion object`: factory for starting PaymentActivity with model + plan extras. */
    companion object {
        private const val EXTRA_MODEL = "model_id"
        private const val EXTRA_PLAN = "plan"
        fun createIntent(context: Context, modelId: String, plan: String) =
            Intent(context, PaymentActivity::class.java)
                .putExtra(EXTRA_MODEL, modelId)
                .putExtra(EXTRA_PLAN, plan)
    }
}
