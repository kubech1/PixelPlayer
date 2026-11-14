package com.theveloper.pixelplay.data.image

import android.net.Uri
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.FileSystem
import okio.Path.Companion.toPath
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared Coil [Fetcher] base for remote cover art URIs that resolve to an HTTP URL
 * and cache the downloaded image on disk.
 */
abstract class BaseRemoteCoverFetcher(
    protected val uri: Uri,
    private val okHttpClient: OkHttpClient,
    private val cacheDir: File
) : Fetcher {

    protected abstract val tag: String
    protected abstract val cachePrefix: String
    protected abstract val isLoggedIn: Boolean

    protected abstract fun resolveImageUrl(id: String, sizePx: Int): String?
    protected open fun resolveAuthorizationHeader(): String? = null

    override suspend fun fetch(): FetchResult? {
        Timber.v("$tag: Fetching $uri")

        val itemId = uri.host ?: uri.path?.removePrefix("/")
        if (itemId.isNullOrBlank()) {
            Timber.w("$tag: Invalid URI format: $uri")
            return null
        }

        if (!isLoggedIn) {
            Timber.v("$tag: Not logged in, skipping fetch")
            return null
        }

        val sizeParam = uri.getQueryParameter("size")?.toIntOrNull() ?: 500
        val cachedFile = File(cacheDir, "${cachePrefix}_${itemId}_$sizeParam.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            Timber.v("$tag: Using cached cover for $itemId")
            return SourceResult(
                source = ImageSource(
                    file = cachedFile.absolutePath.toPath(),
                    fileSystem = FileSystem.SYSTEM
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }

        val imageUrl = resolveImageUrl(itemId, sizeParam)
        if (imageUrl.isNullOrBlank()) {
            if (shouldLogFailure("${tag}_no_url_$itemId")) {
                Timber.w("$tag: No cover art URL for $itemId")
            }
            return null
        }

        val authHeader = resolveAuthorizationHeader()
        return try {
            downloadImage(imageUrl, cachedFile, authHeader)
        } catch (e: Exception) {
            if (shouldLogFailure("${tag}_download_$itemId")) {
                Timber.w(e, "$tag: Failed to download cover art for $itemId")
            }
            null
        }
    }

    private fun downloadImage(url: String, cacheFile: File, authHeader: String?): FetchResult? {
        val request = Request.Builder()
            .url(url)
            .apply { if (authHeader != null) header("Authorization", authHeader) }
            .get()
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("$tag: HTTP ${response.code} for $url")
                    return null
                }

                val bytes = response.body.bytes()
                if (bytes.isEmpty()) {
                    Timber.w("$tag: Empty response body for $url")
                    return null
                }

                FileOutputStream(cacheFile).use { fos ->
                    fos.write(bytes)
                }

                Timber.v("$tag: Cached cover art (${bytes.size} bytes)")

                SourceResult(
                    source = ImageSource(
                        file = cacheFile.absolutePath.toPath(),
                        fileSystem = FileSystem.SYSTEM
                    ),
                    mimeType = response.header("Content-Type") ?: "image/jpeg",
                    dataSource = DataSource.NETWORK
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "$tag: Failed to download image from $url")
            null
        }
    }

    companion object {
        private val recentlyLoggedFailures = ConcurrentHashMap<String, Long>()
        private const val LOG_FAILURE_COOLDOWN_MS = 60_000L

        private fun shouldLogFailure(key: String): Boolean {
            val now = System.currentTimeMillis()
            val lastLogged = recentlyLoggedFailures[key]
            return if (lastLogged == null || now - lastLogged > LOG_FAILURE_COOLDOWN_MS) {
                recentlyLoggedFailures[key] = now
                if (recentlyLoggedFailures.size > 100) {
                    recentlyLoggedFailures.entries.removeIf { now - it.value > LOG_FAILURE_COOLDOWN_MS }
                }
                true
            } else {
                false
            }
        }
    }
}
