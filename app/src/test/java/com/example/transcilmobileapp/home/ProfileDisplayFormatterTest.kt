package com.example.transcilmobileapp.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileDisplayFormatterTest {

    @Test
    fun formatPhone_tenDigits_prefixed() {
        assertEquals("+91 9876543210", ProfileDisplayFormatter.formatPhone("9876543210"))
        assertEquals("+91 9876543210", ProfileDisplayFormatter.formatPhone("+91 98765 43210"))
    }

    @Test
    fun formatPhone_missing_showsEmpty() {
        assertEquals(ProfileDisplayFormatter.EMPTY, ProfileDisplayFormatter.formatPhone(""))
        assertEquals(ProfileDisplayFormatter.EMPTY, ProfileDisplayFormatter.formatPhone("123"))
    }

    @Test
    fun formatEmail_blank_showsEmpty() {
        assertEquals(ProfileDisplayFormatter.EMPTY, ProfileDisplayFormatter.formatEmail("  "))
        assertEquals("a@b.com", ProfileDisplayFormatter.formatEmail(" a@b.com "))
    }

    @Test
    fun formatLocation_joinsCityState() {
        assertEquals("Hyderabad, Telangana", ProfileDisplayFormatter.formatLocation("Hyderabad", "Telangana"))
        assertEquals("Nagole", ProfileDisplayFormatter.formatLocation("Nagole", ""))
        assertEquals(ProfileDisplayFormatter.EMPTY, ProfileDisplayFormatter.formatLocation("", ""))
    }
}
