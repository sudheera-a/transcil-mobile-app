/**
 * Identifies which remote content page to load in [ApiContentFragment] / [ApiContentViewModel].
 */
package com.transcil.rider.home

import com.transcil.rider.R

enum class ContentPage {
    HELP,
    TERMS_PRIVACY,
    PRIVACY,
    TERMS;

    fun titleRes(): Int = when (this) {
        HELP -> R.string.settings_help_center
        TERMS_PRIVACY -> R.string.settings_terms_privacy
        PRIVACY -> R.string.profile_menu_privacy
        TERMS -> R.string.settings_terms_privacy
    }
}
