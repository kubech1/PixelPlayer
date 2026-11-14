package com.theveloper.pixelplay.data.image

import android.net.Uri
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.request.Options
import com.theveloper.pixelplay.data.navidrome.NavidromeRepository
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

/**
 * Custom Coil Fetcher for Navidrome album art.
 * Handles URIs in format: navidrome_cover://coverArtId
 */
class NavidromeCoilFetcher(
    uri: Uri,
    private val repository: NavidromeRepository,
    okHttpClient: OkHttpClient,
    cacheDir: File
) : BaseRemoteCoverFetcher(uri, okHttpClient, cacheDir) {

    override val tag: String = "NavidromeCoilFetcher"
    override val cachePrefix: String = "navidrome_cover"
    override val isLoggedIn: Boolean get() = repository.isLoggedIn

    override fun resolveImageUrl(id: String, sizePx: Int): String? =
        repository.getCoverArtUrl(id, sizePx)

    /**
     * Factory for creating NavidromeCoilFetcher instances.
     * Registered with Coil's ImageLoader to handle navidrome_cover:// URIs.
     */
    class Factory @Inject constructor(
        private val repository: NavidromeRepository,
        private val okHttpClient: OkHttpClient
    ) : Fetcher.Factory<Uri> {

        private var cacheDir: File? = null

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme == "navidrome_cover") {
                val cache = cacheDir ?: options.context.cacheDir.also { cacheDir = it }
                NavidromeCoilFetcher(data, repository, okHttpClient, cache)
            } else {
                null
            }
        }
    }
}
