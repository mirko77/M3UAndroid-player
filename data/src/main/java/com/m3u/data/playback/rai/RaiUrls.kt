package com.m3u.data.playback.rai

import okhttp3.HttpUrl

/**
 * Inspection helpers for the Rai urls.
 */
internal object RaiUrls {
    private val LIVE_PATH = Regex("/hls/live/([^/]+)(?:/([^/]+))?")

    private val TOKEN_NAME = "(exp|hdnea|hdnts|hdntl|token|hmac|sig|signature|expires|" +
        "key-pair-id|keyname|aka_me_session_id)"

    private val PATH_TOKEN = Regex("(?i)\\b$TOKEN_NAME=([^&#/\\s]*)")

    private val QUERY_TOKEN = Regex("(?i)\\b$TOKEN_NAME=([^&#\\s]*)")

    /**
     * The `cont` identifier of a relinker url, null when the url is not a relinker url.
     */
    fun relinkerCont(url: HttpUrl): String? {
        if (!url.host.equals(RaiChannels.RELINKER_HOST, ignoreCase = true)) return null
        if (!url.encodedPath.contains("relinker", ignoreCase = true)) return null
        return url.queryParameter("cont")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    /**
     * True when the url points to an already resolved Rai live stream.
     *
     * The Rai channels are served by two content delivery networks, the Akamai one and
     * the Netrw one, and both of them expose the live streams below a `/hls` path.
     */
    fun isResolvedLiveUrl(url: HttpUrl): Boolean {
        val host = url.host
        val isRaiHost = host.endsWith("akamaized.net", ignoreCase = true) ||
            host.endsWith("cdn.netrw.it", ignoreCase = true)
        if (!isRaiHost) return false
        return url.encodedPath.contains("/hls", ignoreCase = true)
    }

    /**
     * The live stream key of a resolved url, for instance `598310`.
     */
    fun liveKey(url: HttpUrl): String? =
        LIVE_PATH.find(url.encodedPath)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }

    /**
     * The channel slug of a resolved url, for instance `raidue1`.
     */
    fun slug(url: HttpUrl): String? =
        LIVE_PATH.find(url.encodedPath)?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }

    /**
     * The host prefix of a resolved url, for instance `raidue1` for
     * `raidue1-live.akamaized.net` and `raitre1` for `raitre1-push.cdn.netrw.it`.
     */
    fun hostPrefix(url: HttpUrl): String? {
        val host = url.host
        val prefix = host.substringBefore("-live").substringBefore("-push")
        return prefix.takeIf { it.isNotEmpty() && !it.equals(host, ignoreCase = true) }
    }

    /**
     * The url without its Kodi options, which are player side settings and not part of
     * the address itself.
     */
    fun withoutKodiOptions(url: String): String = url.substringBefore('|')

    /**
     * The url with every token replaced, so that it can be logged safely.
     *
     * The tokens of the Akamai streams live in the path and the tokens of the Netrw
     * streams live in the query, where they may hold a slash, so both parts are
     * redacted on their own.
     */
    fun redact(url: String): String {
        val query = url.indexOf('?')
        if (query == -1) return PATH_TOKEN.replace(url) { match ->
            "${match.groupValues[1]}=***"
        }
        val path = PATH_TOKEN.replace(url.substring(0, query)) { match ->
            "${match.groupValues[1]}=***"
        }
        val parameters = QUERY_TOKEN.replace(url.substring(query + 1)) { match ->
            "${match.groupValues[1]}=***"
        }
        return "$path?$parameters"
    }
}
