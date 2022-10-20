package me.mudkip.moememos.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.ext.DataStoreKeys
import me.mudkip.moememos.ext.dataStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemosApiService @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    var memosApi: MemosApi? = null
        private set
    var host: String? = null
        private set
    private val mutex = Mutex()

    init {
        runBlocking {
            context.dataStore.data.map { it[DataStoreKeys.Host.key] }.first()?.let {
                memosApi = createClient(it)
                host = it
            }
        }
    }

    suspend fun update(host: String) {
        context.dataStore.edit {
            it[DataStoreKeys.Host.key] = host
        }

        mutex.withLock {
            memosApi = createClient(host)
            this.host = host
        }
    }

    fun createClient(host: String): MemosApi {
        return Retrofit.Builder()
            .baseUrl(host)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosApi::class.java)
    }
}