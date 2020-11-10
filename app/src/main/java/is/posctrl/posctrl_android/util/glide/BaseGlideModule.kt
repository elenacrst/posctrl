package `is`.posctrl.posctrl_android.util.glide

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Glide module applies specified options for all load() calls
 */
@GlideModule
class BaseGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val memoryCacheSizeBytes = 1024 * 1024 * 12L // 12mb
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, memoryCacheSizeBytes))
        builder.setDefaultRequestOptions(RequestOptions())
    }
}