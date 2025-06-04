package me.mudkip.moememos.data.api

import android.net.Uri
import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Date

interface MemosV1Api {
    @POST("api/v1/auth/status")
    suspend fun authStatus(): ApiResponse<MemosV1User>

    @GET("api/v1/users/{id}/setting")
    suspend fun getUserSetting(@Path("id") userId: String): ApiResponse<MemosV1UserSetting>

    @GET("api/v1/memos")
    suspend fun listMemos(
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String? = null,
        @Query("state") state: MemosV1State? = null,
        @Query("parent") parent: String? = null,
    ): ApiResponse<ListMemosResponse>

    @POST("api/v1/memos")
    suspend fun createMemo(@Body body: MemosV1CreateMemoRequest): ApiResponse<MemosV1Memo>

    @PATCH("api/v1/memos/{id}/resources")
    suspend fun setMemoResources(@Path("id") memoId: String, @Body body: MemosV1SetMemoResourcesRequest): ApiResponse<Unit>

    @PATCH("api/v1/memos/{id}")
    suspend fun updateMemo(@Path("id") memoId: String, @Body body: UpdateMemoRequest): ApiResponse<MemosV1Memo>

    @DELETE("api/v1/memos/{id}")
    suspend fun deleteMemo(@Path("id") memoId: String): ApiResponse<Unit>

    @DELETE("api/v1/memos/{id}/tags/{tag}")
    suspend fun deleteMemoTag(@Path("id") memoId: String, @Path("tag") tag: String, @Query("deleteRelatedMemos") deleteRelatedMemos: Boolean): ApiResponse<Unit>

    @GET("api/v1/resources")
    suspend fun listResources(): ApiResponse<ListResourceResponse>

    @POST("api/v1/resources")
    suspend fun createResource(@Body body: CreateResourceRequest): ApiResponse<MemosV1Resource>

    @DELETE("api/v1/resources/{id}")
    suspend fun deleteResource(@Path("id") resourceId: String): ApiResponse<Unit>

    @GET("api/v1/workspace/profile")
    suspend fun getProfile(): ApiResponse<MemosProfile>

    @GET("api/v1/users/{id}")
    suspend fun getUser(@Path("id") userId: String): ApiResponse<MemosV1User>

    @GET("api/v1/users/{id}/stats")
    suspend fun getUserStats(@Path("id") userId: String): ApiResponse<MemosV1Stats>
}

@Keep
data class MemosV1User(
    val name: String,
    val role: MemosRole,
    val username: String,
    val email: String,
    val nickname: String,
    val avatarUrl: String,
    val description: String,
    val state: MemosV1State? = null,
    val createTime: Date,
    val updateTime: Date
)

@Keep
data class MemosV1CreateMemoRequest(
    val content: String,
    val visibility: MemosVisibility?
)

@Keep
data class ListMemosResponse(
    val memos: List<MemosV1Memo>,
    val nextPageToken: String
)

@Keep
data class MemosV1SetMemoResourcesRequest(
    val resources: List<MemosV1SetMemoResourcesRequestItem>
)

@Keep
data class MemosV1SetMemoResourcesRequestItem(
    val name: String
)

@Keep
data class UpdateMemoRequest(
    val content: String? = null,
    val visibility: MemosVisibility? = null,
    val state: MemosV1State? = null,
    val pinned: Boolean? = null,
    val updateTime: Date? = null,
)

@Keep
data class ListResourceResponse(
    val resources: List<MemosV1Resource>
)

@Keep
data class CreateResourceRequest(
    val filename: String,
    val type: String,
    val content: String,
    val memo: String?
)

@Keep
data class MemosV1Memo(
    val name: String,
    val state: MemosV1State? = null,
    val creator: String,
    val createTime: Date,
    val updateTime: Date,
    val displayTime: Date,
    val content: String,
    val visibility: MemosVisibility,
    val pinned: Boolean,
    val resources: List<MemosV1Resource>,
    val tags: List<String>? = null
)

@Keep
data class MemosV1Resource(
    val name: String,
    val createTime: Date,
    val filename: String,
    val externalLink: String,
    val type: String,
    val size: Int,
    val memo: String?
) {
    fun uri(host: String): Uri {
        if (externalLink.isNotEmpty()) {
            return Uri.parse(externalLink)
        }

        return Uri.parse(host).buildUpon().apply {
            appendPath("file")
            name.split("/").forEach { path -> appendPath(path) }
            appendPath(filename)
        }.build()
    }
}

@Keep
data class MemosV1UserSetting(
    val name: String,
    val locale: String,
    val appearance: String,
    val memoVisibility: MemosVisibility
)

@JsonClass(generateAdapter = false)
enum class MemosV1State {
    @field:Json(name = "STATE_UNSPECIFIED")
    STATE_UNSPECIFIED,
    @field:Json(name = "NORMAL")
    NORMAL,
    @field:Json(name = "ARCHIVED")
    ARCHIVED,
}

@Keep
data class MemosV1Stats(
    val tagCount: Map<String, Int>,
)
