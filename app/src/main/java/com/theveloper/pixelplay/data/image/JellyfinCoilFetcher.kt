package com.theveloper.pixelplay.data.image

import android.net.Uri
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.request.Options
import com.theveloper.pixelplay.data.jellyfin.JellyfinRepository
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

class JellyfinCoilFetcher(
    uri: Uri,
    private val repository: JellyfinRepository,
    okHttpClient: OkHttpClient,
    cacheDir: File
) : BaseRemoteCoverFetcher(uri, okHttpClient, cacheDir) {

    override val tag: String = "JellyfinCoilFetcher"
    override val cachePrefix: String = "jellyfin_cover"
    override val isLoggedIn: Boolean get() = repository.isLoggedIn

    override fun resolveImageUrl(id: String, sizePx: Int): String? =
        repository.getImageUrl(id, sizePx)

    override fun resolveAuthorizationHeader(): String? =
        repository.getAuthorizationHeader()

    class Factory @Inject constructor(
        private val repository: JellyfinRepository,
        private val okHttpClient: OkHttpClient
    ) : Fetcher.Factory<Uri> {

        private var cacheDir: File? = null

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme == "jellyfin_cover") {
                val cache = cacheDir ?: options.context.cacheDir.also { cacheDir = it }
                JellyfinCoilFetcher(data, repository, okHttpClient, cache)
            } else {
                null
            }
        }
    }
}
