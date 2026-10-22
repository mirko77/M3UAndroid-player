package com.m3u.data.playback.rai

import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which Rai channel an already resolved live url belongs to.
 *
 * The mapping is learned from the master playlists which have already been resolved, so
 * that a url pasted into a playlist can be mapped back to its channel even when the
 * channel is not part of the static table.
 */
internal object RaiChannelRegistry {
    private val contByLiveKey = ConcurrentHashMap<String, String>()
    private val contBySlug = ConcurrentHashMap<String, String>()
    private val contByHost = ConcurrentHashMap<String, String>()

    fun record(url: HttpUrl, cont: String) {
        RaiUrls.liveKey(url)?.let { contByLiveKey[it] = cont }
        RaiUrls.slug(url)?.lowercase()?.let { contBySlug[it] = cont }
        RaiUrls.hostPrefix(url)?.lowercase()?.let { contByHost[it] = cont }
    }

    /**
     * The channel of an already resolved live url, null when the url is unknown.
     */
    fun channelFor(url: HttpUrl): RaiChannel? {
        val learned = RaiUrls.liveKey(url)?.let { contByLiveKey[it] }
            ?: RaiUrls.slug(url)?.lowercase()?.let { contBySlug[it] }
            ?: RaiUrls.hostPrefix(url)?.lowercase()?.let { contByHost[it] }
        val channel = learned?.let { cont -> RaiChannels.byCont(cont) }
            ?: RaiChannels.byResolvedUrl(
                liveKey = RaiUrls.liveKey(url),
                slug = RaiUrls.slug(url),
                hostPrefix = RaiUrls.hostPrefix(url),
            )
        return channel
    }
}
