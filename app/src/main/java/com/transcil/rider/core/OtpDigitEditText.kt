/**
 * Custom EditText for one OTP digit box. Fixes soft-keyboard backspace when the field is empty
 * so focus moves to the previous box (used by [UiFormHelpers.setupOtpAutoAdvance]).
 *
 * Kotlin / Android notes:
 * - Subclass of AppCompatEditText; `@JvmOverloads` generates Java-friendly constructors.
 * - `override fun onCreateInputConnection` = hook into IME/key events for delete forwarding.
 * - `var onDeleteWhenEmpty` = optional callback set by the parent OTP setup code.
 */
package com.transcil.rider.core

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

/**
 * Soft keyboards often skip KEYCODE_DEL on an empty field.
 * Forward that delete so OTP boxes can step focus left.
 */
class OtpDigitEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    var onDeleteWhenEmpty: (() -> Unit)? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(base, true) {
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_DEL &&
                    text.isNullOrEmpty()
                ) {
                    onDeleteWhenEmpty?.invoke()
                    return true
                }
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength == 1 && afterLength == 0 && text.isNullOrEmpty()) {
                    return sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) &&
                        sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }
    }
}
