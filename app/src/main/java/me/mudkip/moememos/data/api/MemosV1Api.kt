package me.mudkip.moememos.data.api

import android.net.Uri
import com.skydoves.sandwich.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import androidx.core.net.toUri
import java.time.Instant

interface MemosV1Api {
    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): ApiResponse<GetCurrentUserResponse>

    @GET("api/v1/users/{id}/settings/GENERAL")
    suspend fun getUserSetting(@Path("id") userId: String): ApiResponse<MemosV1UserSetting>

    @GET("api/v1/memos")
    suspend fun listMemos(
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String? = null,
        @Query("state") state: MemosV1State? = null,
        @Query("filter") filter: String? = null,
    ): ApiResponse<ListMemosResponse>

    @POST("api/v1/memos")
    suspend fun createMemo(@Body body: MemosV1CreateMemoRequest): ApiResponse<MemosV1Memo>

    @PATCH("api/v1/memos/{id}/attachments")
    suspend fun setMemoResources(@Path("id") memoId: String, @Body body: MemosV1SetMemoResourcesRequest): ApiResponse<Unit>

    @PATCH("api/v1/memos/{id}")
    suspend fun updateMemo(@Path("id") memoId: String, @Body body: UpdateMemoRequest): ApiResponse<MemosV1Memo>

    @DELETE("api/v1/memos/{id}")
    suspend fun deleteMemo(@Path("id") memoId: String): ApiResponse<Unit>

    @GET("api/v1/attachments")
    suspend fun listResources(): ApiResponse<ListResourceResponse>

    @POST("api/v1/attachments")
    suspend fun createResource(@Body body: CreateResourceRequest): ApiResponse<MemosV1Resource>

    @DELETE("api/v1/attachments/{id}")
    suspend fun deleteResource(@Path("id") resourceId: String): ApiResponse<Unit>

    @GET("api/v1/instance/profile")
    suspend fun getProfile(): ApiResponse<MemosProfile>

    @GET("api/v1/users/{id}")
    suspend fun getUser(@Path("id") userId: String): ApiResponse<MemosV1User>

    @GET("api/v1/users/{id}:getStats")
    suspend fun getUserStats(@Path("id") userId: String): ApiResponse<MemosV1Stats>
}

@Serializable
data class MemosV1User(
    val name: String,
    val role: MemosRole = MemosRole.ROLE_UNSPECIFIED,
    val username: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val state: MemosV1State = MemosV1State.STATE_UNSPECIFIED,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val createTime: Instant? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val updateTime: Instant? = null
)

@Serializable
data class GetCurrentUserResponse(
    val user: MemosV1User?
)

@Serializable
data class MemosV1CreateMemoRequest(
    val content: String,
    val visibility: MemosVisibility?
)

@Serializable
data class ListMemosResponse(
    val memos: List<MemosV1Memo>,
    val nextPageToken: String?
)

@Serializable
data class MemosV1SetMemoResourcesRequest(
    val name: String,
    val attachments: List<MemosV1Resource>
)

@Serializable
data class UpdateMemoRequest(
    val content: String? = null,
    val visibility: MemosVisibility? = null,
    val state: MemosV1State? = null,
    val pinned: Boolean? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val updateTime: Instant? = null,
)

@Serializable
data class ListResourceResponse(
    val attachments: List<MemosV1Resource>
)

@Serializable
data class CreateResourceRequest(
    val filename: String,
    val type: String,
    val content: String,
    val memo: String?
)

@Serializable
data class MemosV1Memo(
    val name: String,
    val state: MemosV1State? = null,
    val creator: String? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val createTime: Instant? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val updateTime: Instant? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val displayTime: Instant? = null,
    val content: String? = null,
    val visibility: MemosVisibility? = null,
    val pinned: Boolean? = null,
    val attachments: List<MemosV1Resource>? = null,
    val tags: List<String>? = null
)

@Serializable
data class MemosV1Resource(
    val name: String? = null,
    @Serializable(with = Rfc3339InstantSerializer::class)
    val createTime: Instant? = null,
    val filename: String? = null,
    val externalLink: String? = null,
    val type: String? = null,
    val size: String? = null,
    val memo: String? = null
) {
    fun uri(host: String): Uri {
        if (!externalLink.isNullOrEmpty()) {
            return externalLink.toUri()
        }
        return host.toUri()
            .buildUpon().appendPath("file").appendEncodedPath(name ?: "").appendPath(filename ?: "").build()
    }
}

@Serializable
data class MemosV1UserSettingGeneralSetting(
    val locale: String? = null,
    val memoVisibility: MemosVisibility? = null,
    val theme: String? = null
)

@Serializable
data class MemosV1UserSetting(
    val generalSetting: MemosV1UserSettingGeneralSetting?
)

@Serializable
enum class MemosV1State {
    @SerialName("STATE_UNSPECIFIED")
    STATE_UNSPECIFIED,
    @SerialName("NORMAL")
    NORMAL,
    @SerialName("ARCHIVED")
    ARCHIVED,
}

@Serializable
data class MemosV1Stats(
    val tagCount: Map<String, Int>,
)
