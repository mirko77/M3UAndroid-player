package com.m3u.smartphone.ui.business.channel

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeScaleTest {
    @Test
    fun `volume percent is relative to the maximum`() {
        assertEquals(
            expected = 30,
            actual = calculateVolumePercent(volume = 0.3f),
        )
    }

    @Test
    fun `volume percent of a silent stream is zero`() {
        assertEquals(
            expected = 0,
            actual = calculateVolumePercent(volume = 0f),
        )
    }

    @Test
    fun `volume percent of a full stream is one hundred`() {
        assertEquals(
            expected = 100,
            actual = calculateVolumePercent(volume = 1f),
        )
    }

    @Test
    fun `volume percent is rounded`() {
        assertEquals(
            expected = 30,
            actual = calculateVolumePercent(volume = 0.297f),
        )
    }

    @Test
    fun `volume percent above the maximum is clamped`() {
        assertEquals(
            expected = 100,
            actual = calculateVolumePercent(volume = 1.5f),
        )
    }

    @Test
    fun `volume percent below zero is clamped`() {
        assertEquals(
            expected = 0,
            actual = calculateVolumePercent(volume = -0.2f),
        )
    }
}
