package com.transcil.rider.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class AvailableListDedupeTest {

    private val pairedBuds = BluetoothDeviceItem("AA:AA:AA:AA:AA:AA", "My Buds", bonded = true)

    @Test
    fun pairedDeviceGhostUnderRotatedMacIsHidden() {
        val ghost = BluetoothDeviceItem("77:12:34:56:78:9A", "My Buds", bonded = false)
        val result = BluetoothRepository.dedupeForDisplay(listOf(ghost), listOf(pairedBuds))
        assertEquals(emptyList<BluetoothDeviceItem>(), result)
    }

    @Test
    fun dualModeDeviceSightedTwiceCollapsesToOneRow() {
        val classic = BluetoothDeviceItem("11:11:11:11:11:11", "BMS-01", bonded = false)
        val ble = BluetoothDeviceItem("55:22:33:44:55:66", "BMS-01", bonded = false)
        val result = BluetoothRepository.dedupeForDisplay(listOf(classic, ble), emptyList())
        assertEquals(listOf(classic), result)
    }

    @Test
    fun namelessRowsAreKeptButSortedLast() {
        val nameless = BluetoothDeviceItem("66:00:00:00:00:01", "66:00:00:00:00:01", bonded = false)
        val named = BluetoothDeviceItem("22:00:00:00:00:02", "BMS-01", bonded = false)
        val result = BluetoothRepository.dedupeForDisplay(listOf(nameless, named), emptyList())
        assertEquals(listOf(named, nameless), result)
    }
}
