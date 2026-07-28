package com.example.transcilmobileapp.splash

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.transcilmobileapp.auth.AuthSession
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false

    private val goNext = Runnable { navigateAfterSplash() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playEntrance()
        animateProgress()

        handler.postDelayed(goNext, 2200)

        binding.btnRetry.setOnClickListener {
            binding.errorState.isVisible = false
            binding.ivSplashLogo.isVisible = true
            binding.tvTagline.isVisible = true
            binding.progressTrack.isVisible = true
            navigated = false
            animateProgress()
            handler.postDelayed(goNext, 2200)
        }
    }

    private fun navigateAfterSplash() {
        if (navigated) return
        navigated = true
        lifecycleScope.launch {
            startActivity(AuthSession.resolveColdStart(this@MainActivity))
            finish()
        }
    }

    private fun playEntrance() {
        binding.ivSplashLogo.alpha = 0f
        binding.ivSplashLogo.translationY = 28f
        binding.ivSplashLogo.scaleX = 0.92f
        binding.ivSplashLogo.scaleY = 0.92f
        binding.tvTagline.alpha = 0f

        binding.ivSplashLogo.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.tvTagline.animate()
            .alpha(1f)
            .setStartDelay(350).setDuration(500)
            .start()
    }

    private fun animateProgress() {
        val track = binding.progressTrack
        val fill = binding.progressFill
        fill.layoutParams = fill.layoutParams.apply { width = 0 }
        fill.requestLayout()
        track.post {
            ObjectAnimator.ofInt(0, track.width).apply {
                duration = 1800
                startDelay = 400
                addUpdateListener {
                    fill.layoutParams = fill.layoutParams.apply { width = it.animatedValue as Int }
                    fill.requestLayout()
                }
                start()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
