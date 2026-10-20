package com.m3u.data.service.internal

/**
 * The device media volume level, expressed as a fraction of its maximum (0f..1f).
 *
 * A non positive maximum means the platform cannot report the level, in which case
 * the volume is considered unconstrained.
 */
internal fun calculateDeviceVolumeFraction(currentVolume: Int, maxVolume: Int): Float =
    if (maxVolume <= 0) {
        1f
    } else {
        currentVolume.coerceIn(0, maxVolume).toFloat() / maxVolume
    }
