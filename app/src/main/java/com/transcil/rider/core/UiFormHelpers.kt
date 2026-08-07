/**
 * Reusable form UI behaviors: focus highlights, step progress, OTP auto-advance, field errors.
 * Shared by auth OTP entry and KYC/onboarding forms so each screen doesn't reimplement listeners.
 *
 * Kotlin notes:
 * - `object` = collection of top-level helpers.
 * - `private class SimpleTextWatcher` = minimal TextWatcher wrapper (only afterTextChanged matters).
 */
package com.transcil.rider.core

import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView

import com.transcil.rider.R

object UiFormHelpers {

    fun bindFocusHighlight(editText: EditText) {
        val container = editText.parent as View
        editText.setOnFocusChangeListener { _, hasFocus ->
            container.setBackgroundResource(
                if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input_default
            )
        }
    }

    fun bindStepProgress(root: View, activeStep: Int) {
        val segments = listOf(
            root.findViewById<View>(R.id.stepSeg1),
            root.findViewById(R.id.stepSeg2),
            root.findViewById(R.id.stepSeg3),
            root.findViewById(R.id.stepSeg4)
        )
        segments.forEachIndexed { index, view ->
            view.setBackgroundResource(
                if (index < activeStep) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )
        }
        root.findViewById<TextView>(R.id.tvStepLabel).text =
            root.context.getString(R.string.step_of_four, activeStep)
    }

    // Wires 6 boxes: type one digit → focus next; backspace on empty → focus previous and clear it.
    fun setupOtpAutoAdvance(boxes: List<EditText>) {
        boxes.forEachIndexed { i, box ->
            box.addTextChangedListener(SimpleTextWatcher {
                if (it?.length == 1 && i < boxes.lastIndex) {
                    boxes[i + 1].requestFocus()
                }
            })
            val stepBack = {
                val prev = OtpInput.previousIndexOnEmptyDelete(i, box.text.isNullOrEmpty())
                if (prev != null) {
                    boxes[prev].requestFocus()
                    boxes[prev].text?.clear()
                }
            }
            if (box is OtpDigitEditText) {
                box.onDeleteWhenEmpty = stepBack
            } else {
                box.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        box.text.isNullOrEmpty()
                    ) {
                        stepBack()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    fun fillOtpBoxes(boxes: List<EditText>, otp: String) {
        val digits = otp.filter { it.isDigit() }.take(boxes.size)
        boxes.forEachIndexed { index, box ->
            box.setText(digits.getOrNull(index)?.toString().orEmpty())
        }
        val focusIndex = (digits.length - 1).coerceIn(0, boxes.lastIndex)
        boxes[focusIndex].requestFocus()
        boxes[focusIndex].setSelection(boxes[focusIndex].text?.length ?: 0)
    }

    fun setFieldError(errorView: TextView, container: View?, messageRes: Int?) {
        if (messageRes == null) {
            errorView.visibility = View.GONE
            errorView.text = ""
            container?.setBackgroundResource(R.drawable.bg_input_default)
        } else {
            errorView.visibility = View.VISIBLE
            errorView.setText(messageRes)
            container?.setBackgroundResource(R.drawable.bg_input_error)
        }
    }
}

private class SimpleTextWatcher(
    private val after: (android.text.Editable?) -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: android.text.Editable?) = after(s)
}
