package com.m3u.smartphone.ui.common

import android.app.ActivityOptions
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.m3u.business.playlist.configuration.navigateToPlaylistConfiguration
import com.m3u.business.playlist.navigateToPlaylist
import com.m3u.core.foundation.architecture.preferences.PreferencesKeys
import com.m3u.core.foundation.architecture.preferences.preferenceOf
import com.m3u.core.foundation.wrapper.eventOf
import com.m3u.smartphone.ui.business.channel.PlayerActivity
import com.m3u.smartphone.ui.business.configuration.playlistConfigurationScreen
import com.m3u.smartphone.ui.business.playlist.playlistScreen
import com.m3u.smartphone.ui.common.internal.Events
import com.m3u.smartphone.ui.material.components.Destination
import com.m3u.smartphone.ui.material.components.SettingDestination

@Composable
fun AppNavHost(
    navController: NavHostController,
    navigateToDestination: (Destination) -> Unit,
    navigateToChannel: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showBottomEdgeBlur: Boolean = true,
    onNestedDetailVisibilityChanged: (Boolean) -> Unit = {},
    startDestination: String = Destination.Foryou.name
) {
    val context = LocalContext.current

    val zappingMode by preferenceOf(PreferencesKeys.ZAPPING_MODE)

    key(startDestination) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = DESTINATION_FADE_MILLIS))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = DESTINATION_FADE_MILLIS))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = DESTINATION_FADE_MILLIS))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = DESTINATION_FADE_MILLIS))
            },
            modifier = modifier
        ) {
        rootGraph(
            contentPadding = contentPadding,
            navigateToPlaylist = { playlist ->
                navController.navigateToPlaylist(playlist.url)
            },
            navigateToChannel = navigateToChannel,
            navigateToSettingPlaylistManagement = {
                navigateToDestination(Destination.Setting)
                Events.settingDestination = eventOf(SettingDestination.Playlists)
            },
            navigateToPlaylistConfiguration = {
                navController.navigateToPlaylistConfiguration(it.url)
            },
            showBottomEdgeBlur = showBottomEdgeBlur,
            onNestedDetailVisibilityChanged = onNestedDetailVisibilityChanged,
        )
        playlistScreen(
            navigateToChannel = {
                if (zappingMode && PlayerActivity.isInPipMode) return@playlistScreen
                val options = ActivityOptions.makeCustomAnimation(
                    context,
                    0,
                    0
                )
                context.startActivity(
                    Intent(context, PlayerActivity::class.java).apply {
                        // addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    options.toBundle()
                )
            },
            onBack = {
                navController.popBackStack()
            },
            contentPadding = contentPadding
        )
        playlistConfigurationScreen(
            contentPadding = contentPadding,
            onBack = {
                navController.popBackStack()
            },
            onPlaylistRemoved = {
                navController.popBackStack()
            },
        )
        }
    }
}

private const val DESTINATION_FADE_MILLIS = 160
