package com.example.transcilmobileapp

import android.view.View
import android.widget.EditText
import android.widget.TextView

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
                if (index + 1 == activeStep) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )
        }
        root.findViewById<TextView>(R.id.tvStepLabel).text =
            root.context.getString(R.string.step_of_four, activeStep)
    }

    fun setupOtpAutoAdvance(boxes: List<EditText>) {
        boxes.forEachIndexed { i, box ->
            box.addTextChangedListener(SimpleTextWatcher {
                if (it?.length == 1 && i < boxes.lastIndex) {
                    boxes[i + 1].requestFocus()
                }
            })
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
