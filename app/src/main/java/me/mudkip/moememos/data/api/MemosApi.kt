package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

@Keep
data class MemosOutput<T>(
    val data: T
)

@Keep
data class SignInInput(
    val email: String,
    var username: String,
    val password: String
)

@Keep
data class CreateMemoInput(
    val content: String,
    val visibility: MemosVisibility? = null,
    val resourceIdList: List<Long>? = null
)

@Keep
data class UpdateMemoOrganizerInput(
    val pinned: Boolean
)

@Keep
data class PatchMemoInput(
    val id: Long,
    val createdTs: Long? = null,
    val rowStatus: MemosRowStatus? = null,
    val content: String? = null,
    val visibility: MemosVisibility? = null,
    val resourceIdList: List<Long>? = null,
)

interface MemosApi {
    @POST("/api/auth/signin")
    suspend fun signIn(@Body body: SignInInput): ApiResponse<MemosOutput<User>>

    @POST("/api/auth/signout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("/api/auth/logout")
    suspend fun logoutLegacy(): ApiResponse<Unit>

    @GET("/api/user/me")
    suspend fun me(): ApiResponse<MemosOutput<User>>

    @GET("/api/memo")
    suspend fun listMemo(
        @Query("creatorId") creatorId: Long? = null,
        @Query("rowStatus") rowStatus: MemosRowStatus? = null,
        @Query("visibility") visibility: MemosVisibility? = null
    ): ApiResponse<MemosOutput<List<Memo>>>

    @POST("/api/memo")
    suspend fun createMemo(@Body body: CreateMemoInput): ApiResponse<MemosOutput<Memo>>

    @GET("/api/tag")
    suspend fun getTags(@Query("creatorId") creatorId: Long? = null): ApiResponse<MemosOutput<List<String>>>

    @POST("/api/memo/{id}/organizer")
    suspend fun updateMemoOrganizer(@Path("id") memoId: Long, @Body body: UpdateMemoOrganizerInput): ApiResponse<MemosOutput<Memo>>

    @PATCH("/api/memo/{id}")
    suspend fun patchMemo(@Path("id") memoId: Long, @Body body: PatchMemoInput): ApiResponse<MemosOutput<Memo>>

    @DELETE("/api/memo/{id}")
    suspend fun deleteMemo(@Path("id") memoId: Long): ApiResponse<Unit>

    @GET("/api/resource")
    suspend fun getResources(): ApiResponse<MemosOutput<List<Resource>>>

    @Multipart
    @POST("/api/resource/blob")
    suspend fun uploadResource(@Part file: MultipartBody.Part): ApiResponse<MemosOutput<Resource>>

    @Multipart
    @POST("/api/resource")
    suspend fun uploadResourceLegacy(@Part file: MultipartBody.Part): ApiResponse<MemosOutput<Resource>>

    @DELETE("/api/resource/{id}")
    suspend fun deleteResource(@Path("id") resourceId: Long): ApiResponse<Unit>

    @GET("/auth")
    suspend fun auth(): ApiResponse<Unit>

    @GET("/api/status")
    suspend fun status(): ApiResponse<MemosOutput<Status>>
}