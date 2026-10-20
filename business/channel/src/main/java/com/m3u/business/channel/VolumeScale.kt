package com.m3u.business.channel

/**
 * The effective loudness of the stream, as a fraction of the device media volume maximum.
 *
 * The player volume is applied on top of the device media volume, so the result never
 * exceeds the device level: the device volume buttons remain the only way to go louder.
 */
internal fun calculateEffectiveVolume(playerVolume: Float, deviceVolume: Float): Float =
    playerVolume.coerceIn(0f, 1f) * deviceVolume.coerceIn(0f, 1f)
