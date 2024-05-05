package me.mudkip.moememos.data.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.data.api.MemosApi
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.MemosUserSettingKey
import me.mudkip.moememos.ext.settingsDataStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountService @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    var currentAccount: Account? by mutableStateOf(null)
        private set
    private var memosApi: MemosApi? = null
    var httpClient: OkHttpClient = okHttpClient
        private set
    private var accounts: List<Account> = emptyList()
    private val mutex = Mutex()

    init {
        runBlocking {
            // Load account from data store
            val settings = context.settingsDataStore.data.first()
            accounts = settings.usersList.mapNotNull {
                Account.parseUserData(it)
            }

            val currentAccount = accounts.firstOrNull { it.accountKey() == settings.currentUser } ?: accounts.firstOrNull()
            updateCurrentAccount(currentAccount)
        }
    }

    private fun updateCurrentAccount(account: Account?) {
        currentAccount = account
        when (account) {
            is Account.Memos -> {
                val memosAccount = account.info
                val (client, memosApi) = createMemosClient(memosAccount.host, memosAccount.accessToken)
                this.memosApi = memosApi
                this.httpClient = client
            }
            else -> {
                memosApi = null
                httpClient = okHttpClient
            }
        }
    }

    suspend fun switchAccount(accountKey: String) {
        mutex.withLock {
            val account = accounts.firstOrNull { it.accountKey() == accountKey }
            context.settingsDataStore.updateData { settings ->
                settings.toBuilder().apply {
                    this.currentUser = accountKey
                }.build()
            }
            updateCurrentAccount(account)
        }
    }

    suspend fun addAccount(account: Account) {
        mutex.withLock {
            accounts = accounts.filter { it.accountKey() != account.accountKey() }
            accounts = accounts + account
            context.settingsDataStore.updateData { settings ->
                var builder = settings.toBuilder()
                val index = settings.usersList.indexOfFirst { it.accountKey == account.accountKey() }
                if (index != -1) {
                    builder = builder.removeUsers(index)
                }
                builder.addUsers(account.toUserData()).setCurrentUser(account.accountKey()).build()
            }
            updateCurrentAccount(account)
        }
    }

    suspend fun removeAccount(account: Account) {
        mutex.withLock {
            accounts = accounts.filter { it.accountKey() != account.accountKey() }
            context.settingsDataStore.updateData { settings ->
                var builder = settings.toBuilder()
                val index = settings.usersList.indexOfFirst { it.accountKey == account.accountKey() }
                if (index != -1) {
                    builder = builder.removeUsers(index)
                }
                if (settings.currentUser == account.accountKey()) {
                    builder = builder.setCurrentUser(accounts.firstOrNull()?.accountKey() ?: "")
                }
                builder.build()
            }
            if (currentAccount == account) {
                updateCurrentAccount(accounts.firstOrNull())
            }
        }
    }

    fun createMemosClient(host: String, accessToken: String?): Pair<OkHttpClient, MemosApi> {
        var client = okHttpClient

        if (!accessToken.isNullOrEmpty()) {
            client = client.newBuilder().addNetworkInterceptor { chain ->
                var request = chain.request()
                if (request.url.host == host.toHttpUrlOrNull()?.host) {
                    try {
                        request = request.newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
                    } catch (e: Throwable) {
                        Timber.e(e)
                    }
                }
                chain.proceed(request)
            }.build()
        }

        return client to Retrofit.Builder()
            .baseUrl(host)
            .client(client)
            .addConverterFactory(
                MoshiConverterFactory.create(
                Moshi.Builder()
                    .add(
                        MemosUserSettingKey::class.java, EnumJsonAdapter.create(MemosUserSettingKey::class.java)
                        .withUnknownFallback(MemosUserSettingKey.UNKNOWN))
                    .add(KotlinJsonAdapterFactory())
                    .build()
            ))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosApi::class.java)
    }

    suspend fun <T>memosCall(block: suspend (MemosApi) -> ApiResponse<T>): ApiResponse<T> {
        return memosApi?.let { block(it) } ?: ApiResponse.exception(MoeMemosException.notLogin)
    }
}