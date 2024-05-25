package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Date

interface MemosV1Api {
    @POST("/api/v1/auth/signin")
    suspend fun signIn(@Body body: MemosV1SignInRequest): ApiResponse<MemosV1User>

    @POST("api/v1/auth/signout")
    suspend fun signOut(): ApiResponse<Unit>

    @POST("api/v1/auth/status")
    suspend fun authStatus(): ApiResponse<MemosV1User>

    @GET("api/v1/memos")
    suspend fun listMemos(
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String,
        @Query("filter") filter: String
    ): ApiResponse<List<MemosV1Memo>>

    @POST("api/v1/memos")
    suspend fun createMemo(@Body body: MemosV1CreateMemoRequest): ApiResponse<MemosV1Memo>
}

@Keep
data class MemosV1User(
    val name: String,
    val id: Int,
    val role: MemosRole,
    val username: String,
    val email: String,
    val nickname: String,
    val avatarUrl: String,
    val description: String,
    val rowStatus: MemosRowStatus,
    val createTime: Date,
    val updateTime: Date
)

@Keep
data class MemosV1SignInRequest(
    val username: String,
    val password: String,
    val neverExpire: Boolean
)

@Keep
data class MemosV1CreateMemoRequest(
    val content: String,
    val visibility: MemosVisibility?
)

@Keep
data class MemosV1Memo(
    val name: String,
    val uid: String,
    val rowStatus: MemosRowStatus,
    val creator: String,
    val createTime: Date,
    val updateTime: Date,
    val displayTime: Date,
    val content: String,
    val visibility: MemosVisibility,
    val tags: List<String>,
    val pinned: Boolean,
    val resources: List<MemosV1Resource>
)

@Keep
data class MemosV1Resource(
    val name: String,
    val uid: String,
    val createTime: Date,
    val filename: String,
    val externalLink: String,
    val type: String,
    val size: Int,
    val memo: String?
)