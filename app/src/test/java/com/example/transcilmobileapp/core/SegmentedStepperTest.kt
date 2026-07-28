package com.example.transcilmobileapp.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure helper for filled-count clamping used by UI wiring. */
object StepperMath {
    fun clampFilled(filled: Int, total: Int = 4): Int = filled.coerceIn(0, total)
}

class SegmentedStepperTest {
    @Test
    fun clampFilled_bounds() {
        assertEquals(0, StepperMath.clampFilled(-1))
        assertEquals(4, StepperMath.clampFilled(9))
        assertEquals(2, StepperMath.clampFilled(2))
    }
}
