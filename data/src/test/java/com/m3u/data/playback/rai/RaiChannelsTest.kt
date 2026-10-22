package com.m3u.data.playback.rai

import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaiChannelsTest {
    @Test
    fun `a channel is found by its identifier`() {
        val channel = RaiChannels.byCont("308718")

        assertEquals("Rai 2", channel?.title)
        assertEquals("rai2", channel?.key)
    }

    @Test
    fun `a channel is found by its title`() {
        assertEquals("rai2", RaiChannels.byTitle("Rai 2")?.key)
        assertEquals("rai2", RaiChannels.byTitle("RAI 2")?.key)
        assertEquals("rai2", RaiChannels.byTitle("Rai 2 HD")?.key)
        assertEquals("raimovie", RaiChannels.byTitle("Rai Movie")?.key)
        assertEquals("raisport", RaiChannels.byTitle("Rai Sport+ HD")?.key)
        assertEquals("rainews24", RaiChannels.byTitle("Rai News 24")?.key)
        assertEquals("rairadio2visual", RaiChannels.byTitle("Rai Radio 2 Visual")?.key)
    }

    @Test
    fun `a title of another channel is not a Rai channel`() {
        assertNull(RaiChannels.byTitle("Sky TG24"))
        assertNull(RaiChannels.byTitle(""))
    }

    @Test
    fun `the identifier of a relinker url is read`() {
        val url = "https://mediapolis.rai.it/relinker/relinkerServlet.htm" +
            "?cont=308718&output=7&forceUserAgent=raiplayappletv"

        assertEquals("308718", RaiUrls.relinkerCont(url.toHttpUrl()))
    }

    @Test
    fun `an url of another host is not a relinker url`() {
        val url = "https://example.com/relinker/relinkerServlet.htm?cont=308718"

        assertNull(RaiUrls.relinkerCont(url.toHttpUrl()))
    }

    @Test
    fun `a resolved live url is recognised`() {
        val url = "https://raidue1-live.akamaized.net/hls/live/598310/raidue1/rai2_5000/chunklist.m3u8"

        assertTrue(RaiUrls.isResolvedLiveUrl(url.toHttpUrl()))
    }

    @Test
    fun `a resolved live url of the other delivery network is recognised`() {
        val url = "https://raitre1-push.cdn.netrw.it/rai/hls-enc/tre/rai3/rai3_5000/chunklist.m3u8"

        assertTrue(RaiUrls.isResolvedLiveUrl(url.toHttpUrl()))
    }

    @Test
    fun `an url of another host is not a resolved live url`() {
        val url = "https://example.com/hls/live/598310/raidue1/rai2_5000/chunklist.m3u8"

        assertTrue(!RaiUrls.isResolvedLiveUrl(url.toHttpUrl()))
    }

    @Test
    fun `the live stream of a resolved url is read`() {
        val url = "https://raidue1-live.akamaized.net/hls/live/598310/raidue1/rai2_5000/chunklist.m3u8"

        assertEquals("598310", RaiUrls.liveKey(url.toHttpUrl()))
        assertEquals("raidue1", RaiUrls.slug(url.toHttpUrl()))
        assertEquals("raidue1", RaiUrls.hostPrefix(url.toHttpUrl()))
    }

    @Test
    fun `the live stream of the other delivery network is read`() {
        val url = "https://raitre1-push.cdn.netrw.it/rai/hls-enc/tre/rai3/rai3_5000/chunklist.m3u8"

        assertEquals("raitre1", RaiUrls.hostPrefix(url.toHttpUrl()))
    }

    @Test
    fun `the tokens of an url are redacted`() {
        val redacted = RaiUrls.redact(
            "https://raidue1-live.akamaized.net/hls/live/598310/raidue1/rai2_5000" +
                "/exp=1712345678/chunklist.m3u8?hdnea=token"
        )

        assertTrue(redacted.contains("/exp=***/"))
        assertTrue(redacted.contains("hdnea=***"))
        assertTrue(!redacted.contains("1712345678"))
        assertTrue(!redacted.contains("hdnea=token"))
    }

    @Test
    fun `a token of the query is redacted as a whole`() {
        val redacted = RaiUrls.redact(
            "https://raidue1-live.akamaized.net/hls/live/598310/playlist.m3u8" +
                "?hdnea=st=1712345678~exp=1712345778~acl=/*~hmac=0123456789"
        )

        assertTrue(redacted.contains("hdnea=***"))
        assertTrue(!redacted.contains("0123456789"))
    }

    @Test
    fun `the kodi options of an url are removed`() {
        assertEquals(
            "https://example.com/stream.m3u8",
            RaiUrls.withoutKodiOptions("https://example.com/stream.m3u8|user-agent=VLC"),
        )
    }
}
