package com.m3u.data.service.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceVolumeTest {
    @Test
    fun `device volume fraction is relative to the maximum`() {
        assertEquals(
            expected = 0.3f,
            actual = calculateDeviceVolumeFraction(currentVolume = 3, maxVolume = 10),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `device volume fraction of a silent device is zero`() {
        assertEquals(
            expected = 0f,
            actual = calculateDeviceVolumeFraction(currentVolume = 0, maxVolume = 10),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `device volume fraction of a full device is one`() {
        assertEquals(
            expected = 1f,
            actual = calculateDeviceVolumeFraction(currentVolume = 10, maxVolume = 10),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `a volume above the maximum is clamped`() {
        assertEquals(
            expected = 1f,
            actual = calculateDeviceVolumeFraction(currentVolume = 15, maxVolume = 10),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `a negative volume is clamped`() {
        assertEquals(
            expected = 0f,
            actual = calculateDeviceVolumeFraction(currentVolume = -3, maxVolume = 10),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `an unknown maximum does not divide by zero`() {
        assertEquals(
            expected = 1f,
            actual = calculateDeviceVolumeFraction(currentVolume = 3, maxVolume = 0),
            absoluteTolerance = 0.0001f,
        )
    }
}
