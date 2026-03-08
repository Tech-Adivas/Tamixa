package com.araro.android.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Application-level audio cache for ExoPlayer. Caches streamed audio to disk
 * so previously played stories can be played offline.
 */
object AudioCache {

    private var cache: SimpleCache? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        if (cacheDataSourceFactory == null) {
            val cacheDir = File(context.cacheDir, "araro_audio_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024L) // 100 MB
            val dbProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, dbProvider)

            val httpFactory = DefaultHttpDataSource.Factory()
            val defaultFactory = DefaultDataSource.Factory(context, httpFactory)
            cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache!!)
                .setUpstreamDataSourceFactory(defaultFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
        return cacheDataSourceFactory!!
    }
}
