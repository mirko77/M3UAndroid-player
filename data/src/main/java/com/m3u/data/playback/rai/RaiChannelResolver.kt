package com.m3u.data.playback.rai

import com.m3u.data.api.OkhttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import timber.log.Timber
import javax.inject.Inject

/**
 * A Rai channel which has to be resolved through the relinker before it can be played.
 */
data class RaiTarget(
    val cont: String,
    val channel: RaiChannel?,
    val relinkerUrl: String,
)

/**
 * The playable media playlist a Rai channel resolves to.
 */
data class RaiResolution(
    val cont: String,
    val channel: RaiChannel?,
    val mediaPlaylistUrl: String,
    val variantUri: String,
    val resolution: String?,
    val masterUrl: String,
    val statuses: List<String>,
)

/**
 * Raised when a Rai channel cannot be resolved.
 */
class RaiResolutionException(message: String) : Exception(message)

/**
 * Resolves a Rai channel to a playable media playlist.
 *
 * The Rai master playlists are not always complete: a channel may declare alternate
 * audio renditions which are not served at all and I-frame playlists which are served
 * for another channel. The player cannot skip them, so the channel is resolved here:
 * the relinker is asked for the master playlist, one video variant is picked and the
 * player is fed with the media playlist of that variant, which carries the muxed audio
 * and video of the channel.
 *
 * Every request uses the same user agent, because the media tokens are bound to the
 * user agent which fetched the master playlist.
 */
class RaiChannelResolver @Inject constructor(
    @OkhttpClient(false) okHttpClient: OkHttpClient,
) {
    private val timber = Timber.tag("RaiChannelResolver")

    private val client = okHttpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    /**
     * The Rai channel of an url, null when the url is not a Rai live stream.
     *
     * A relinker url carries its own identifier. An already resolved url is mapped back
     * to its channel, either by the live stream it belongs to or by the title of the
     * channel it is declared in.
     */
    fun detect(url: String, channelTitle: String? = null): RaiTarget? {
        val httpUrl = RaiUrls.withoutKodiOptions(url).toHttpUrlOrNull() ?: return null
        val cont = RaiUrls.relinkerCont(httpUrl)
        if (cont != null) {
            return RaiTarget(
                cont = cont,
                channel = RaiChannels.byCont(cont),
                relinkerUrl = httpUrl.toString(),
            )
        }
        if (!RaiUrls.isResolvedLiveUrl(httpUrl)) return null
        val channel = RaiChannelRegistry.channelFor(httpUrl)
            ?: channelTitle?.let { RaiChannels.byTitle(it) }
            ?: return null
        return RaiTarget(
            cont = channel.cont,
            channel = channel,
            relinkerUrl = RaiChannels.relinkerUrl(channel.cont),
        )
    }

    /**
     * Resolves a Rai channel to the media playlist the player can read.
     *
     * The resolution runs on the io dispatcher: reading a response body is a network
     * operation, which is not allowed on the main thread.
     */
    suspend fun resolve(target: RaiTarget): RaiResolution = withContext(Dispatchers.IO) {
        resolveOnIo(target)
    }

    private suspend fun resolveOnIo(target: RaiTarget): RaiResolution {
        val statuses = mutableListOf<String>()
        val master = fetchMaster(target, statuses)
        val variants = RaiMasterPlaylist.orderedVariants(RaiMasterPlaylist.parse(master.body))
        if (variants.isEmpty()) {
            throw RaiResolutionException(
                "The master playlist of ${target.cont} declares no video variant"
            )
        }
        var failure: RaiResolutionException? = null
        variants.forEach { variant ->
            val mediaUrl = master.url.resolve(variant.uri)?.toString()
            if (mediaUrl == null) {
                failure = RaiResolutionException(
                    "The variant ${variant.uri} of ${target.cont} is not a valid address"
                )
                return@forEach
            }
            val status = probe(mediaUrl, statuses)
            if (status !in SUCCESSFUL_STATUSES) {
                failure = RaiResolutionException(
                    "The media playlist of ${target.cont} answered $status"
                )
                return@forEach
            }
            val resolution = RaiResolution(
                cont = target.cont,
                channel = target.channel,
                mediaPlaylistUrl = mediaUrl,
                variantUri = variant.uri,
                resolution = variant.resolution,
                masterUrl = master.url.toString(),
                statuses = statuses.toList(),
            )
            RaiChannelRegistry.record(master.url, target.cont)
            log(target, resolution)
            return resolution
        }
        throw failure ?: RaiResolutionException("The channel ${target.cont} cannot be resolved")
    }

    /**
     * The url a probe has to read, null when the original url can be used.
     *
     * A probe must never be fatal, so a channel which cannot be resolved is reported as
     * a failure of the probe and not of the playback.
     */
    suspend fun probeUrl(url: String): String? {
        val target = detect(url) ?: return null
        return try {
            resolve(target).mediaPlaylistUrl
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            timber.w(failure, "The Rai probe url cannot be resolved")
            null
        }
    }

    private suspend fun fetchMaster(
        target: RaiTarget,
        statuses: MutableList<String>,
    ): MasterPlaylist {
        var current = target.relinkerUrl.toHttpUrlOrNull()
            ?: throw RaiResolutionException("The relinker url ${target.relinkerUrl} is invalid")
        var label = RELINKER_LABEL
        var redirects = 0
        while (redirects <= MAX_REDIRECTS) {
            val result = fetch(current, readBody = true)
            statuses += "$label=${result.status}"
            if (result.status in REDIRECT_STATUSES) {
                val location = result.location
                if (location == null) {
                    throw RaiResolutionException(
                        "The relinker of ${target.cont} redirected without an address"
                    )
                }
                current = current.resolve(location)
                    ?: throw RaiResolutionException(
                        "The relinker of ${target.cont} redirected to $location"
                    )
                label = MASTER_LABEL
                redirects++
                continue
            }
            if (result.status !in SUCCESSFUL_STATUSES) {
                throw RaiResolutionException(
                    "The $label of ${target.cont} answered ${result.status}"
                )
            }
            return MasterPlaylist(current, result.body)
        }
        throw RaiResolutionException(
            "The relinker of ${target.cont} redirected more than $MAX_REDIRECTS times"
        )
    }

    private suspend fun probe(url: String, statuses: MutableList<String>): Int {
        val httpUrl = url.toHttpUrlOrNull() ?: return 0
        val result = fetch(httpUrl, readBody = false)
        statuses += "$PLAYLIST_LABEL=${result.status}"
        return result.status
    }

    /**
     * A request to the Rai delivery networks.
     *
     * The whole exchange, the reading of the body and the closing of the response, runs
     * on the io dispatcher: reading a body is a network operation, which is not allowed
     * on the main thread.
     */
    private suspend fun fetch(url: HttpUrl, readBody: Boolean): HttpResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header(USER_AGENT_HEADER, RaiChannels.USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                HttpResult(
                    status = response.code,
                    location = response.header(LOCATION_HEADER),
                    body = if (readBody && response.isSuccessful) {
                        val content: ResponseBody? = response.body
                        content?.string().orEmpty()
                    } else {
                        ""
                    },
                )
            }
        }

    private fun log(target: RaiTarget, resolution: RaiResolution) {
        timber.i(
            buildString {
                append("Rai resolve: channel=")
                append(resolution.channel?.title ?: "unknown")
                append(" cont=")
                append(target.cont)
                append(" statuses=")
                append(resolution.statuses.joinToString(","))
                append(" master=")
                append(RaiUrls.redact(resolution.masterUrl))
                append(" variant=")
                append(RaiUrls.redact(resolution.variantUri))
                append(" resolution=")
                append(resolution.resolution ?: "unknown")
                append(" playlist=")
                append(RaiUrls.redact(resolution.mediaPlaylistUrl))
            }
        )
    }

    private data class MasterPlaylist(
        val url: HttpUrl,
        val body: String,
    )

    private data class HttpResult(
        val status: Int,
        val location: String?,
        val body: String,
    )

    private companion object {
        const val RELINKER_LABEL = "relinker"
        const val MASTER_LABEL = "master"
        const val PLAYLIST_LABEL = "playlist"
        const val LOCATION_HEADER = "Location"
        const val USER_AGENT_HEADER = "User-Agent"
        const val MAX_REDIRECTS = 3
        val REDIRECT_STATUSES = 300..399
        val SUCCESSFUL_STATUSES = 200..299
    }
}
