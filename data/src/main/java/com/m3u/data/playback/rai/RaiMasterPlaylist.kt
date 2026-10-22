package com.m3u.data.playback.rai

/**
 * A video variant declared by a Rai master playlist.
 */
data class RaiVariant(
    val uri: String,
    val width: Int,
    val height: Int,
    val bandwidth: Long,
) {
    val resolution: String?
        get() = if (width > 0 && height > 0) "${width}x$height" else null
}

/**
 * Reads the video variants of a Rai master playlist.
 *
 * The master playlist of a Rai channel also declares alternate audio renditions and
 * I-frame playlists which the CDN does not always serve. They are ignored on purpose:
 * the variant playlists carry the muxed audio and video of the channel.
 */
object RaiMasterPlaylist {
    private val PREFERRED_RESOLUTIONS = listOf(1920 to 1080, 1280 to 720)

    /**
     * The video variants of a master playlist, in the order they are declared.
     */
    fun parse(content: String): List<RaiVariant> {
        val variants = mutableListOf<RaiVariant>()
        var attributes: Map<String, String>? = null
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith(STREAM_INF) -> {
                    attributes = parseAttributes(line.removePrefix(STREAM_INF))
                }

                line.isEmpty() || line.startsWith("#") -> Unit

                else -> {
                    val declared = attributes
                    attributes = null
                    if (declared != null) {
                        val resolution = declared["RESOLUTION"]?.let { parseResolution(it) }
                        variants += RaiVariant(
                            uri = line,
                            width = resolution?.first ?: 0,
                            height = resolution?.second ?: 0,
                            bandwidth = declared["BANDWIDTH"]?.toLongOrNull() ?: 0L,
                        )
                    }
                }
            }
        }
        return variants
    }

    /**
     * The variants ordered by preference, the best one first.
     */
    fun orderedVariants(variants: List<RaiVariant>): List<RaiVariant> {
        val preferred = PREFERRED_RESOLUTIONS.mapNotNull { (width, height) ->
            variants.firstOrNull { it.width == width && it.height == height }
        }
        val rest = variants
            .filterNot { variant -> preferred.any { it.uri == variant.uri } }
            .sortedWith(
                compareByDescending<RaiVariant> { it.height }
                    .thenByDescending { it.width }
                    .thenByDescending { it.bandwidth }
            )
        return preferred + rest
    }

    private fun parseAttributes(value: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        var index = 0
        while (index < value.length) {
            val separator = value.indexOf('=', index)
            if (separator == -1) break
            val key = value.substring(index, separator).trim()
            index = separator + 1
            val builder = StringBuilder()
            if (index < value.length && value[index] == '"') {
                index++
                while (index < value.length && value[index] != '"') {
                    builder.append(value[index])
                    index++
                }
                if (index < value.length) index++
            } else {
                while (index < value.length && value[index] != ',') {
                    builder.append(value[index])
                    index++
                }
            }
            attributes[key] = builder.toString()
            while (index < value.length && value[index] != ',') index++
            if (index < value.length) index++
        }
        return attributes
    }

    private fun parseResolution(value: String): Pair<Int, Int>? {
        val separator = value.indexOf('x')
        if (separator == -1) return null
        val width = value.substring(0, separator).trim().toIntOrNull() ?: return null
        val height = value.substring(separator + 1).trim().toIntOrNull() ?: return null
        return width to height
    }

    private const val STREAM_INF = "#EXT-X-STREAM-INF:"
}
