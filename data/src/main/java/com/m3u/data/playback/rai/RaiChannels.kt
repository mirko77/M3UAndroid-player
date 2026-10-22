package com.m3u.data.playback.rai

/**
 * A Rai live channel, addressed by the identifier the Rai relinker understands.
 */
data class RaiChannel(
    val key: String,
    val title: String,
    val cont: String,
    val aliases: List<String>,
)

/**
 * The Rai live channels and the rules used to recognise them.
 *
 * The `cont` identifiers are the ones published by the Rai relinker and they are stable:
 * the relinker resolves them to the current live master playlist of the channel.
 */
object RaiChannels {
    /**
     * The single User-Agent used for every Rai request.
     *
     * The relinker rejects desktop user agents and the media tokens are bound to the
     * user agent which fetched the master playlist, so the very same value has to be
     * used for the relinker, the master playlist, the media playlist and the segments.
     */
    const val USER_AGENT = "Rai"

    const val RELINKER_HOST = "mediapolis.rai.it"
    const val RELINKER_PATH = "/relinker/relinkerServlet.htm"

    private const val RELINKER_QUERY = "output=7&forceUserAgent=raiplayappletv"

    /**
     * The live stream identifiers of the channels whose Akamai path is publicly known.
     *
     * They are only used to map an already resolved url back to its channel, the
     * authoritative mapping always comes from the relinker itself.
     */
    private val KNOWN_LIVE_KEY_CONT = mapOf(
        "598308" to "rai1",
        "598310" to "rai2",
        "598326" to "rainews24",
    )

    private val KNOWN_SLUG_CONT = mapOf(
        "raiuno1" to "rai1",
        "raidue1" to "rai2",
        "rainews1" to "rainews24",
    )

    /**
     * The host prefix of the channels, they are the same for both content delivery
     * networks: `raiuno1-live.akamaized.net` and `raiuno1-push.cdn.netrw.it`.
     */
    private val KNOWN_HOST_CONT = mapOf(
        "raiuno1" to "rai1",
        "raidue1" to "rai2",
        "raitre1" to "rai3",
        "raiquattro1" to "rai4",
        "raicinque1" to "rai5",
        "raimovie1" to "raimovie",
        "raipremium1" to "raipremium",
        "raigulp1" to "raigulp",
        "raiyoyo1" to "raiyoyo",
        "rainews1" to "rainews24",
        "raistoria1" to "raistoria",
        "raiscuola1" to "raiscuola",
        "raisport1" to "raisport",
        "raivisualradio1" to "rairadio2visual",
    )

    val channels: List<RaiChannel> = listOf(
        channel("rai1", "Rai 1", "2606803", "raiuno", "raione"),
        channel("rai2", "Rai 2", "308718", "raidue"),
        channel("rai3", "Rai 3", "308709", "raitre"),
        channel("rai4", "Rai 4", "746966", "raiquattro"),
        channel("rai5", "Rai 5", "395276", "raicinque"),
        channel("raimovie", "Rai Movie", "747002", "raimovie1"),
        channel("raipremium", "Rai Premium", "746992", "raipremiumhd"),
        channel("raigulp", "Rai Gulp", "746953", "raigulp1"),
        channel("raiyoyo", "Rai Yoyo", "746899", "raiyoyo1"),
        channel("rainews24", "Rai News 24", "1", "rainews24hd"),
        channel("raistoria", "Rai Storia", "746990", "raistoria1"),
        channel("raiscuola", "Rai Scuola", "747011", "raiscuola1"),
        channel("raisport", "Rai Sport+ HD", "358025", "raisport", "raisportplus", "raisporthd"),
        channel("rairadio2visual", "Rai Radio 2 Visual", "5674080", "rairadio2", "rairadio2visual1"),
    )

    private val channelsByCont: Map<String, RaiChannel> = channels.associateBy { it.cont }

    private val channelsByKey: Map<String, RaiChannel> = channels.associateBy { it.key }

    private val channelsByAlias: Map<String, RaiChannel> = channels
        .flatMap { channel -> channel.aliases.map { alias -> alias to channel } }
        .toMap()

    fun byCont(cont: String): RaiChannel? = channelsByCont[cont]

    /**
     * The channel matching a playlist title, null when the title is not a Rai channel.
     */
    fun byTitle(title: String): RaiChannel? {
        val normalized = title.normalizedRaiName()
        if (normalized.isEmpty()) return null
        channelsByAlias[normalized]?.let { return it }
        val candidates = channelsByAlias.keys.filter { normalized.startsWith(it) }
        val longest = candidates.maxByOrNull { it.length } ?: return null
        if (candidates.count { it.length == longest.length } > 1) return null
        return channelsByAlias[longest]
    }

    /**
     * The relinker url of a channel.
     */
    fun relinkerUrl(cont: String): String =
        "https://$RELINKER_HOST$RELINKER_PATH?cont=$cont&$RELINKER_QUERY"

    /**
     * The channel an already resolved live url belongs to, null when it is unknown.
     *
     * The mapping learned from the previous resolutions wins over the static one, the
     * static one only covers the channels whose live path is publicly known.
     */
    fun byResolvedUrl(
        liveKey: String?,
        slug: String?,
        hostPrefix: String?,
    ): RaiChannel? {
        val key = liveKey?.let { KNOWN_LIVE_KEY_CONT[it] }
            ?: slug?.lowercase()?.let { KNOWN_SLUG_CONT[it] }
            ?: hostPrefix?.lowercase()?.let { KNOWN_HOST_CONT[it] }
            ?: return null
        return channelsByKey[key]
    }

    private fun channel(
        key: String,
        title: String,
        cont: String,
        vararg aliases: String,
    ): RaiChannel = RaiChannel(
        key = key,
        title = title,
        cont = cont,
        aliases = (listOf(title) + aliases)
            .map { it.normalizedRaiName() }
            .filter { it.isNotEmpty() }
            .distinct(),
    )
}

/**
 * The comparable form of a channel name: lower case and without any separator.
 */
internal fun String.normalizedRaiName(): String =
    lowercase()
        .filter { it.isLetterOrDigit() }
