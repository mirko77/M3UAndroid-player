package com.m3u.smartphone.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.material.components.Destination

internal const val FLOATING_NAVIGATION_TEST_TAG = "floating-app-navigation"
internal const val FLOATING_REMOTE_CONTROL_TEST_TAG = "floating-remote-control-action"

@Composable
internal fun FloatingAppNavigationDock(
    selectedDestination: Destination?,
    backdrop: Backdrop,
    useBackdropEffects: Boolean,
    enabled: Boolean,
    remoteControlVisible: Boolean,
    onDestinationSelected: (Destination) -> Unit,
    onOpenRemoteControl: () -> Unit,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showRemoteControl = enabled && remoteControlVisible
        val targetNavigationWidth = calculateFloatingNavigationDockNavigationWidth(
            containerWidth = maxWidth,
            itemCount = Destination.entries.size,
            trailingActionVisible = showRemoteControl,
            trailingActionSlotWidth = FLOATING_REMOTE_CONTROL_SLOT_WIDTH,
        )
        val navigationWidth by animateDpAsState(
            targetValue = targetNavigationWidth,
            label = "floating-navigation-dock-width",
        )
        val remoteControlSlotWidth by animateDpAsState(
            targetValue = if (showRemoteControl) {
                FLOATING_REMOTE_CONTROL_SLOT_WIDTH
            } else {
                0.dp
            },
            label = "floating-remote-control-slot-width",
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(navigationWidth + remoteControlSlotWidth)
                .height(FLOATING_NAVIGATION_HEIGHT),
        ) {
            FloatingAppNavigationBar(
                selectedDestination = selectedDestination,
                enabled = enabled,
                onDestinationSelected = onDestinationSelected,
                onHeightChanged = onHeightChanged,
                widthResolvedByParent = true,
                modifier = Modifier.width(navigationWidth),
            )
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .width(remoteControlSlotWidth)
                    .fillMaxHeight(),
            ) {
                FloatingRemoteControlVisibility(
                    visible = showRemoteControl,
                    backdrop = backdrop,
                    useBackdropEffects = useBackdropEffects,
                    enabled = enabled,
                    onClick = onOpenRemoteControl,
                )
            }
        }
    }
}

@Composable
private fun FloatingRemoteControlVisibility(
    visible: Boolean,
    backdrop: Backdrop,
    useBackdropEffects: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.78f),
        exit = fadeOut() + scaleOut(targetScale = 0.78f),
    ) {
        FloatingRemoteControlAction(
            backdrop = backdrop,
            useBackdropEffects = useBackdropEffects,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun FloatingRemoteControlAction(
    backdrop: Backdrop,
    useBackdropEffects: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 620f,
        ),
        label = "floating-remote-control-press",
    )
    val glassTokens = resolveFloatingNavigationGlassTokens(
        colorScheme = MaterialTheme.colorScheme,
        useBackdropEffects = useBackdropEffects,
    )
    val actionSizePx = with(density) { FLOATING_REMOTE_CONTROL_SIZE.toPx() }
    val blurRadiusPx = with(density) { (FLOATING_NAVIGATION_INNER_PADDING * 2).toPx() }
    val surfaceModifier = if (useBackdropEffects) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(blurRadiusPx)
                lens(
                    refractionHeight = actionSizePx * SHELL_REFRACTION_HEIGHT_SHARE,
                    refractionAmount = actionSizePx * REMOTE_ACTION_REFRACTION_AMOUNT_SHARE,
                    depthEffect = true,
                )
            },
            highlight = {
                Highlight.Default.copy(alpha = glassTokens.highlightAlpha)
            },
            shadow = {
                Shadow.Default.copy(color = glassTokens.shadowColor)
            },
            layerBlock = {
                scaleX = pressScale
                scaleY = pressScale
            },
            onDrawSurface = {
                drawRect(glassTokens.surfaceColor)
            },
        )
    } else {
        Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
            )
            .background(glassTokens.surfaceColor, CircleShape)
    }
    val label = stringResource(string.feat_setting_remote_control)
    val interactionModifier = if (enabled) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            }
    } else {
        Modifier.clearAndSetSemantics {}
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(FLOATING_REMOTE_CONTROL_SIZE)
            .then(surfaceModifier)
            .border(
                width = 1.dp,
                color = glassTokens.outlineColor,
                shape = CircleShape,
            )
            .clip(CircleShape)
            .testTag(FLOATING_REMOTE_CONTROL_TEST_TAG)
            .then(interactionModifier),
    ) {
        Icon(
            imageVector = Icons.Rounded.SettingsRemote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(NAVIGATION_ICON_SIZE),
        )
    }
}

@Composable
internal fun FloatingAppNavigationBar(
    selectedDestination: Destination?,
    enabled: Boolean,
    onDestinationSelected: (Destination) -> Unit,
    onHeightChanged: (Dp) -> Unit,
    widthResolvedByParent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val destinations = Destination.entries
    val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val interactionSources = remember(destinations.size) {
        List(destinations.size) { MutableInteractionSource() }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val navigationWidth = if (widthResolvedByParent) {
            maxWidth
        } else {
            calculateFloatingNavigationWidth(
                containerWidth = maxWidth,
                itemCount = destinations.size,
            )
        }
        val itemWidth = (navigationWidth - FLOATING_NAVIGATION_INNER_PADDING * 2) /
            destinations.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = 900f,
                visibilityThreshold = 0.5.dp,
            ),
            label = "floating-navigation-indicator",
        )

        Box(
            modifier = Modifier
                .width(navigationWidth)
                .height(FLOATING_NAVIGATION_HEIGHT)
                .clip(CircleShape)
                .background(colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .onSizeChanged { size ->
                    onHeightChanged(with(density) { size.height.toDp() })
                }
                .testTag(FLOATING_NAVIGATION_TEST_TAG),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FLOATING_NAVIGATION_INNER_PADDING),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colorScheme.secondaryContainer),
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (enabled) {
                                Modifier.selectableGroup()
                            } else {
                                Modifier.clearAndSetSemantics {}
                            },
                        ),
                ) {
                    destinations.forEachIndexed { index, destination ->
                        val label = stringResource(destination.iconTextId)
                        val isSelected = index == selectedIndex
                        val selectDestination = {
                            if (enabled && index != selectedIndex) {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.SegmentTick,
                                )
                                onDestinationSelected(destination)
                            }
                        }
                        val interactionModifier = if (enabled) {
                            Modifier
                                .clearAndSetSemantics {
                                    contentDescription = label
                                    role = Role.Tab
                                    selected = isSelected
                                    onClick(label = label) {
                                        selectDestination()
                                        true
                                    }
                                }
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = interactionSources[index],
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = selectDestination,
                                )
                        } else {
                            Modifier.clearAndSetSemantics {}
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .then(interactionModifier),
                        ) {
                            Icon(
                                imageVector = if (isSelected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = null,
                                tint = if (isSelected) {
                                    colorScheme.onSecondaryContainer
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(NAVIGATION_ICON_SIZE),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal data class FloatingNavigationGlassTokens(
    val surfaceColor: Color,
    val outlineColor: Color,
    val highlightAlpha: Float,
    val shadowColor: Color,
    val idleIndicatorColor: Color,
)

internal fun resolveFloatingNavigationGlassTokens(
    colorScheme: ColorScheme,
    useBackdropEffects: Boolean,
): FloatingNavigationGlassTokens {
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    val surface = if (isDarkTheme) {
        colorScheme.surfaceContainer
    } else {
        colorScheme.surfaceContainerLowest
    }
    return FloatingNavigationGlassTokens(
        surfaceColor = surface.copy(
            alpha = if (useBackdropEffects) {
                GLASS_SURFACE_ALPHA
            } else {
                GLASS_FALLBACK_SURFACE_ALPHA
            },
        ),
        outlineColor = if (isDarkTheme) {
            colorScheme.outlineVariant.copy(alpha = DARK_GLASS_OUTLINE_ALPHA)
        } else {
            colorScheme.outline.copy(alpha = LIGHT_GLASS_OUTLINE_ALPHA)
        },
        highlightAlpha = if (isDarkTheme) {
            DARK_GLASS_HIGHLIGHT_ALPHA
        } else {
            LIGHT_GLASS_HIGHLIGHT_ALPHA
        },
        shadowColor = Color.Black.copy(
            alpha = if (isDarkTheme) {
                DARK_GLASS_SHADOW_ALPHA
            } else {
                LIGHT_GLASS_SHADOW_ALPHA
            },
        ),
        idleIndicatorColor = if (isDarkTheme) {
            Color.White.copy(alpha = IDLE_INDICATOR_ALPHA)
        } else {
            Color.Black.copy(alpha = IDLE_INDICATOR_ALPHA)
        },
    )
}

private val FLOATING_NAVIGATION_HEIGHT = 64.dp
private val FLOATING_NAVIGATION_INNER_PADDING = 4.dp
private val FLOATING_REMOTE_CONTROL_SIZE = 64.dp
private val FLOATING_REMOTE_CONTROL_SLOT_WIDTH = 76.dp
private val NAVIGATION_ICON_SIZE = 24.dp
private const val SHELL_REFRACTION_HEIGHT_SHARE = 0.34f
private const val REMOTE_ACTION_REFRACTION_AMOUNT_SHARE = 0.18f
private const val GLASS_SURFACE_ALPHA = 0.40f
private const val GLASS_FALLBACK_SURFACE_ALPHA = 0.94f
private const val LIGHT_GLASS_OUTLINE_ALPHA = 0.56f
private const val DARK_GLASS_OUTLINE_ALPHA = 0.72f
private const val LIGHT_GLASS_HIGHLIGHT_ALPHA = 0.75f
private const val DARK_GLASS_HIGHLIGHT_ALPHA = 0.38f
private const val LIGHT_GLASS_SHADOW_ALPHA = 0.10f
private const val DARK_GLASS_SHADOW_ALPHA = 0.20f
private const val IDLE_INDICATOR_ALPHA = 0.10f
