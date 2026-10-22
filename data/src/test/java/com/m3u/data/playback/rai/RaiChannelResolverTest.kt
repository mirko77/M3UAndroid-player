package com.m3u.data.playback.rai

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.BufferedSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaiChannelResolverTest {
    private val server = MockWebServer()

    private val resolver = RaiChannelResolver(OkHttpClient())

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun `a relinker url is resolved to the media playlist of a variant`() = runBlocking {
        server.start()
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", server.url("/hls/live/598310/master.m3u8").toString())
                .body("")
                .build()
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(MASTER_PLAYLIST)
                .build()
        )
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("#EXTM3U\n#EXT-X-TARGETDURATION:10\n")
                .build()
        )

        val resolution = resolver.resolve(
            RaiTarget(
                cont = "308718",
                channel = RaiChannels.byCont("308718"),
                relinkerUrl = server.url("/relinker/relinkerServlet.htm?cont=308718").toString(),
            )
        )

        assertEquals("308718", resolution.cont)
        assertEquals("Rai 2", resolution.channel?.title)
        assertEquals("1920x1080", resolution.resolution)
        assertEquals(
            server.url("/hls/live/598310/raidue1/rai2_5000/chunklist.m3u8").toString(),
            resolution.mediaPlaylistUrl,
        )
        assertEquals(listOf("relinker=302", "master=200", "playlist=200"), resolution.statuses)
    }

    @Test
    fun `every request of a resolution uses the same user agent`() = runBlocking {
        server.start()
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", server.url("/master.m3u8").toString())
                .body("")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body(MASTER_PLAYLIST).build())
        server.enqueue(MockResponse.Builder().code(200).body("#EXTM3U\n").build())

        resolver.resolve(
            RaiTarget(
                cont = "308718",
                channel = RaiChannels.byCont("308718"),
                relinkerUrl = server.url("/relinker?cont=308718").toString(),
            )
        )

        val userAgents = mutableListOf<String?>()
        repeat(3) {
            userAgents += server.takeRequest().headers["User-Agent"]
        }
        assertEquals(listOf(RaiChannels.USER_AGENT), userAgents.distinct())
    }

    @Test
    fun `a broken variant is skipped in favour of a working one`() = runBlocking {
        server.start()
        server.enqueue(MockResponse.Builder().code(200).body(MASTER_PLAYLIST).build())
        server.enqueue(MockResponse.Builder().code(404).body("").build())
        server.enqueue(MockResponse.Builder().code(200).body("#EXTM3U\n").build())

        val resolution = resolver.resolve(
            RaiTarget(
                cont = "308718",
                channel = RaiChannels.byCont("308718"),
                relinkerUrl = server.url("/relinker?cont=308718").toString(),
            )
        )

        assertEquals("1280x720", resolution.resolution)
        assertEquals(
            server.url("/hls/live/598310/raidue1/rai2_3000/chunklist.m3u8").toString(),
            resolution.mediaPlaylistUrl,
        )
        assertEquals(listOf("relinker=200", "playlist=404", "playlist=200"), resolution.statuses)
    }

    @Test
    fun `the resolution never reads the network on the calling thread`() = runBlocking {
        val readers = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .body(RecordingBody(response.body, readers))
                    .build()
            }
            .build()
        val resolver = RaiChannelResolver(client)
        server.start()
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", server.url("/master.m3u8").toString())
                .body("")
                .build()
        )
        server.enqueue(MockResponse.Builder().code(200).body(MASTER_PLAYLIST).build())
        server.enqueue(MockResponse.Builder().code(200).body("#EXTM3U\n").build())

        resolver.resolve(
            RaiTarget(
                cont = "308718",
                channel = RaiChannels.byCont("308718"),
                relinkerUrl = server.url("/relinker?cont=308718").toString(),
            )
        )

        val caller = Thread.currentThread().name
        assertTrue(readers.isNotEmpty())
        readers.forEach { reader ->
            assertNotEquals(caller, reader, "the body was read on the calling thread")
        }
    }

    @Test
    fun `a relinker url is detected with its identifier`() {
        val target = resolver.detect(
            "https://mediapolis.rai.it/relinker/relinkerServlet.htm" +
                "?cont=308718&output=7&forceUserAgent=raiplayappletv",
            "Rai 2",
        )

        assertNotNull(target)
        assertEquals("308718", target.cont)
        assertEquals("Rai 2", target.channel?.title)
    }

    @Test
    fun `a resolved url is mapped back to its channel`() {
        val target = resolver.detect(
            "https://raidue1-live.akamaized.net/hls/live/598310/raidue1/rai2_5000" +
                "/exp=1712345678/chunklist.m3u8",
            "Rai 2",
        )

        assertNotNull(target)
        assertEquals("308718", target.cont)
        assertEquals("Rai 2", target.channel?.title)
        assertTrue(target.relinkerUrl.contains("cont=308718"))
    }

    @Test
    fun `a resolved url of an unknown channel is mapped by its title`() {
        val target = resolver.detect(
            "https://raitre1-live.akamaized.net/hls/live/598309/raitre1/rai3_5000/chunklist.m3u8",
            "Rai 3",
        )

        assertNotNull(target)
        assertEquals("308709", target.cont)
        assertEquals("Rai 3", target.channel?.title)
    }

    @Test
    fun `a resolved url of the other delivery network is mapped back to its channel`() {
        val target = resolver.detect(
            "https://raitre1-push.cdn.netrw.it/rai/hls-enc/tre/rai3/rai3_5000/chunklist.m3u8",
            "Rai 3",
        )

        assertNotNull(target)
        assertEquals("308709", target.cont)
        assertEquals("Rai 3", target.channel?.title)
    }

    @Test
    fun `an url of another channel is not a Rai channel`() {
        assertNull(resolver.detect("https://example.com/stream.m3u8", "Example"))
        assertNull(resolver.detect("not an url", "Rai 2"))
    }

    private companion object {
        val MASTER_PLAYLIST = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_ita",NAME="Italiano",LANGUAGE="it",DEFAULT=YES,AUTOSELECT=YES,URI="audio_ita.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_vo",NAME="Lingua Originale",LANGUAGE="V.O",DEFAULT=NO,AUTOSELECT=YES,URI="audio_vo.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2",AUDIO="audio_ita"
            /hls/live/598310/raidue1/rai2_5000/chunklist.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2",AUDIO="audio_ita"
            /hls/live/598310/raidue1/rai2_3000/chunklist.m3u8
            #EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=100000,RESOLUTION=1920x1080,CODECS="avc1.640028",URI="/hls/live/598308/raiuno1/iframe.m3u8"
        """.trimIndent()
    }

    /**
     * A body which remembers the thread which reads it.
     */
    private class RecordingBody(
        private val delegate: ResponseBody?,
        private val readers: MutableList<String>,
    ) : ResponseBody() {
        override fun contentType(): MediaType? = delegate?.contentType()

        override fun contentLength(): Long = delegate?.contentLength() ?: -1L

        override fun source(): BufferedSource {
            readers += Thread.currentThread().name
            val body = delegate ?: throw IllegalStateException("the response has no body")
            return body.source()
        }
    }
}
