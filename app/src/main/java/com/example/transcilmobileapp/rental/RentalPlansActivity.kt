package com.example.transcilmobileapp.rental

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.databinding.ActivityRentalPlansBinding
import com.example.transcilmobileapp.home.PlanType
import com.example.transcilmobileapp.home.RentalCatalog
import com.example.transcilmobileapp.home.VehicleModelId
import com.example.transcilmobileapp.payment.PaymentActivity

class RentalPlansActivity : BaseActivity<ActivityRentalPlansBinding>(ActivityRentalPlansBinding::inflate) {

    private val viewModel: RentalPlansViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modelId = intent.getStringExtra(EXTRA_MODEL)
            ?.let { runCatching { VehicleModelId.valueOf(it) }.getOrNull() }
            ?: VehicleModelId.ELLOD_ELITE
        viewModel.bind(modelId)

        binding.ivBack.setOnClickListener { finish() }
        binding.cardDaily.setOnClickListener { viewModel.select(PlanType.DAILY) }
        binding.cardWeekly.setOnClickListener { viewModel.select(PlanType.WEEKLY) }
        binding.cardMonthly.setOnClickListener { viewModel.select(PlanType.MONTHLY) }
        binding.btnReviewPay.setOnClickListener {
            val plan = viewModel.selected.value ?: return@setOnClickListener
            startActivity(PaymentActivity.createIntent(this, modelId.name, plan.name))
        }

        viewModel.selected.observe(this) { plan ->
            styleCard(binding.cardDaily, binding.dailyExpanded, plan == PlanType.DAILY)
            styleCard(binding.cardWeekly, binding.weeklyExpanded, plan == PlanType.WEEKLY)
            styleCard(binding.cardMonthly, binding.monthlyExpanded, plan == PlanType.MONTHLY)
            val rupees = RentalCatalog.pricePaise(modelId, plan) / 100
            binding.tvStickyTotal.text = getString(R.string.plans_pay_today, rupees)
            binding.tvStickyPlan.text = when (plan) {
                PlanType.DAILY -> getString(R.string.plan_daily)
                PlanType.WEEKLY -> getString(R.string.plan_weekly)
                PlanType.MONTHLY -> getString(R.string.plan_monthly)
            }
        }

        bindPrices(modelId)
    }

    private fun bindPrices(modelId: VehicleModelId) {
        fun set(priceView: android.widget.TextView, perDayView: android.widget.TextView, plan: PlanType) {
            val rupees = RentalCatalog.pricePaise(modelId, plan) / 100
            priceView.text = getString(R.string.plans_price_format, rupees)
            perDayView.text = getString(R.string.plans_per_day, RentalCatalog.perDayRupees(modelId, plan))
        }
        set(binding.tvDailyPrice, binding.tvDailyPerDay, PlanType.DAILY)
        set(binding.tvWeeklyPrice, binding.tvWeeklyPerDay, PlanType.WEEKLY)
        set(binding.tvMonthlyPrice, binding.tvMonthlyPerDay, PlanType.MONTHLY)
    }

    private fun styleCard(card: View, expanded: View, selected: Boolean) {
        card.setBackgroundResource(
            if (selected) R.drawable.bg_card_selected else R.drawable.bg_card_default,
        )
        expanded.isVisible = selected
    }

    companion object {
        private const val EXTRA_MODEL = "model_id"
        fun createIntent(context: Context, modelId: String) =
            Intent(context, RentalPlansActivity::class.java).putExtra(EXTRA_MODEL, modelId)
    }
}
