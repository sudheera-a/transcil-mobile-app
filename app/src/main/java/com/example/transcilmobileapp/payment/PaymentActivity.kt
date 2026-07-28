package com.example.transcilmobileapp.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.FeedbackUi
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.ActivityPaymentBinding
import com.example.transcilmobileapp.home.HomeDashboardActivity
import com.example.transcilmobileapp.home.PlanType
import com.example.transcilmobileapp.home.VehicleModelId

class PaymentActivity : BaseActivity<ActivityPaymentBinding>(ActivityPaymentBinding::inflate) {

    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    companion object {
        private const val EXTRA_MODEL = "model_id"
        private const val EXTRA_PLAN = "plan"
        fun createIntent(context: Context, modelId: String, plan: String) =
            Intent(context, PaymentActivity::class.java)
                .putExtra(EXTRA_MODEL, modelId)
                .putExtra(EXTRA_PLAN, plan)
    }
}
