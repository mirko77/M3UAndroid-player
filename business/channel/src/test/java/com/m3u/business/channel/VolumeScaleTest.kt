package com.m3u.business.channel

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeScaleTest {
    @Test
    fun `effective volume never exceeds the device level`() {
        assertEquals(
            expected = 0.3f,
            actual = calculateEffectiveVolume(playerVolume = 1f, deviceVolume = 0.3f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `effective volume scales with the player volume`() {
        assertEquals(
            expected = 0.15f,
            actual = calculateEffectiveVolume(playerVolume = 0.5f, deviceVolume = 0.3f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `effective volume is silent when the player is muted`() {
        assertEquals(
            expected = 0f,
            actual = calculateEffectiveVolume(playerVolume = 0f, deviceVolume = 0.3f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `effective volume is silent when the device is muted`() {
        assertEquals(
            expected = 0f,
            actual = calculateEffectiveVolume(playerVolume = 1f, deviceVolume = 0f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `a player volume above the maximum is clamped`() {
        assertEquals(
            expected = 0.3f,
            actual = calculateEffectiveVolume(playerVolume = 2f, deviceVolume = 0.3f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `a negative player volume is clamped`() {
        assertEquals(
            expected = 0f,
            actual = calculateEffectiveVolume(playerVolume = -1f, deviceVolume = 0.3f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun `a device volume above the maximum is clamped`() {
        assertEquals(
            expected = 1f,
            actual = calculateEffectiveVolume(playerVolume = 1f, deviceVolume = 1.5f),
            absoluteTolerance = 0.0001f,
        )
    }
}
