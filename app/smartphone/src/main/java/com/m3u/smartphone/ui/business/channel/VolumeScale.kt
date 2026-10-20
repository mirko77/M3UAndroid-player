package com.m3u.smartphone.ui.business.channel

import kotlin.math.roundToInt

/**
 * The volume level to display, as a percentage of the maximum.
 */
internal fun calculateVolumePercent(volume: Float): Int =
    (volume.coerceIn(0f, 1f) * 100).roundToInt()
