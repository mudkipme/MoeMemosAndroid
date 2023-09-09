package me.mudkip.moememos.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import com.skydoves.sandwich.suspendOnSuccess
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.MemosUserSettingKey
import me.mudkip.moememos.data.model.Status
import me.mudkip.moememos.ext.DataStoreKeys
import me.mudkip.moememos.ext.dataStore
import net.swiftzer.semver.SemVer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemosApiService @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private var memosApi: MemosApi? = null
    var host: String? = null
        private set
    var status: Status? = null
        private set
    var client: OkHttpClient = okHttpClient
        private set

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun loadStatus() = scope.launch {
        memosApi?.status()?.suspendOnSuccess {
            status = data
        }
    }

    init {
        runBlocking {
            context.dataStore.data.first().let {
                val host = it[DataStoreKeys.Host.key]
                val openId = it[DataStoreKeys.OpenId.key]
                if (host != null && host.isNotEmpty()) {
                    val (client, memosApi) = createClient(host, openId)
                    this@MemosApiService.client = client
                    this@MemosApiService.memosApi = memosApi
                    this@MemosApiService.host = host
                }
            }
        }
        loadStatus()
    }

    suspend fun update(host: String, openId: String?) {
        context.dataStore.edit {
            it[DataStoreKeys.Host.key] = host
            if (!openId.isNullOrEmpty()) {
                it[DataStoreKeys.OpenId.key] = openId
            } else {
                it.remove(DataStoreKeys.OpenId.key)
            }
        }

        mutex.withLock {
            val (client, memosApi) = createClient(host, openId)
            this.client = client
            this.memosApi = memosApi
            this.host = host
        }
        loadStatus()
    }

    fun createClient(host: String, openId: String?): Pair<OkHttpClient, MemosApi> {
        var client = okHttpClient
        if (!openId.isNullOrEmpty()) {
            client = client.newBuilder().addNetworkInterceptor { chain ->
                var request = chain.request()
                val url = request.url.newBuilder().addQueryParameter("openId", openId).build()
                request = request.newBuilder().url(url).build()
                chain.proceed(request)
            }.build()
        }

        return client to Retrofit.Builder()
            .baseUrl(host)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(
                Moshi.Builder()
                    .add(MemosUserSettingKey::class.java, EnumJsonAdapter.create(MemosUserSettingKey::class.java)
                        .withUnknownFallback(MemosUserSettingKey.UNKNOWN))
                    .add(KotlinJsonAdapterFactory())
                    .build()
            ))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosApi::class.java)
    }

    suspend fun <T>call(block: suspend (MemosApi) -> ApiResponse<T>): ApiResponse<T> {
        return memosApi?.let { block(it) } ?: ApiResponse.error(MoeMemosException.notLogin)
    }

    fun versionCompare(target: String): Boolean {
        val version = status?.profile?.version

        return version?.let {
            SemVer.parse(it) >= SemVer.parse(target)
        } ?: false
    }
}