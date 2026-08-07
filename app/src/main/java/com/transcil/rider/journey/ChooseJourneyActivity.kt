/**
 * Journey selection screen — rider picks Rent-EV or 3PL path after auth, before KYC personal details.
 * Part of post-login onboarding; persists choice via API then opens CreatePersonalAccountActivity.
 */
package com.transcil.rider.journey

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import com.transcil.rider.R
import com.transcil.rider.core.BaseActivity
import com.transcil.rider.core.JourneyType
import com.transcil.rider.core.NavExtras
import com.transcil.rider.core.SegmentedStepper
import com.transcil.rider.databinding.ActivityChooseJourneyBinding
import com.transcil.rider.core.FeedbackUi
import com.transcil.rider.kyc.CreatePersonalAccountActivity

class ChooseJourneyActivity :
    BaseActivity<ActivityChooseJourneyBinding>(ActivityChooseJourneyBinding::inflate) {

    private val viewModel: ChooseJourneyViewModel by viewModels()

    /** `override fun`: wire journey cards, observe selection state, navigate on continue. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.navyHeader.findViewById<TextView>(R.id.headerTitle).setText(R.string.choose_journey_title)
        binding.navyHeader.findViewById<TextView>(R.id.headerSubtitle).setText(R.string.choose_journey_subtitle)
        SegmentedStepper.apply(binding.navyHeader, filledCount = 3, navyInactive = true)

        binding.cardRentEv.setOnClickListener {
            viewModel.onJourneySelected(JourneyType.RENT_EV)
        }
        binding.cardThreePl.setOnClickListener {
            viewModel.onJourneySelected(JourneyType.THREE_PL)
        }
        binding.btnContinue.setOnClickListener {
            viewModel.onContinueClicked()
        }

        viewModel.selectedJourney.observe(this, ::renderSelection)
        viewModel.continueEnabled.observe(this) { enabled ->
            val loading = viewModel.isLoading.value == true
            binding.btnContinue.isEnabled = enabled == true && !loading
            binding.btnContinue.alpha = if (binding.btnContinue.isEnabled) 1f else 0.5f
        }
        viewModel.isLoading.observe(this) { loading ->
            val enabled = viewModel.continueEnabled.value == true
            binding.btnContinue.isEnabled = enabled && loading != true
            binding.btnContinue.alpha = if (binding.btnContinue.isEnabled) 1f else 0.5f
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) FeedbackUi.toast(this, message)
        }
        viewModel.navigateToPersonalAccount.observe(this) { go ->
            if (go == true) {
                val type = viewModel.selectedJourney.value ?: return@observe
                startActivity(
                    Intent(this, CreatePersonalAccountActivity::class.java)
                        .putExtra(NavExtras.JOURNEY_TYPE, type.name),
                )
            }
        }
    }

    /** `private fun`: updates card backgrounds and check icons to reflect selected journey. */
    private fun renderSelection(type: JourneyType?) {
        val rentSelected = type == JourneyType.RENT_EV
        val threePlSelected = type == JourneyType.THREE_PL

        binding.cardRentEv.setBackgroundResource(
            if (rentSelected) R.drawable.bg_card_selected else R.drawable.bg_card_default,
        )
        binding.cardThreePl.setBackgroundResource(
            if (threePlSelected) R.drawable.bg_card_selected else R.drawable.bg_card_default,
        )
        binding.ivRentEvCheck.setImageResource(
            if (rentSelected) R.drawable.ic_check_circle else R.drawable.ic_radio_off,
        )
        binding.ivThreePlCheck.setImageResource(
            if (threePlSelected) R.drawable.ic_check_circle else R.drawable.ic_radio_off,
        )
    }
}
