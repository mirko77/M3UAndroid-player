package com.m3u.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionWorkerSchedulingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val workManager = WorkManager.getInstance(context)
    private val uniqueWorkNames = mutableSetOf<String>()

    @After
    fun cancelScheduledWork() {
        uniqueWorkNames.forEach { uniqueWorkName ->
            workManager.cancelUniqueWork(uniqueWorkName).result.get(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun localM3uNeedsNoNetworkWhileRemoteM3uDoes() {
        val nonce = UUID.randomUUID()
        val localUrl = "content://com.m3u.test/playlists/$nonce"
        val remoteUrl = "https://reader:private-token@example.test/$nonce/list.m3u"
        val localWorkName = m3uSubscriptionWorkName(localUrl)
        val remoteWorkName = m3uSubscriptionWorkName(remoteUrl)
        uniqueWorkNames += localWorkName
        uniqueWorkNames += remoteWorkName

        val localWorkId = SubscriptionWorker.m3u(
            workManager = workManager,
            title = "Local",
            url = localUrl,
            requireExistingPlaylist = false,
        )
        val remoteWorkId = SubscriptionWorker.m3u(
            workManager = workManager,
            title = "Remote",
            url = remoteUrl,
            requireExistingPlaylist = true,
        )

        val local = workSpecFor(localWorkName)
        val remote = workSpecFor(remoteWorkName)

        assertEquals(localWorkId.toString(), local.id)
        assertEquals(remoteWorkId.toString(), remote.id)
        assertEquals(NetworkType.NOT_REQUIRED, local.constraints.requiredNetworkType)
        assertEquals(NetworkType.CONNECTED, remote.constraints.requiredNetworkType)
        assertFalse(local.input.getBoolean(REQUIRE_EXISTING_INPUT, true))
        assertTrue(remote.input.getBoolean(REQUIRE_EXISTING_INPUT, false))
        assertSafeIdentity(localWorkName, localUrl, "playlists", nonce.toString())
        assertSafeIdentity(
            remoteWorkName,
            remoteUrl,
            "reader",
            "private-token",
            nonce.toString(),
        )
        assertSafeTags(local, localUrl, nonce.toString())
        assertSafeTags(
            remote,
            remoteUrl,
            "reader",
            "private-token",
            nonce.toString(),
        )
        assertEquals(emptyList<WorkInfo>(), workInfosForUniqueWork(localUrl))
        assertEquals(emptyList<WorkInfo>(), workInfosForUniqueWork(remoteUrl))
    }

    @Test
    fun replacementIdentityIsStableButDifferentSourcesCannotCollide() {
        val value = "https://example.test/private/list.m3u?token=secret"

        assertEquals(
            m3uSubscriptionWorkName(value),
            m3uSubscriptionWorkName(value),
        )
        assertNotEquals(
            m3uSubscriptionWorkName(value),
            epgSubscriptionWorkName(value),
        )
    }

    private fun workSpecFor(uniqueWorkName: String): WorkSpec {
        val info = workInfosForUniqueWork(uniqueWorkName).single()
        val implementation = workManager as WorkManagerImpl
        return checkNotNull(
            implementation.workDatabase.workSpecDao().getWorkSpec(info.id.toString())
        )
    }

    private fun workInfosForUniqueWork(uniqueWorkName: String): List<WorkInfo> =
        workManager
            .getWorkInfosForUniqueWork(uniqueWorkName)
            .get(5, TimeUnit.SECONDS)

    private fun assertSafeTags(
        workSpec: WorkSpec,
        vararg sensitiveValues: String,
    ) {
        val info = workManager
            .getWorkInfoById(UUID.fromString(workSpec.id))
            .get(5, TimeUnit.SECONDS)
        checkNotNull(info)
        info.tags.forEach { tag ->
            sensitiveValues.forEach { sensitive ->
                assertFalse(
                    "WorkManager tag leaked sensitive input: $tag",
                    tag.contains(sensitive, ignoreCase = true),
                )
            }
        }
    }

    private fun assertSafeIdentity(
        identity: String,
        vararg sensitiveValues: String,
    ) {
        assertTrue(identity.matches(SAFE_WORK_IDENTITY))
        sensitiveValues.forEach { sensitive ->
            assertFalse(
                "Unique work identity leaked sensitive input: $identity",
                identity.contains(sensitive, ignoreCase = true),
            )
        }
    }

    private companion object {
        const val REQUIRE_EXISTING_INPUT = "require-existing-playlist"
        const val EPG_IGNORE_CACHE_INPUT = "ignore_cache"
        val SAFE_WORK_IDENTITY = Regex("[a-z0-9-]+:[0-9a-f]{64}")
    }
}
