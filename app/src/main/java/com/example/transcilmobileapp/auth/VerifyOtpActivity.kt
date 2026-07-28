package com.example.transcilmobileapp.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.FeedbackUi
import com.example.transcilmobileapp.core.NavExtras
import com.example.transcilmobileapp.core.OtpInput
import com.example.transcilmobileapp.core.SegmentedStepper
import com.example.transcilmobileapp.core.UiFormHelpers
import com.example.transcilmobileapp.databinding.ActivityVerifyOtpBinding
import com.example.transcilmobileapp.journey.ChooseJourneyActivity
import com.example.transcilmobileapp.kyc.KycProgressRepository
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class VerifyOtpActivity : BaseActivity<ActivityVerifyOtpBinding>(ActivityVerifyOtpBinding::inflate) {

    private val viewModel: VerifyOtpViewModel by viewModels()
    private var otpSession: String = ""
    private lateinit var otpBoxes: List<EditText>
    private var smsReceiverRegistered = false

    private val smsConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
        val code = OtpInput.extractSixDigitCode(message) ?: return@registerForActivityResult
        UiFormHelpers.fillOtpBoxes(otpBoxes, code)
    }

    private val smsConsentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return
            val extras = intent.extras ?: return
            val status = statusFrom(extras) ?: return
            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val consentIntent = consentIntentFrom(extras) ?: return
                    smsConsentLauncher.launch(consentIntent)
                }
                CommonStatusCodes.TIMEOUT -> {
                    binding.tvAutoDetect.setText(R.string.otp_auto_detect_timeout)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mobileNumber = intent.getStringExtra(NavExtras.MOBILE_NUMBER).orEmpty()
        otpSession = intent.getStringExtra(NavExtras.OTP_SESSION).orEmpty()
        if (otpSession.isBlank()) {
            FeedbackUi.toast(this, getString(R.string.otp_session_missing))
            finish()
            return
        }

        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerTitle).setText(R.string.verify_otp_title)
        binding.navyHeaderInclude.findViewById<TextView>(R.id.headerSubtitle).setText(R.string.enter_otp_title)
        SegmentedStepper.apply(binding.navyHeaderInclude, filledCount = 2, navyInactive = true)
        binding.tvOtpSentTo.text = "${getString(R.string.otp_sent_prefix)} +91 $mobileNumber"

        otpBoxes = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6,
        )
        UiFormHelpers.setupOtpAutoAdvance(otpBoxes)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener { finish() }
        binding.btnVerify.setOnClickListener {
            viewModel.onVerifyClicked(
                otpSession,
                mobileNumber,
                otpBoxes.joinToString("") { it.text.toString() },
            )
        }
        binding.tvResendOtp.setOnClickListener {
            viewModel.onResendClicked(mobileNumber)
            binding.tvAutoDetect.setText(R.string.otp_auto_detect)
            startSmsUserConsent()
        }

        viewModel.otpSession.observe(this) { session ->
            if (!session.isNullOrBlank()) otpSession = session
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.btnVerify.isEnabled = loading != true
            binding.tvResendOtp.isEnabled = loading != true
        }
        viewModel.navigateToChooseJourney.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                if (mobileNumber.isNotBlank()) {
                    KycProgressRepository.saveSessionMobile(mobileNumber)
                }
                startActivity(Intent(this, ChooseJourneyActivity::class.java))
                finish()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) FeedbackUi.toast(this, message)
        }

        registerSmsConsentReceiver()
        startSmsUserConsent()
    }

    override fun onDestroy() {
        if (smsReceiverRegistered) {
            unregisterReceiver(smsConsentReceiver)
            smsReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun startSmsUserConsent() {
        SmsRetriever.getClient(this).startSmsUserConsent(/* senderPhoneNumber= */ null)
    }

    private fun registerSmsConsentReceiver() {
        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            this,
            smsConsentReceiver,
            filter,
            SmsRetriever.SEND_PERMISSION,
            /* scheduler= */ null,
            ContextCompat.RECEIVER_EXPORTED,
        )
        smsReceiverRegistered = true
    }

    private fun consentIntentFrom(extras: Bundle): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
        }

    private fun statusFrom(extras: Bundle): Status? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(SmsRetriever.EXTRA_STATUS, Status::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(SmsRetriever.EXTRA_STATUS)
        }
}
