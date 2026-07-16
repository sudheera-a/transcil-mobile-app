package com.example.transcilmobileapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.example.transcilmobileapp.databinding.ActivityChooseJourneyBinding

class ChooseJourneyActivity :
    BaseActivity<ActivityChooseJourneyBinding>(ActivityChooseJourneyBinding::inflate) {

    private val viewModel: ChooseJourneyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            binding.btnContinue.isEnabled = enabled
            binding.btnContinue.alpha = if (enabled) 1f else 0.5f
        }
        viewModel.navigateToPersonalAccount.observe(this) { go ->
            if (go == true) {
                startActivity(Intent(this, CreatePersonalAccountActivity::class.java))
            }
        }
        viewModel.showComingSoon.observe(this) { show ->
            if (show == true) {
                Toast.makeText(this, R.string.coming_soon_3pl, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderSelection(type: JourneyType?) {
        val rentSelected = type == JourneyType.RENT_EV
        val threePlSelected = type == JourneyType.THREE_PL

        binding.cardRentEv.setBackgroundResource(
            if (rentSelected) R.drawable.bg_card_selected else R.drawable.bg_card_default
        )
        binding.cardThreePl.setBackgroundResource(
            if (threePlSelected) R.drawable.bg_card_selected else R.drawable.bg_card_default
        )
        binding.ivRentEvCheck.visibility = if (rentSelected) View.VISIBLE else View.GONE
        binding.ivThreePlCheck.visibility = if (threePlSelected) View.VISIBLE else View.GONE
    }
}
