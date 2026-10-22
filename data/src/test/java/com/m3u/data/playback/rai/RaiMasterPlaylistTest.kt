package com.m3u.data.playback.rai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RaiMasterPlaylistTest {
    @Test
    fun `the video variants of a master playlist are read`() {
        val variants = RaiMasterPlaylist.parse(MASTER_PLAYLIST)

        assertEquals(2, variants.size)
        assertEquals(
            "../../../598310/raidue1/raidue1/rai2_5000/exp=1712345678/chunklist.m3u8",
            variants[0].uri,
        )
        assertEquals(1920, variants[0].width)
        assertEquals(1080, variants[0].height)
        assertEquals("1920x1080", variants[0].resolution)
        assertEquals(5_000_000L, variants[0].bandwidth)
    }

    @Test
    fun `the alternate renditions of a master playlist are ignored`() {
        val uris = RaiMasterPlaylist.parse(MASTER_PLAYLIST).map { it.uri }

        assertTrue(uris.none { it.contains("audio_ita") })
        assertTrue(uris.none { it.contains("audio_vo") })
        assertTrue(uris.none { it.contains("audio_desc") })
        assertTrue(uris.none { it.contains("iframe") })
    }

    @Test
    fun `the full high definition variant is preferred`() {
        val variants = RaiMasterPlaylist.orderedVariants(RaiMasterPlaylist.parse(MASTER_PLAYLIST))

        assertEquals("1920x1080", variants.first().resolution)
    }

    @Test
    fun `the high definition variant is preferred over a bigger one`() {
        val variants = RaiMasterPlaylist.orderedVariants(
            listOf(
                RaiVariant(uri = "ultra.m3u8", width = 3840, height = 2160, bandwidth = 12_000_000),
                RaiVariant(uri = "full.m3u8", width = 1920, height = 1080, bandwidth = 5_000_000),
            )
        )

        assertEquals("full.m3u8", variants.first().uri)
    }

    @Test
    fun `the high definition variant is used when there is no full one`() {
        val variants = RaiMasterPlaylist.orderedVariants(
            listOf(
                RaiVariant(uri = "ultra.m3u8", width = 3840, height = 2160, bandwidth = 12_000_000),
                RaiVariant(uri = "high.m3u8", width = 1280, height = 720, bandwidth = 3_000_000),
            )
        )

        assertEquals("high.m3u8", variants.first().uri)
    }

    @Test
    fun `the biggest variant is used when no preferred one is declared`() {
        val variants = RaiMasterPlaylist.orderedVariants(
            listOf(
                RaiVariant(uri = "small.m3u8", width = 640, height = 360, bandwidth = 1_000_000),
                RaiVariant(uri = "medium.m3u8", width = 1024, height = 576, bandwidth = 2_000_000),
            )
        )

        assertEquals("medium.m3u8", variants.first().uri)
    }

    @Test
    fun `a master playlist without variant is read as an empty one`() {
        assertTrue(RaiMasterPlaylist.parse("#EXTM3U\n#EXT-X-VERSION:3\n").isEmpty())
    }

    @Test
    fun `a variant without resolution has no resolution`() {
        val variants = RaiMasterPlaylist.parse(
            "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=800000\nchunklist.m3u8\n"
        )

        assertEquals(1, variants.size)
        assertNull(variants.first().resolution)
    }

    private companion object {
        val MASTER_PLAYLIST = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-INDEPENDENT-SEGMENTS
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_ita",NAME="Italiano",LANGUAGE="it",DEFAULT=YES,AUTOSELECT=YES,URI="../../../598310/raidue1/raidue1/rai2_ita/exp=1712345678/audio_ita.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_vo",NAME="Lingua Originale",LANGUAGE="V.O",DEFAULT=NO,AUTOSELECT=YES,URI="../../../598310/raidue1/raidue1/rai2_vo/exp=1712345678/audio_vo.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio_desc",NAME="Audiodescrizione",LANGUAGE="des",DEFAULT=NO,AUTOSELECT=YES,URI="../../../598310/raidue1/raidue1/rai2_desc/exp=1712345678/audio_desc.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,FRAME-RATE=50.000,CODECS="avc1.640028,mp4a.40.2",AUDIO="audio_ita"
            ../../../598310/raidue1/raidue1/rai2_5000/exp=1712345678/chunklist.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,FRAME-RATE=50.000,CODECS="avc1.64001f,mp4a.40.2",AUDIO="audio_ita"
            ../../../598310/raidue1/raidue1/rai2_3000/exp=1712345678/chunklist.m3u8
            #EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=100000,RESOLUTION=1920x1080,CODECS="avc1.640028",URI="../../../598308/raiuno1/raiuno1/rai1_5000/exp=1712345678/iframe.m3u8"
        """.trimIndent()
    }
}
