package com.m3u.testing

import android.content.res.Configuration
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.m3u.i18n.R.string
import com.m3u.smartphone.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistAdaptiveLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun narrowWidthOverviewEditorActionsRemainUsable() {
        val configuration = currentConfiguration()
        assertEquals(
            MATRIX_CASE_COMPACT_NARROW_LTR,
            requestedAccessibilityMatrixCase(),
        )
        assertTrue(
            "Expected a 320dp narrow window, actual=${configuration.screenWidthDp}",
            configuration.screenWidthDp in NARROW_WIDTH_RANGE,
        )

        openPlaylistManagementOverview()
        composeRule.onNodeWithTag(ADD_ACTION_TAG).run {
            performScrollTo()
            performClick()
        }
        waitUntilTagExists(M3U_EDITOR_TAG)

        val submitBounds = composeRule.onNodeWithTag(SUBMIT_ACTION_TAG)
            .fetchSemanticsNode()
            .boundsInWindow
        val pasteAction = hasContentDescription(
            context.getString(string.feat_setting_label_parse_from_clipboard),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()
        waitUntilMatcherExists(pasteAction)
        val pasteBounds = composeRule.onNode(pasteAction)
            .fetchSemanticsNode()
            .boundsInWindow
        val density = composeRule.density.density

        assertTrue(
            "Submit action escaped the 320dp window: $submitBounds",
            submitBounds.left >= 0f && submitBounds.right <= device.displayWidth,
        )
        assertTrue(
            "Paste action escaped the 320dp window: $pasteBounds",
            pasteBounds.left >= 0f && pasteBounds.right <= device.displayWidth,
        )
        assertFalse(
            "Submit and paste actions overlap at 320dp: " +
                "submit=$submitBounds, paste=$pasteBounds",
            submitBounds.intersects(pasteBounds),
        )
        assertTrue(
            "Paste action must keep a 48dp touch target: $pasteBounds",
            pasteBounds.width >= MINIMUM_TOUCH_TARGET_DP * density &&
                pasteBounds.height >= MINIMUM_TOUCH_TARGET_DP * density,
        )
    }

    @Test
    fun mediumWidthSideRailUsesSinglePlaylistPaneHeadersAndBackNavigation() {
        val configuration = currentConfiguration()
        assertEquals(MATRIX_CASE_MEDIUM_LTR, requestedAccessibilityMatrixCase())
        assertEquals(
            "The medium tablet case must use English resources",
            LOCALE_ENGLISH,
            configuration.locales[0].toLanguageTag(),
        )
        assertEquals(
            "The medium tablet case must be LTR",
            View.LAYOUT_DIRECTION_LTR,
            configuration.layoutDirection,
        )
        assertTrue(
            "Expected 600–839dp medium width, actual=${configuration.screenWidthDp}",
            configuration.screenWidthDp in MEDIUM_WIDTH_RANGE,
        )
        assertTrue(
            "The medium tablet case should use normal font scale",
            configuration.fontScale < LARGE_TEXT_THRESHOLD,
        )

        openPlaylistManagementOverview()
        assertSideRailAndSinglePane()
        val overviewBack = assertPaneHeader(
            contentTag = OVERVIEW_TAG,
            title = context.getString(string.feat_setting_playlist_management),
        )

        composeRule.onNodeWithTag(ADD_ACTION_TAG).run {
            performScrollTo()
            performClick()
        }
        waitUntilTagExists(M3U_EDITOR_TAG)
        val editorBack = assertPaneHeader(
            contentTag = M3U_EDITOR_TAG,
            title = context.getString(
                string.feat_setting_label_add_playlist
            ),
        )
        clickBoundsCenter(editorBack)
        waitUntilTagGone(M3U_EDITOR_TAG)
        waitUntilTagExists(OVERVIEW_TAG)

        val restoredOverviewBack = assertPaneHeader(
            contentTag = OVERVIEW_TAG,
            title = context.getString(string.feat_setting_playlist_management),
        )
        assertEquals(
            "The overview pane header moved after returning from the playlist editor",
            overviewBack,
            restoredOverviewBack,
        )
        clickBoundsCenter(restoredOverviewBack)
        waitUntilTagGone(OVERVIEW_TAG)
        waitUntilMatcherExists(playlistManagementEntryMatcher())
    }

    private fun openPlaylistManagementOverview() {
        if (tagExists(OVERVIEW_TAG)) return

        val playlistManagement = playlistManagementEntryMatcher()
        val settingsDestination = hasContentDescription(
            context.getString(string.ui_destination_setting),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()
        waitUntilMatcherExists(playlistManagement or settingsDestination)
        if (composeRule.onAllNodes(playlistManagement).fetchSemanticsNodes().isEmpty()) {
            composeRule.onNode(settingsDestination).performClick()
        }
        waitUntilMatcherExists(playlistManagement)
        composeRule.onNode(playlistManagement).performClick()
        waitUntilTagExists(OVERVIEW_TAG)
    }

    private fun assertSideRailAndSinglePane() {
        val overviewBounds = composeRule.onNodeWithTag(OVERVIEW_TAG)
            .fetchSemanticsNode()
            .boundsInWindow
        val density = composeRule.density.density
        assertTrue(
            "Playlist content did not leave space for the side rail: " +
                "overview=$overviewBounds",
            overviewBounds.left >= MINIMUM_SIDE_RAIL_WIDTH_DP * density,
        )
        assertTrue(
            "Playlist detail did not occupy the medium window's single content pane: " +
                "overview=$overviewBounds, displayWidth=${device.displayWidth}",
            overviewBounds.width >=
                device.displayWidth * MINIMUM_SINGLE_PANE_WIDTH_FRACTION,
        )
    }

    private fun assertPaneHeader(
        contentTag: String,
        title: String,
    ): Rect {
        val contentBounds = composeRule.onNodeWithTag(contentTag)
            .fetchSemanticsNode()
            .boundsInWindow
        val heading = hasText(
            title,
            substring = false,
            ignoreCase = true,
        ) and SemanticsMatcher("is heading") { node ->
            node.config.contains(SemanticsProperties.Heading)
        }
        val headingBounds = composeRule.onAllNodes(heading)
            .fetchSemanticsNodes()
            .map { node -> node.boundsInWindow }
            .firstOrNull { bounds -> bounds.isHeaderFor(contentBounds) }
            ?: error(
                "Playlist pane heading was not found above $contentTag: " +
                    "title=$title, content=$contentBounds",
            )
        val back = hasContentDescription(
            context.getString(string.ui_cd_top_bar_on_back_pressed),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()
        val backNode = composeRule.onAllNodes(back)
            .fetchSemanticsNodes()
            .firstOrNull { node -> node.boundsInWindow.isHeaderFor(contentBounds) }
            ?: error(
                "Playlist pane back action was not found above $contentTag: " +
                    "content=$contentBounds",
            )
        val backBounds = backNode.boundsInWindow
        val touchBounds = backNode.touchBoundsInRoot
        val touchDensity = backNode.layoutInfo.density.density

        assertTrue(
            "Pane heading overlaps content: heading=$headingBounds, " +
                "content=$contentBounds",
            headingBounds.bottom <= contentBounds.top + BOUNDS_TOLERANCE_PX,
        )
        assertTrue(
            "Pane back action must keep a 48dp touch target: $touchBounds",
            touchBounds.width >= MINIMUM_TOUCH_TARGET_DP * touchDensity &&
                touchBounds.height >= MINIMUM_TOUCH_TARGET_DP * touchDensity,
        )
        assertFalse(
            "Pane heading overlaps its back action: " +
                "heading=$headingBounds, back=$backBounds",
            headingBounds.intersects(backBounds),
        )
        return backBounds
    }

    private fun clickBoundsCenter(bounds: Rect) {
        assertTrue(
            "Could not click pane action at $bounds",
            device.click(bounds.center.x.toInt(), bounds.center.y.toInt()),
        )
    }

    private fun playlistManagementEntryMatcher(): SemanticsMatcher =
        hasText(
            context.getString(string.feat_setting_playlist_management),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()

    private fun currentConfiguration(): Configuration = composeRule.runOnIdle {
        Configuration(composeRule.activity.resources.configuration)
    }

    private fun requestedAccessibilityMatrixCase(): String =
        InstrumentationRegistry.getArguments()
            .getString(ARG_ACCESSIBILITY_MATRIX_CASE)
            ?: error(
                "Missing required instrumentation argument " +
                    "$ARG_ACCESSIBILITY_MATRIX_CASE. Run " +
                    "testing/bin/run-smartphone-provider-ui-matrix.sh.",
            )

    private fun waitUntilTagExists(tag: String) {
        composeRule.waitUntil(UI_TIMEOUT_MILLIS) { tagExists(tag) }
    }

    private fun waitUntilTagGone(tag: String) {
        composeRule.waitUntil(UI_TIMEOUT_MILLIS) { !tagExists(tag) }
    }

    private fun waitUntilMatcherExists(matcher: SemanticsMatcher) {
        composeRule.waitUntil(UI_TIMEOUT_MILLIS) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tagExists(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun Rect.isHeaderFor(content: Rect): Boolean =
        bottom <= content.top + BOUNDS_TOLERANCE_PX &&
            right > content.left &&
            left < content.right

    private fun Rect.intersects(other: Rect): Boolean =
        left < other.right &&
            right > other.left &&
            top < other.bottom &&
            bottom > other.top

    private fun Rect.contains(other: Rect): Boolean =
        left <= other.left + BOUNDS_TOLERANCE_PX &&
            top <= other.top + BOUNDS_TOLERANCE_PX &&
            right >= other.right - BOUNDS_TOLERANCE_PX &&
            bottom >= other.bottom - BOUNDS_TOLERANCE_PX

    private companion object {
        const val UI_TIMEOUT_MILLIS = 15_000L
        const val ARG_ACCESSIBILITY_MATRIX_CASE = "accessibilityMatrixCase"
        const val MATRIX_CASE_MEDIUM_LTR = "medium-ltr"
        const val MATRIX_CASE_COMPACT_NARROW_LTR = "compact-narrow-ltr"
        const val LOCALE_ENGLISH = "en"
        const val MEDIUM_WIDTH_MINIMUM_DP = 600
        const val MEDIUM_WIDTH_MAXIMUM_DP = 839
        val MEDIUM_WIDTH_RANGE =
            MEDIUM_WIDTH_MINIMUM_DP..MEDIUM_WIDTH_MAXIMUM_DP
        val NARROW_WIDTH_RANGE = 315..325
        const val LARGE_TEXT_THRESHOLD = 1.3f
        const val MINIMUM_TOUCH_TARGET_DP = 48
        const val MINIMUM_SIDE_RAIL_WIDTH_DP = 72
        const val MINIMUM_SINGLE_PANE_WIDTH_FRACTION = 0.75f
        const val BOUNDS_TOLERANCE_PX = 2f

        const val OVERVIEW_TAG = "playlist-management-overview"
        const val ADD_ACTION_TAG = "playlist-add-action"
        const val M3U_EDITOR_TAG = "playlist-editor:data-source:m3u"
        const val SUBMIT_ACTION_TAG = "subscription-submit-action"
    }
}
