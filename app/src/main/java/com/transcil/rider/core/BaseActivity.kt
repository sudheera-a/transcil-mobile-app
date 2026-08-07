/**
 * Shared base for all screens (Activities). Inflates a ViewBinding layout, shows it,
 * and pads content so it isn't hidden under status bar / notch / nav bar.
 *
 * Kotlin / Android notes:
 * - `abstract class` = cannot create directly; subclasses (WelcomeActivity, etc.) fill in details.
 * - Generics `<VB : ViewDataBinding>` = each subclass picks its binding type (e.g. ActivityMainBinding).
 * - `override fun` = replaces/extends a method from a parent class (here AppCompatActivity).
 * - `protected` = visible to this class and subclasses.
 * - `lateinit var` = "I'll assign this before use" (binding is set in onCreate).
 * - `private fun` = helper only this class can call.
 * - `private val` = read-only property owned by this class (immutable reference).
 * - ViewBinding / DataBinding: generated class that lets you write `binding.btnRetry`
 *   instead of `findViewById(R.id.btnRetry)`. Safer and clearer.
 */
package com.transcil.rider.core

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding

abstract class BaseActivity<VB : ViewDataBinding>(
    // Constructor param: function that turns a LayoutInflater into this screen's binding.
    private val inflate: (LayoutInflater) -> VB
) : AppCompatActivity() {

    // `lateinit var` because Activity must exist before we inflate; set in onCreate.
    // Subclasses use `binding.someView` to touch UI.
    protected lateinit var binding: VB

    // Lifecycle callback: Android calls this when the screen is created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // always call parent first
        WindowCompat.setDecorFitsSystemWindows(window, false) // we handle insets ourselves
        binding = inflate(layoutInflater)
        setContentView(binding.root) // show the XML layout on screen
        applySafeDrawingInsets()
    }

    /**
     * Pads the activity root for status/nav bars and display cutouts (punch-hole,
     * notch, etc.) so content stays visible across devices and screen sizes.
     * Layout padding from XML is preserved and combined with inset values.
     */
    private fun applySafeDrawingInsets() {
        val root = binding.root
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                baseLeft + safe.left,
                baseTop + safe.top,
                baseRight + safe.right,
                baseBottom + safe.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
