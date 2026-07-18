package com.example.transcilmobileapp.home

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.transcilmobileapp.R
import com.example.transcilmobileapp.core.BaseActivity
import com.example.transcilmobileapp.core.KycNavigator
import com.example.transcilmobileapp.core.KycStatus
import com.example.transcilmobileapp.databinding.ActivityHomeDashboardBinding

/**
 * Post-KYC app shell: single Activity + NavHost + shared bottom navigation.
 * Settings is a Profile sub-destination (bottom nav hidden).
 */
class HomeDashboardActivity :
    BaseActivity<ActivityHomeDashboardBinding>(ActivityHomeDashboardBinding::inflate) {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHost = supportFragmentManager.findFragmentById(R.id.homeNavHost) as? NavHostFragment
            ?: error("homeNavHost must host a NavHostFragment")
        navController = navHost.navController

        bindBottomNav()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isSettings = destination.id == R.id.settingsFragment
            binding.bottomNav.visibility = if (isSettings) View.GONE else View.VISIBLE
            renderBottomNav(destinationIdToTab(destination.id))
        }

        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_START_TAB)
                ?.let { runCatching { HomeNavTab.valueOf(it) }.getOrNull() }
                ?.takeIf { it != HomeNavTab.HOME }
                ?.let { navigateToTab(it) }
        }
    }

    fun navigateToTab(tab: HomeNavTab) {
        val destinationId = tabToDestinationId(tab)
        if (navController.currentDestination?.id == destinationId) return

        // Leaving Settings (or any stacked dest) via tab should land cleanly on the tab root.
        val options = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(R.id.homeDashboardFragment) {
                saveState = true
            }
        }
        runCatching { navController.navigate(destinationId, null, options) }
    }

    private fun bindBottomNav() {
        binding.navHome.setOnClickListener { navigateToTab(HomeNavTab.HOME) }
        binding.navMap.setOnClickListener { navigateToTab(HomeNavTab.MAP) }
        binding.navBattery.setOnClickListener { navigateToTab(HomeNavTab.BATTERY) }
        binding.navWallet.setOnClickListener { navigateToTab(HomeNavTab.WALLET) }
        binding.navProfile.setOnClickListener { navigateToTab(HomeNavTab.PROFILE) }
    }

    private fun renderBottomNav(tab: HomeNavTab) {
        setNavItemSelected(
            binding.navHome,
            binding.ivNavHome,
            binding.tvNavHome,
            tab == HomeNavTab.HOME
        )
        setNavItemSelected(
            binding.navMap,
            binding.ivNavMap,
            binding.tvNavMap,
            tab == HomeNavTab.MAP
        )
        setNavItemSelected(
            binding.navBattery,
            binding.ivNavBattery,
            binding.tvNavBattery,
            tab == HomeNavTab.BATTERY
        )
        setNavItemSelected(
            binding.navWallet,
            binding.ivNavWallet,
            binding.tvNavWallet,
            tab == HomeNavTab.WALLET
        )
        setNavItemSelected(
            binding.navProfile,
            binding.ivNavProfile,
            binding.tvNavProfile,
            tab == HomeNavTab.PROFILE
        )
    }

    private fun setNavItemSelected(
        container: View,
        icon: ImageView,
        label: TextView,
        selected: Boolean
    ) {
        container.isSelected = selected
        icon.isSelected = selected
        label.isSelected = selected
        label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun tabToDestinationId(tab: HomeNavTab): Int = when (tab) {
        HomeNavTab.HOME -> R.id.homeDashboardFragment
        HomeNavTab.MAP -> R.id.nearbyHubsFragment
        HomeNavTab.BATTERY -> R.id.batterySwapFragment
        HomeNavTab.WALLET -> R.id.walletFragment
        HomeNavTab.PROFILE -> R.id.profileFragment
    }

    private fun destinationIdToTab(destinationId: Int): HomeNavTab = when (destinationId) {
        R.id.nearbyHubsFragment -> HomeNavTab.MAP
        R.id.batterySwapFragment -> HomeNavTab.BATTERY
        R.id.walletFragment -> HomeNavTab.WALLET
        R.id.profileFragment, R.id.settingsFragment -> HomeNavTab.PROFILE
        else -> HomeNavTab.HOME
    }

    companion object {
        const val EXTRA_START_TAB = "extra_start_tab"

        fun createIntent(
            context: Context,
            status: KycStatus,
            startTab: HomeNavTab = HomeNavTab.HOME
        ): Intent {
            return Intent(context, HomeDashboardActivity::class.java)
                .putExtra(KycNavigator.EXTRA_KYC_STATUS, status.name)
                .putExtra(EXTRA_START_TAB, startTab.name)
        }
    }
}
