package me.mudkip.moememos.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.skydoves.sandwich.suspendOnSuccess
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.mudkip.moememos.data.api.MemosV0Api
import me.mudkip.moememos.data.api.MemosV0UserSettingKey
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.local.MoeMemosDatabase
import me.mudkip.moememos.data.local.UserPreferences
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.UserData
import me.mudkip.moememos.data.repository.AbstractMemoRepository
import me.mudkip.moememos.data.repository.LocalDatabaseRepository
import me.mudkip.moememos.data.repository.MemosV0Repository
import me.mudkip.moememos.data.repository.MemosV1Repository
import me.mudkip.moememos.ext.settingsDataStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountService @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val database: MoeMemosDatabase,
    private val fileStorage: FileStorage,
    private val userPreferences: UserPreferences
) {
    var httpClient: OkHttpClient = okHttpClient
        private set
    val accounts = context.settingsDataStore.data.map { settings ->
        settings.usersList.mapNotNull { Account.parseUserData(it) }
    }
    val currentAccount = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }
            ?.let { Account.parseUserData(it) }
    }
    private var repository: AbstractMemoRepository = LocalDatabaseRepository(
        database.memoDao(),
        fileStorage,
        userPreferences
    )

    private val mutex = Mutex()

    init {
        runBlocking {
            updateCurrentAccount(currentAccount.first())
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }

    private fun updateCurrentAccount(account: Account?) {
        if (!isOnline()) {
            // Use LocalDatabaseRepository but maintain the current account
            this.repository = LocalDatabaseRepository(
                database.memoDao(),
                fileStorage,
                userPreferences,
                account  // Pass the current account instead of null
            )
            httpClient = okHttpClient
            return
        }

        when (account) {
            null -> {
                this.repository = LocalDatabaseRepository(database.memoDao(), fileStorage, userPreferences, null)
                httpClient = okHttpClient
            }
            is Account.MemosV0 -> {
                val (client, memosApi) = createMemosV0Client(
                    account.info.host,
                    account.info.accessToken
                )
                this.repository = MemosV0Repository(memosApi, account)
                this.httpClient = client
            }
            is Account.MemosV1 -> {
                val (client, memosApi) = createMemosV1Client(
                    account.info.host,
                    account.info.accessToken
                )
                this.repository = MemosV1Repository(memosApi, account)
                this.httpClient = client
            }
            Account.Local -> {
                this.repository = LocalDatabaseRepository(database.memoDao(), fileStorage, userPreferences, account)
                httpClient = okHttpClient
            }
        }
    }

    suspend fun switchAccount(accountKey: String) {
        mutex.withLock {
            val account = accounts.first().firstOrNull { it.accountKey() == accountKey }
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
            context.settingsDataStore.updateData { settings ->
                var builder = settings.toBuilder()
                val index =
                    settings.usersList.indexOfFirst { it.accountKey == account.accountKey() }
                if (index != -1) {
                    builder = builder.removeUsers(index)
                }
                builder.addUsers(account.toUserData()).setCurrentUser(account.accountKey()).build()
            }
            updateCurrentAccount(account)
        }
    }

    suspend fun removeAccount(accountKey: String) {
        mutex.withLock {
            context.settingsDataStore.updateData { settings ->
                var builder = settings.toBuilder()
                val index = settings.usersList.indexOfFirst { it.accountKey == accountKey }
                if (index != -1) {
                    builder = builder.removeUsers(index)
                }
                if (settings.currentUser == accountKey) {
                    builder =
                        builder.setCurrentUser(accounts.first().firstOrNull()?.accountKey() ?: "")
                }
                builder.build()
            }
            updateCurrentAccount(currentAccount.first())
        }
    }

    fun createMemosV0Client(host: String, accessToken: String?): Pair<OkHttpClient, MemosV0Api> {
        var client = okHttpClient

        if (!accessToken.isNullOrEmpty()) {
            client = client.newBuilder().addNetworkInterceptor { chain ->
                var request = chain.request()
                if (request.url.host == host.toHttpUrlOrNull()?.host) {
                    request = request.newBuilder().addHeader("Authorization", "Bearer $accessToken")
                        .build()
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
                            MemosV0UserSettingKey::class.java,
                            EnumJsonAdapter.create(MemosV0UserSettingKey::class.java)
                                .withUnknownFallback(MemosV0UserSettingKey.UNKNOWN)
                        )
                        .add(KotlinJsonAdapterFactory())
                        .build()
                )
            )
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosV0Api::class.java)
    }

    fun createMemosV1Client(host: String, accessToken: String?): Pair<OkHttpClient, MemosV1Api> {
        val client = httpClient.newBuilder().apply {
            if (accessToken != null) {
                addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()
                    chain.proceed(request)
                }
            }
        }.build()

        return client to Retrofit.Builder()
            .baseUrl(host)
            .client(client)
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                        .add(Instant::class.java, Rfc3339DateJsonAdapter().nullSafe())
                        .build()
                )
            )
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosV1Api::class.java)
    }

    suspend fun detectAccountCase(host: String): UserData.AccountCase {
        val memosV0Status = createMemosV0Client(host, null).second.status().getOrNull()
        if (!memosV0Status?.profile?.version.isNullOrEmpty()) {
            return UserData.AccountCase.MEMOS_V0
        }
        val memosV1Profile = createMemosV1Client(host, null).second.getProfile().getOrThrow()
        if (memosV1Profile.version.isNotEmpty()) {
            return UserData.AccountCase.MEMOS_V1
        }
        return UserData.AccountCase.ACCOUNT_NOT_SET
    }

    suspend fun getRepository(): AbstractMemoRepository {
        mutex.withLock {
            return repository
        }
    }

    fun getLocalRepository(): AbstractMemoRepository {
        return LocalDatabaseRepository(
            database.memoDao(),
            fileStorage,
            userPreferences
        )
    }

    suspend fun syncMemos() {
        val localRepository = getLocalRepository() as LocalDatabaseRepository
        val onlineRepository = getRepository()
        
        // First handle local memos that need syncing
        val localMemos = localRepository.getUnsyncedMemos()
        val syncedIdentifiers = mutableSetOf<String>() // Track synced memo identifiers
        
        for (localMemo in localMemos) {
            if (localMemo.isDeleted) {
                onlineRepository.deleteMemo(localMemo.identifier).suspendOnSuccess {
                    localRepository.permanentlyDeleteMemo(localMemo.identifier)
                }
            } else {
                // New local memo - create online
                onlineRepository.createMemo(
                    content = localMemo.content,
                    visibility = localMemo.visibility,
                    resources = emptyList(),
                    tags = null
                ).suspendOnSuccess {
                    // Delete the local memo with temporary UUID
                    localRepository.permanentlyDeleteMemo(localMemo.identifier)
                    // Create new local memo with server ID
                    localRepository.storeSyncedMemos(listOf(data))
                    syncedIdentifiers.add(data.identifier)
                }
            }
        }
        
        // Then sync online memos to local, excluding ones we just synced
        onlineRepository.listMemos().suspendOnSuccess { 
            val filteredMemos = data.filterNot { memo ->
                syncedIdentifiers.contains(memo.identifier)
            }
            localRepository.storeSyncedMemos(filteredMemos)
        }
    }
}