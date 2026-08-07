package com.transcil.rider.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class PayloadFormatTest {

    @Test
    fun printableTextStaysText() {
        assertEquals("OK", BluetoothConnectionService.formatPayload("OK\r\n".toByteArray()))
    }

    @Test
    fun binaryBmsFrameRendersAsHex() {
        val frame = byteArrayOf(0x7E, 0x20, 0x10, 0x08, 0x00, 0x7F)
        assertEquals("7E 20 10 08 00 7F", BluetoothConnectionService.formatPayload(frame))
    }
}
