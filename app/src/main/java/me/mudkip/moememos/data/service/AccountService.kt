package me.mudkip.moememos.data.service

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import me.mudkip.moememos.R
import me.mudkip.moememos.data.api.MemosV0Api
import me.mudkip.moememos.data.api.MemosV1Api
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.MoeMemosDatabase
import me.mudkip.moememos.data.local.entity.ResourceEntity
import me.mudkip.moememos.data.model.Account
import me.mudkip.moememos.data.model.LocalAccount
import me.mudkip.moememos.data.model.User
import me.mudkip.moememos.data.model.UserData
import me.mudkip.moememos.data.model.UserSettings
import me.mudkip.moememos.data.repository.AbstractMemoRepository
import me.mudkip.moememos.data.repository.LocalDatabaseRepository
import me.mudkip.moememos.data.repository.MemosV0Repository
import me.mudkip.moememos.data.repository.MemosV1Repository
import me.mudkip.moememos.data.repository.RemoteRepository
import me.mudkip.moememos.data.repository.SyncingRepository
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ext.settingsDataStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val database: MoeMemosDatabase,
    private val fileStorage: FileStorage,
) {
    sealed class LoginCompatibility {
        data class Supported(val accountCase: UserData.AccountCase) : LoginCompatibility()
        data class Unsupported(val message: String) : LoginCompatibility()
        data class RequiresConfirmation(
            val accountCase: UserData.AccountCase,
            val version: String,
            val message: String,
        ) : LoginCompatibility()
    }

    sealed class SyncCompatibility {
        object Allowed : SyncCompatibility()
        data class Blocked(val message: String?) : SyncCompatibility()
        data class RequiresConfirmation(val version: String, val message: String) : SyncCompatibility()
    }

    private data class ServerVersionInfo(
        val accountCase: UserData.AccountCase,
        val version: String,
    )

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<SemanticVersion> {
        override fun compareTo(other: SemanticVersion): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            return patch.compareTo(other.patch)
        }
    }

    private enum class VersionPolicy {
        SUPPORTED,
        TOO_LOW,
        V1_HIGHER,
    }

    private val exportDateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss", Locale.US)
        .withZone(ZoneId.systemDefault())

    private val networkJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

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
        Account.Local(LocalAccount())
    )

    private var remoteRepository: RemoteRepository? = null

    private val mutex = Mutex()

    init {
        runBlocking {
            updateCurrentAccount(currentAccount.first())
        }
    }

    private fun updateCurrentAccount(account: Account?) {
        repository.close()
        when (account) {
            null -> {
                this.repository = LocalDatabaseRepository(database.memoDao(), fileStorage, Account.Local(LocalAccount()))
                this.remoteRepository = null
                httpClient = okHttpClient
            }
            is Account.Local -> {
                this.repository = LocalDatabaseRepository(database.memoDao(), fileStorage, account)
                this.remoteRepository = null
                httpClient = okHttpClient
            }
            is Account.MemosV0 -> {
                val (client, memosApi) = createMemosV0Client(account.info.host, account.info.accessToken)
                val remote = MemosV0Repository(memosApi, account)
                this.repository = SyncingRepository(
                    database.memoDao(),
                    fileStorage,
                    remote,
                    account
                ) { user ->
                    updateAccountFromSyncedUser(account.accountKey(), user)
                }
                this.remoteRepository = remote
                this.httpClient = client
            }
            is Account.MemosV1 -> {
                val (client, memosApi) = createMemosV1Client(account.info.host, account.info.accessToken)
                val remote = MemosV1Repository(memosApi, account)
                this.repository = SyncingRepository(
                    database.memoDao(),
                    fileStorage,
                    remote,
                    account
                ) { user ->
                    updateAccountFromSyncedUser(account.accountKey(), user)
                }
                this.remoteRepository = remote
                this.httpClient = client
            }
        }
    }

    suspend fun switchAccount(accountKey: String) {
        mutex.withLock {
            val account = accounts.first().firstOrNull { it.accountKey() == accountKey }
            context.settingsDataStore.updateData { settings ->
                settings.copy(currentUser = accountKey)
            }
            updateCurrentAccount(account)
        }
    }

    suspend fun addAccount(account: Account) {
        mutex.withLock {
            context.settingsDataStore.updateData { settings ->
                val users = settings.usersList.toMutableList()
                val index = users.indexOfFirst { it.accountKey == account.accountKey() }
                val currentSettings = users.getOrNull(index)?.settings ?: UserSettings()
                if (index != -1) {
                    users.removeAt(index)
                }
                users.add(account.toUserData().copy(settings = currentSettings))
                settings.copy(
                    usersList = users,
                    currentUser = account.accountKey(),
                )
            }
            updateCurrentAccount(account)
        }
    }

    suspend fun removeAccount(accountKey: String) {
        mutex.withLock {
            context.settingsDataStore.updateData { settings ->
                val users = settings.usersList.toMutableList()
                val index = users.indexOfFirst { it.accountKey == accountKey }
                if (index != -1) {
                    users.removeAt(index)
                }
                val newCurrentUser = if (settings.currentUser == accountKey) {
                    users.firstOrNull()?.accountKey ?: ""
                } else {
                    settings.currentUser
                }
                settings.copy(
                    usersList = users,
                    currentUser = newCurrentUser,
                )
            }
            updateCurrentAccount(currentAccount.first())
            purgeAccountData(accountKey)
        }
    }

    suspend fun exportLocalAccountZip(destinationUri: Uri) {
        val accountKey = Account.Local().accountKey()
        val memoDao = database.memoDao()
        val memos = memoDao.getAllMemosForSync(accountKey)
            .filterNot { it.isDeleted }
            .sortedWith(compareBy({ it.date }, { it.content }))

        if (memos.isEmpty()) {
            throw IllegalStateException("No local memos to export")
        }

        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                val collisionMap = hashMapOf<String, Int>()
                for (memo in memos) {
                    val memoBaseName = uniqueMemoBaseName(memo.date, collisionMap)
                    zip.putNextEntry(ZipEntry("$memoBaseName.md"))
                    zip.write(memo.content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    val resources = memoDao.getMemoResources(memo.identifier, accountKey)
                        .sortedWith(compareBy<ResourceEntity>({ it.filename }, { it.uri }))
                    resources.forEachIndexed { index, resource ->
                        val sourceFile = localFileForResource(resource)
                            ?: throw IllegalStateException("Missing resource file: ${resource.filename}")
                        if (!sourceFile.exists()) {
                            throw IllegalStateException("Missing resource file: ${resource.filename}")
                        }
                        val ext = exportFileExtension(resource, sourceFile)
                        val attachmentName = if (ext.isBlank()) {
                            "$memoBaseName-${index + 1}"
                        } else {
                            "$memoBaseName-${index + 1}.$ext"
                        }
                        zip.putNextEntry(ZipEntry(attachmentName))
                        sourceFile.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: throw IllegalStateException("Unable to open export destination")
    }

    private fun uniqueMemoBaseName(date: Instant, collisionMap: MutableMap<String, Int>): String {
        val base = exportDateFormatter.format(date)
        val count = collisionMap[base] ?: 0
        collisionMap[base] = count + 1
        return if (count == 0) base else "${base}_$count"
    }

    private fun localFileForResource(resource: ResourceEntity): File? {
        val uri = (resource.localUri ?: resource.uri).toUri()
        if (uri.scheme != "file") {
            return null
        }
        val path = uri.path ?: return null
        return File(path)
    }

    private fun exportFileExtension(resource: ResourceEntity, sourceFile: File): String {
        val filenameExt = resource.filename.substringAfterLast('.', "")
        if (filenameExt.isNotBlank()) {
            return filenameExt.lowercase(Locale.US)
        }
        val sourceExt = sourceFile.extension
        if (sourceExt.isNotBlank()) {
            return sourceExt.lowercase(Locale.US)
        }
        val fromMime = resource.mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return fromMime?.lowercase(Locale.US) ?: ""
    }

    private suspend fun purgeAccountData(accountKey: String) {
        val memoDao = database.memoDao()
        memoDao.deleteResourcesByAccount(accountKey)
        memoDao.deleteMemosByAccount(accountKey)
        fileStorage.deleteAccountFiles(accountKey)
    }

    private suspend fun updateAccountFromSyncedUser(accountKey: String, user: User) {
        mutex.withLock {
            context.settingsDataStore.updateData { settings ->
                val index = settings.usersList.indexOfFirst { it.accountKey == accountKey }
                if (index == -1) {
                    return@updateData settings
                }
                val existingUser = settings.usersList[index]
                val current = Account.parseUserData(existingUser) ?: return@updateData settings
                val updated = current.withUser(user)
                val users = settings.usersList.toMutableList()
                users[index] = updated.toUserData().copy(settings = existingUser.settings)
                settings.copy(usersList = users)
            }
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
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosV0Api::class.java)
    }

    fun createMemosV1Client(host: String, accessToken: String?): Pair<OkHttpClient, MemosV1Api> {
        val client = okHttpClient.newBuilder().apply {
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
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
            .create(MemosV1Api::class.java)
    }

    suspend fun checkLoginCompatibility(host: String, allowHigherV1Version: Boolean = false): LoginCompatibility {
        val serverVersion = detectAccountCaseAndVersion(host)
        return when (evaluateVersionPolicy(serverVersion)) {
            VersionPolicy.SUPPORTED -> LoginCompatibility.Supported(serverVersion.accountCase)
            VersionPolicy.TOO_LOW -> LoginCompatibility.Unsupported(R.string.memos_supported_versions.string)
            VersionPolicy.V1_HIGHER -> {
                if (allowHigherV1Version) {
                    LoginCompatibility.Supported(serverVersion.accountCase)
                } else {
                    LoginCompatibility.RequiresConfirmation(
                        accountCase = serverVersion.accountCase,
                        version = serverVersion.version,
                        message = R.string.memos_login_version_higher_warning.string,
                    )
                }
            }
        }
    }

    suspend fun checkCurrentAccountSyncCompatibility(
        isAutomatic: Boolean,
        allowHigherV1Version: String? = null,
    ): SyncCompatibility {
        val account = currentAccount.first() ?: return SyncCompatibility.Allowed
        if (account !is Account.MemosV0 && account !is Account.MemosV1) {
            return SyncCompatibility.Allowed
        }

        val serverVersion = fetchVersionForAccount(account)
            ?: return if (isAutomatic) {
                SyncCompatibility.Blocked(null)
            } else {
                SyncCompatibility.Blocked(R.string.memos_supported_versions.string)
            }
        return when (evaluateVersionPolicy(serverVersion)) {
            VersionPolicy.SUPPORTED -> SyncCompatibility.Allowed
            VersionPolicy.TOO_LOW -> {
                if (isAutomatic) {
                    SyncCompatibility.Blocked(null)
                } else {
                    SyncCompatibility.Blocked(R.string.memos_supported_versions.string)
                }
            }
            VersionPolicy.V1_HIGHER -> {
                val accepted = isUnsupportedSyncVersionAccepted(account.accountKey(), serverVersion.version)
                if (isAutomatic) {
                    return if (accepted) {
                        SyncCompatibility.Allowed
                    } else {
                        SyncCompatibility.Blocked(null)
                    }
                }
                if (allowHigherV1Version == serverVersion.version) {
                    return SyncCompatibility.Allowed
                }
                if (accepted) {
                    return SyncCompatibility.Allowed
                }
                SyncCompatibility.RequiresConfirmation(
                    version = serverVersion.version,
                    message = R.string.memos_sync_version_higher_warning.string,
                )
            }
        }
    }

    suspend fun rememberAcceptedUnsupportedSyncVersion(version: String) {
        val accountKey = currentAccount.first()?.accountKey() ?: return
        mutex.withLock {
            context.settingsDataStore.updateData { settings ->
                val users = settings.usersList.toMutableList()
                val index = users.indexOfFirst { it.accountKey == accountKey }
                if (index == -1) {
                    return@updateData settings
                }
                val user = users[index]
                val versions = (user.settings.acceptedUnsupportedSyncVersions + version).distinct()
                users[index] = user.copy(
                    settings = user.settings.copy(acceptedUnsupportedSyncVersions = versions)
                )
                settings.copy(usersList = users)
            }
        }
    }

    suspend fun detectAccountCase(host: String): UserData.AccountCase {
        return detectAccountCaseAndVersion(host).accountCase
    }

    suspend fun getRepository(): AbstractMemoRepository {
        mutex.withLock {
            return repository
        }
    }

    suspend fun getRemoteRepository(): RemoteRepository? {
        mutex.withLock {
            return remoteRepository
        }
    }

    private suspend fun detectAccountCaseAndVersion(host: String): ServerVersionInfo {
        val memosV0Status = createMemosV0Client(host, null).second.status().getOrNull()
        val memosV0Version = memosV0Status?.profile?.version?.trim().orEmpty()
        if (memosV0Version.isNotEmpty()) {
            return ServerVersionInfo(UserData.AccountCase.MEMOS_V0, memosV0Version)
        }

        val memosV1Profile = createMemosV1Client(host, null).second.getProfile().getOrThrow()
        val memosV1Version = memosV1Profile.version.trim()
        if (memosV1Version.isNotEmpty()) {
            return ServerVersionInfo(UserData.AccountCase.MEMOS_V1, memosV1Version)
        }

        return ServerVersionInfo(UserData.AccountCase.ACCOUNT_NOT_SET, "")
    }

    private suspend fun fetchVersionForAccount(account: Account): ServerVersionInfo? {
        return when (account) {
            is Account.MemosV0 -> {
                val version = createMemosV0Client(account.info.host, account.info.accessToken)
                    .second
                    .status()
                    .getOrNull()
                    ?.profile
                    ?.version
                    ?.trim()
                    .orEmpty()
                if (version.isBlank()) null else ServerVersionInfo(UserData.AccountCase.MEMOS_V0, version)
            }
            is Account.MemosV1 -> {
                val version = createMemosV1Client(account.info.host, account.info.accessToken)
                    .second
                    .getProfile()
                    .getOrNull()
                    ?.version
                    ?.trim()
                    .orEmpty()
                if (version.isBlank()) null else ServerVersionInfo(UserData.AccountCase.MEMOS_V1, version)
            }
            else -> null
        }
    }

    private suspend fun isUnsupportedSyncVersionAccepted(accountKey: String, version: String): Boolean {
        val userData = context.settingsDataStore.data.first()
            .usersList
            .firstOrNull { it.accountKey == accountKey }
            ?: return false
        return userData.settings.acceptedUnsupportedSyncVersions.contains(version)
    }

    private fun evaluateVersionPolicy(serverVersion: ServerVersionInfo): VersionPolicy {
        val version = parseSemanticVersion(serverVersion.version) ?: return VersionPolicy.TOO_LOW
        return when (serverVersion.accountCase) {
            UserData.AccountCase.MEMOS_V0 -> {
                if (version < MEMOS_V0_MIN_VERSION) VersionPolicy.TOO_LOW else VersionPolicy.SUPPORTED
            }
            UserData.AccountCase.MEMOS_V1 -> {
                when {
                    version < MEMOS_V1_MIN_VERSION -> VersionPolicy.TOO_LOW
                    version > MEMOS_V1_MAX_VERSION -> VersionPolicy.V1_HIGHER
                    else -> VersionPolicy.SUPPORTED
                }
            }
            else -> VersionPolicy.TOO_LOW
        }
    }

    private fun parseSemanticVersion(version: String): SemanticVersion? {
        val match = SEMANTIC_VERSION_REGEX.find(version) ?: return null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return null
        return SemanticVersion(major = major, minor = minor, patch = patch)
    }

    companion object {
        private val MEMOS_V0_MIN_VERSION = SemanticVersion(0, 21, 0)
        private val MEMOS_V1_MIN_VERSION = SemanticVersion(0, 26, 0)
        private val MEMOS_V1_MAX_VERSION = SemanticVersion(0, 26, 1)
        private val SEMANTIC_VERSION_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")
    }
}
