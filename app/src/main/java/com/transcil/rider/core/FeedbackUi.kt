/**
 * Small UI feedback helpers used across screens: dialogs, toasts, and snackbars.
 * Keeps severity styling and placement consistent so Activities don't duplicate Material boilerplate.
 *
 * Kotlin notes:
 * - `enum class` = fixed set of named constants ([FeedbackSeverity]).
 * - `object` = singleton namespace for stateless functions (no instance needed).
 */
package com.transcil.rider.core

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import android.view.View

enum class FeedbackSeverity { SUCCESS, FAILURE, CONFIRM, INFO }

object FeedbackUi {
    fun showDialog(
        context: Context,
        severity: FeedbackSeverity,
        title: String,
        message: String,
        primaryLabel: String = "OK",
        onPrimary: (() -> Unit)? = null,
        secondaryLabel: String? = null,
        onSecondary: (() -> Unit)? = null,
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(primaryLabel) { d, _ ->
                onPrimary?.invoke()
                d.dismiss()
            }
        if (secondaryLabel != null) {
            builder.setNegativeButton(secondaryLabel) { d, _ ->
                onSecondary?.invoke()
                d.dismiss()
            }
        }
        // ponytail: Material dialog chrome only; custom severity icons upgrade later
        return builder.show()
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.BOTTOM, 0, 120)
            show()
        }
    }

    fun snack(anchor: View, message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        val bar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
        if (actionLabel != null && action != null) {
            bar.setAction(actionLabel) { action() }
        }
        bar.show()
    }
}
