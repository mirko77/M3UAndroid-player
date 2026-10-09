package com.m3u.testing

import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.m3u.i18n.R.string
import com.m3u.smartphone.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern
import kotlin.math.abs

class SubscriptionSourceSelectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun providerFormWorksInRequestedAccessibilityConfiguration() {
        val configuration = composeRule.runOnIdle {
            Configuration(composeRule.activity.resources.configuration)
        }
        val matrixCase = requestedAccessibilityMatrixCase()
        assertRequestedAccessibilityConfiguration(configuration, matrixCase)

        openSourcePicker()
        assertSourceRowAccessibility(
            sourceKey = M3U_SOURCE_KEY,
            labelResId = string.feat_setting_data_source_m3u,
        )
        clickSourceAcrossFullRow(M3U_SOURCE_KEY)
        waitUntilTagExists(editorTag(M3U_SOURCE_KEY))
    }

    @Test
    fun sourceRowsExposeLocalizedNamesAndButtonRolesAndCanNavigateBack() {
        openSourcePicker()

        assertSourceRowAccessibility(
            sourceKey = M3U_SOURCE_KEY,
            labelResId = string.feat_setting_data_source_m3u,
        )
        composeRule.onNodeWithTag(sourceTag(M3U_SOURCE_KEY)).performClick()
        waitUntilTagExists(editorTag(M3U_SOURCE_KEY))

        device.pressBack()
        waitUntilTagExists(SOURCE_PICKER_TAG)
        assertSourceRowAccessibility(
            sourceKey = M3U_SOURCE_KEY,
            labelResId = string.feat_setting_data_source_m3u,
        )
    }

    @Test
    fun overviewSourcePickerAndEditorBackStackRestoreEachLevel() {
        openSourcePicker()
        scrollSourcePickerTo(sourceTag(M3U_SOURCE_KEY))
        clickSourceAcrossFullRow(M3U_SOURCE_KEY)

        device.pressBack()
        waitUntilTagExists(SOURCE_PICKER_TAG)
        composeRule.onNodeWithTag(sourceTag(M3U_SOURCE_KEY))
            .assertHasClickAction()

        device.pressBack()
        waitUntilTagExists(OVERVIEW_TAG)
        composeRule.onNodeWithTag(ADD_ACTION_TAG).assertHasClickAction()

        device.pressBack()
        waitUntilTagGone(OVERVIEW_TAG)
        device.clickRequiredObject(
            By.text(
                caseInsensitive(
                    context.getString(string.feat_setting_playlist_management)
                )
            )
        )
        waitUntilTagExists(OVERVIEW_TAG)
        composeRule.onNodeWithTag(ADD_ACTION_TAG).assertHasClickAction()
    }

    @Test
    fun overviewDestinationsOpenDedicatedManagementLists() {
        openPlaylistManagementOverview()

        assertOverviewDestination(
            actionTag = HIDDEN_CHANNELS_ACTION_TAG,
            destinationTag = HIDDEN_CHANNELS_LIST_TAG,
        )

        composeRule.onNodeWithTag(BACKUP_ACTION_TAG).run {
            performScrollTo()
            assertHasClickAction()
        }
        composeRule.onNodeWithTag(RESTORE_ACTION_TAG).run {
            performScrollTo()
            assertHasClickAction()
        }
    }

    private fun openPlaylistManagementOverview() {
        if (tagExists(OVERVIEW_TAG)) return

        val settingsDestination = hasContentDescription(
            context.getString(string.ui_destination_setting),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()
        val playlistManagement = hasText(
            context.getString(string.feat_setting_playlist_management),
            substring = false,
            ignoreCase = true,
        ) and hasClickAction()

        waitUntilMatcherExists(
            playlistManagement or settingsDestination,
        )
        if (composeRule.onAllNodes(playlistManagement).fetchSemanticsNodes().isEmpty()) {
            composeRule.onNode(settingsDestination).performClick()
        }
        waitUntilMatcherExists(playlistManagement)
        composeRule.onNode(playlistManagement).performClick()
        waitUntilTagExists(OVERVIEW_TAG)
    }

    private fun openSourcePicker() {
        openPlaylistManagementOverview()
        composeRule.onNodeWithTag(ADD_ACTION_TAG).run {
            performScrollTo()
            performClick()
        }
        waitUntilTagExists(SOURCE_PICKER_TAG)
    }

    private fun assertOverviewDestination(
        actionTag: String,
        destinationTag: String,
    ) {
        composeRule.onNodeWithTag(actionTag).run {
            performScrollTo()
            assertHasClickAction()
            performClick()
        }
        waitUntilTagExists(destinationTag)
        device.pressBack()
        waitUntilTagExists(OVERVIEW_TAG)
    }

    private fun clickSourceAcrossFullRow(sourceKey: String) {
        val tag = sourceTag(sourceKey)
        scrollSourcePickerTo(tag)
        val row = composeRule.onNodeWithTag(tag)
        row.assertHasClickAction()
        val bounds = row.fetchSemanticsNode().boundsInWindow
        val density = composeRule.activity.resources.displayMetrics.density
        assertTrue(
            "Source row $sourceKey must provide at least a 48dp touch target: $bounds",
            bounds.height >= MINIMUM_TOUCH_TARGET_DP * density,
        )

        val isRtl =
            context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val edgeInset = FULL_ROW_CLICK_INSET_DP * density
        val x = if (isRtl) bounds.left + edgeInset else bounds.right - edgeInset
        assertTrue(
            "Could not click the logical end of source row $sourceKey at $bounds",
            device.click(x.toInt(), bounds.center.y.toInt()),
        )
        waitUntilTagExists(editorTag(sourceKey))
        composeRule.waitForIdle()
    }

    private fun assertSourceRowAccessibility(
        sourceKey: String,
        labelResId: Int,
    ) {
        val tag = sourceTag(sourceKey)
        scrollSourcePickerTo(tag)
        val node = composeRule.onNodeWithTag(tag)
        node.assertHasClickAction()
        val semanticsNode = node.fetchSemanticsNode()
        assertEquals(
            "Source row $sourceKey must expose a button role",
            Role.Button,
            semanticsNode.config[SemanticsProperties.Role],
        )
        val density = composeRule.activity.resources.displayMetrics.density
        assertTrue(
            "Source row $sourceKey has a touch target shorter than 48dp",
            semanticsNode.boundsInWindow.height >= MINIMUM_TOUCH_TARGET_DP * density,
        )

        val localizedLabel = context.getString(labelResId).withoutBidiControls()
        node.assertTextContains(
            localizedLabel,
            substring = true,
            ignoreCase = true,
        )
    }

    private fun scrollSourcePickerTo(tag: String) {
        composeRule.waitUntil(UI_TIMEOUT_MILLIS) {
            runCatching {
                composeRule.onNodeWithTag(SOURCE_PICKER_TAG)
                    .performScrollToNode(hasTestTag(tag))
            }.isSuccess
        }
        waitUntilTagExists(tag)
        composeRule.waitForIdle()
    }

    private fun assertRequestedAccessibilityConfiguration(
        configuration: Configuration,
        matrixCase: String,
    ) {
        when (matrixCase) {
            MATRIX_CASE_COMPACT_LTR,
            MATRIX_CASE_COMPACT_NARROW_LTR -> {
                assertLocale(configuration, LOCALE_ENGLISH, matrixCase)
                assertLayoutDirection(configuration, View.LAYOUT_DIRECTION_LTR, matrixCase)
                assertTrue(
                    "Expected normal text for $matrixCase, actual=${configuration.fontScale}",
                    configuration.fontScale < LARGE_TEXT_THRESHOLD,
                )
                assertTrue(
                    "Expected compact width for $matrixCase, " +
                        "actual=${configuration.screenWidthDp}",
                    configuration.screenWidthDp < WIDE_WINDOW_MINIMUM_DP,
                )
                if (matrixCase == MATRIX_CASE_COMPACT_NARROW_LTR) {
                    assertTrue(
                        "Expected a 320dp narrow window, " +
                            "actual=${configuration.screenWidthDp}",
                        configuration.screenWidthDp in NARROW_WIDTH_RANGE,
                    )
                }
            }

            MATRIX_CASE_COMPACT_RTL_LARGE -> {
                assertLocale(configuration, LOCALE_RTL_PSEUDO, matrixCase)
                assertLayoutDirection(configuration, View.LAYOUT_DIRECTION_RTL, matrixCase)
                assertTrue(
                    "Expected 200% text for $matrixCase, actual=${configuration.fontScale}",
                    configuration.fontScale >= LARGE_TEXT_MINIMUM_SCALE,
                )
                assertTrue(
                    "Expected compact width for $matrixCase, " +
                        "actual=${configuration.screenWidthDp}",
                    configuration.screenWidthDp < WIDE_WINDOW_MINIMUM_DP,
                )
            }

            MATRIX_CASE_WIDE_LTR -> {
                assertLocale(configuration, LOCALE_ENGLISH, matrixCase)
                assertLayoutDirection(configuration, View.LAYOUT_DIRECTION_LTR, matrixCase)
                assertTrue(
                    "Expected normal text for $matrixCase, actual=${configuration.fontScale}",
                    configuration.fontScale < LARGE_TEXT_THRESHOLD,
                )
                assertTrue(
                    "Expected wide width for $matrixCase, " +
                        "actual=${configuration.screenWidthDp}",
                    configuration.screenWidthDp >= WIDE_WINDOW_MINIMUM_DP,
                )
            }

            else -> error(
                "Unknown $ARG_ACCESSIBILITY_MATRIX_CASE=$matrixCase. " +
                    "Expected $MATRIX_CASE_COMPACT_LTR, " +
                    "$MATRIX_CASE_COMPACT_NARROW_LTR, " +
                    "$MATRIX_CASE_COMPACT_RTL_LARGE, or $MATRIX_CASE_WIDE_LTR.",
            )
        }
    }

    private fun requestedAccessibilityMatrixCase(): String =
        InstrumentationRegistry.getArguments()
            .getString(ARG_ACCESSIBILITY_MATRIX_CASE)
            ?: error(
                "Missing required instrumentation argument " +
                    "$ARG_ACCESSIBILITY_MATRIX_CASE. Run " +
                    "testing/bin/run-smartphone-provider-ui-matrix.sh.",
            )

    private fun assertLocale(
        configuration: Configuration,
        expected: String,
        matrixCase: String,
    ) {
        assertEquals(
            "Unexpected app locale for $matrixCase",
            expected,
            configuration.locales[0].toLanguageTag(),
        )
    }

    private fun assertLayoutDirection(
        configuration: Configuration,
        expected: Int,
        matrixCase: String,
    ) {
        assertEquals(
            "Unexpected layout direction for $matrixCase",
            expected,
            configuration.layoutDirection,
        )
    }

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

    private fun UiDevice.findRequiredObject(selector: BySelector): UiObject2 =
        wait(Until.findObject(selector), UI_TIMEOUT_MILLIS)
            ?: error("Required UI object was not found: $selector")

    private fun UiDevice.clickRequiredObject(selector: BySelector) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        var lastFailure: StaleObjectException? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val target = findObject(selector)
            if (target != null) {
                try {
                    target.clickableAncestor().click()
                    return
                } catch (failure: StaleObjectException) {
                    lastFailure = failure
                }
            }
            SystemClock.sleep(TAG_POLL_MILLIS)
        }
        throw AssertionError(
            "Required UI object could not be clicked: $selector",
            lastFailure,
        )
    }

    private fun caseInsensitive(value: String): Pattern = Pattern.compile(
        Pattern.quote(value),
        Pattern.CASE_INSENSITIVE,
    )

    private fun caseInsensitiveContaining(value: String): Pattern = Pattern.compile(
        ".*${Pattern.quote(value)}.*",
        Pattern.CASE_INSENSITIVE,
    )

    private fun String.withoutBidiControls(): String = filterNot { char ->
        char == '\u061C' ||
            char == '\u200E' ||
            char == '\u200F' ||
            char in '\u202A'..'\u202E' ||
            char in '\u2066'..'\u2069'
    }

    private fun UiObject2.clickableAncestor(): UiObject2 {
        var current = this
        while (!current.isClickable) {
            current = current.parent ?: return this
        }
        return current
    }

    private fun sourceTag(sourceKey: String): String = "playlist-source:$sourceKey"

    private fun editorTag(sourceKey: String): String = "playlist-editor:$sourceKey"

    private companion object {
        const val UI_TIMEOUT_MILLIS = 15_000L
        const val TAG_POLL_MILLIS = 50L
        const val MINIMUM_TOUCH_TARGET_DP = 48
        const val FULL_ROW_CLICK_INSET_DP = 12
        const val BOUNDS_TOLERANCE_PX = 2
        const val ARG_ACCESSIBILITY_MATRIX_CASE = "accessibilityMatrixCase"
        const val MATRIX_CASE_COMPACT_LTR = "compact-ltr"
        const val MATRIX_CASE_COMPACT_NARROW_LTR = "compact-narrow-ltr"
        const val MATRIX_CASE_COMPACT_RTL_LARGE = "compact-rtl-large"
        const val MATRIX_CASE_WIDE_LTR = "wide-ltr"
        const val LOCALE_ENGLISH = "en"
        const val LOCALE_RTL_PSEUDO = "ar-XB"
        const val LARGE_TEXT_MINIMUM_SCALE = 1.95f
        const val LARGE_TEXT_THRESHOLD = 1.3f
        const val WIDE_WINDOW_MINIMUM_DP = 600
        val NARROW_WIDTH_RANGE = 315..325

        const val OVERVIEW_TAG = "playlist-management-overview"
        const val ADD_ACTION_TAG = "playlist-add-action"
        const val SOURCE_PICKER_TAG = "playlist-source-picker"
        const val HIDDEN_CHANNELS_ACTION_TAG = "playlist-overview-hidden-channels"
        const val HIDDEN_CHANNELS_LIST_TAG = "playlist-list:hidden-channels"
        const val BACKUP_ACTION_TAG = "playlist-backup-action"
        const val RESTORE_ACTION_TAG = "playlist-restore-action"
        const val M3U_SOURCE_KEY = "data-source:m3u"
    }
}
