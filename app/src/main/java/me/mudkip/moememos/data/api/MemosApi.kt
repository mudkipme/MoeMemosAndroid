package me.mudkip.moememos.data.api

import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

@Keep
data class SignInInput(
    val email: String,
    var username: String,
    val password: String,
    val remember: Boolean
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
data class UpdateTagInput(
    val name: String
)

@Keep
data class DeleteTagInput(
    val name: String
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
    @POST("api/v1/auth/signin")
    suspend fun signIn(@Body body: SignInInput): ApiResponse<Unit>

    @POST("api/v1/auth/signout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("api/v1/user/me")
    suspend fun me(): ApiResponse<User>

    @GET("api/v1/memo")
    suspend fun listMemo(
        @Query("creatorId") creatorId: Long? = null,
        @Query("rowStatus") rowStatus: MemosRowStatus? = null,
        @Query("visibility") visibility: MemosVisibility? = null
    ): ApiResponse<List<Memo>>

    @POST("api/v1/memo")
    suspend fun createMemo(@Body body: CreateMemoInput): ApiResponse<Memo>

    @GET("api/v1/tag")
    suspend fun getTags(@Query("creatorId") creatorId: Long? = null): ApiResponse<List<String>>

    @POST("api/v1/tag")
    suspend fun updateTag(@Body body: UpdateTagInput): ApiResponse<String>

    @POST("api/v1/tag/delete")
    suspend fun deleteTag(@Body body: DeleteTagInput): ApiResponse<Unit>

    @POST("api/v1/memo/{id}/organizer")
    suspend fun updateMemoOrganizer(@Path("id") memoId: Long, @Body body: UpdateMemoOrganizerInput): ApiResponse<Memo>

    @PATCH("api/v1/memo/{id}")
    suspend fun patchMemo(@Path("id") memoId: Long, @Body body: PatchMemoInput): ApiResponse<Memo>

    @DELETE("api/v1/memo/{id}")
    suspend fun deleteMemo(@Path("id") memoId: Long): ApiResponse<Unit>

    @GET("api/v1/resource")
    suspend fun getResources(): ApiResponse<List<Resource>>

    @Multipart
    @POST("api/v1/resource/blob")
    suspend fun uploadResource(@Part file: MultipartBody.Part): ApiResponse<Resource>

    @DELETE("api/v1/resource/{id}")
    suspend fun deleteResource(@Path("id") resourceId: Long): ApiResponse<Unit>

    @GET("api/v1/status")
    suspend fun status(): ApiResponse<Status>

    @GET("api/v1/memo/all")
    suspend fun listAllMemo(
        @Query("pinned") pinned: Boolean? = null,
        @Query("tag") tag: String? = null,
        @Query("visibility") visibility: MemosVisibility? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ApiResponse<List<Memo>>
}