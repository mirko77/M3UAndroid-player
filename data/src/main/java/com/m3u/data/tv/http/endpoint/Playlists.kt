package com.m3u.data.tv.http.endpoint

import androidx.work.WorkManager
import com.m3u.data.database.model.DataSource
import com.m3u.data.repository.playlist.PlaylistRepository
import com.m3u.data.worker.SubscriptionWorker
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class Playlists @Inject constructor(
    private val workManager: WorkManager,
    private val playlistRepository: PlaylistRepository
) : Endpoint {
    override fun apply(route: Route) {
        route.route("/playlists") {
            post("subscribe") {
                val dataSourceValue = call.queryParameters["data_source"]
                val dataSource = dataSourceValue?.let(DataSource::ofOrNull)
                if (dataSource == null) {
                    call.respond(
                        DefRep(
                            result = false,
                            reason = "DataSource $dataSourceValue is unsupported."
                        )
                    )
                    return@post
                }

                val title = call.queryParameters["title"]
                val url = call.queryParameters["url"]
                val epg = call.queryParameters["epg"]

                when (dataSource) {
                    DataSource.M3U -> {
                        if (title == null || url == null) {
                            call.respond(DefRep(false, "Both title and url are required."))
                            return@post
                        }
                        SubscriptionWorker.m3u(workManager, title, url)
                    }

                    DataSource.EPG -> {
                        if (title == null || epg == null) {
                            call.respond(DefRep(false, "Both title and epg link are required."))
                            return@post
                        }
                        playlistRepository.insertEpgAsPlaylist(title, epg)
                    }

                    else -> {
                        call.respond(DefRep(false, "DataSource $dataSource is unsupported."))
                        return@post
                    }
                }
                call.respond(DefRep(result = true))
            }
        }
    }
}
