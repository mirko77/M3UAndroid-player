package com.m3u.smartphone.ui.business.channel

import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.m3u.i18n.R.string
import kotlin.math.absoluteValue
import kotlin.time.Duration

internal object ChannelMaskUtils {
    fun Modifier.detectVerticalGesture(
        threshold: Float = 0.15f,
        time: Float = 1f,
        onVerticalDrag: (pixel: Float) -> Unit,
        onDragStart: (() -> Unit)? = null,
        onDragEnd: (() -> Unit)? = null
    ): Modifier {
        var total = 0f
        return this then Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = { onDragStart?.invoke() },
                onDragEnd = { onDragEnd?.invoke() },
                onVerticalDrag = { change, dragAmount ->
                    total += dragAmount.absoluteValue / size.height
                    if (total < threshold) return@detectVerticalDragGestures
                    onVerticalDrag(dragAmount * time)
                    change.consume()
                }
            )
        }
    }

    @Composable
    fun playbackExceptionDisplayText(e: PlaybackException?): String {
        if (e == null) return ""
        val isOnline by IsOnline
        if (!isOnline) return stringResource(string.feat_channel_playback_state_offline)
        return stringResource(
            when (e.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                PlaybackException.ERROR_CODE_TIMEOUT,
                -> string.feat_channel_playback_error_network

                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                -> string.feat_channel_playback_error_unavailable

                in 3000..6999 -> string.feat_channel_playback_error_unsupported
                in 7000..7999 -> string.feat_channel_playback_error_protected
                else -> string.feat_channel_playback_error_unknown
            }
        )
    }

    @Composable
    fun playStateDisplayText(@Player.State state: Int): String = when (state) {
        Player.STATE_IDLE -> string.feat_channel_playback_state_idle
        Player.STATE_BUFFERING -> string.feat_channel_playback_state_buffering
        Player.STATE_READY -> null
        Player.STATE_ENDED -> string.feat_channel_playback_state_ended
        else -> null
    }
        ?.let { stringResource(it) }
        .orEmpty()

    @Composable
    fun timeunitDisplayTest(duration: Duration): String =
        duration.toComponents { hours, minutes, seconds, _ ->
            buildString {
                if (hours > 0) append("$hours:")
                append("${minutes.fixClockUnit}:")
                append(seconds.fixClockUnit)
            }
        }

    val IsAutoRotatingEnabled: State<Boolean>
        @Composable get() {
            val context = LocalContext.current
            val contentResolver = context.contentResolver
            val initialValue = try {
                Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION
                ) == 1
            } catch (_: Settings.SettingNotFoundException) {
                false
            }
            return produceState(initialValue) {
                val uri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)
                val handler = Handler(Looper.getMainLooper())
                val observer = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        super.onChange(selfChange)
                        value = try {
                            Settings.System.getInt(
                                contentResolver,
                                Settings.System.ACCELEROMETER_ROTATION
                            ) == 1
                        } catch (_: Settings.SettingNotFoundException) {
                            false
                        }
                    }
                }
                contentResolver.registerContentObserver(
                    uri,
                    true,
                    observer
                )
                awaitDispose {
                    contentResolver.unregisterContentObserver(observer)
                }
            }
        }

    val IsOnline: State<Boolean>
        @Composable get() {
            val context = LocalContext.current
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            fun check(): Boolean {
                val network = connectivity?.activeNetwork ?: return false
                val capabilities = connectivity.getNetworkCapabilities(network)
                    ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            return produceState(check()) {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        value = check()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        value = check()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        value = check()
                    }
                }
                runCatching {
                    connectivity?.registerDefaultNetworkCallback(callback)
                }
                awaitDispose {
                    runCatching {
                        connectivity?.unregisterNetworkCallback(callback)
                    }
                }
            }
        }

    private val Int.fixClockUnit: String
        get() = if (this < 10) "0$this"
        else this.toString()
}
