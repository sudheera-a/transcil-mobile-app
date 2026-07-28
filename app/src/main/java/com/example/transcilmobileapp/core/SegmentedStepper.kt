package com.example.transcilmobileapp.core

import android.view.View
import android.view.ViewGroup
import com.example.transcilmobileapp.R

/** Fills the first [filledCount] of 4 segments; navyInactive uses navy track for headers. */
object SegmentedStepper {
    fun apply(root: View, filledCount: Int, navyInactive: Boolean = false) {
        // Include tags overwrite the included root id, so resolve via a segment child.
        val seg1 = root.findViewById<View>(R.id.stepSegment1) ?: return
        val stepper = seg1.parent as? ViewGroup ?: return
        val inactive = if (navyInactive) {
            R.drawable.bg_step_segment_navy_inactive
        } else {
            R.drawable.bg_step_segment_inactive
        }
        val filled = filledCount.coerceIn(0, 4)
        intArrayOf(
            R.id.stepSegment1,
            R.id.stepSegment2,
            R.id.stepSegment3,
            R.id.stepSegment4,
        ).forEachIndexed { index, id ->
            stepper.findViewById<View>(id)?.setBackgroundResource(
                if (index < filled) R.drawable.bg_step_segment_active else inactive,
            )
        }
    }
}
