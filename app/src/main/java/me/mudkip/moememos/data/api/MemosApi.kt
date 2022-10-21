package me.mudkip.moememos.data.api

import com.skydoves.sandwich.ApiResponse
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemosRowStatus
import me.mudkip.moememos.data.model.MemosVisibility
import me.mudkip.moememos.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class SignInInput(
    val email: String,
    val password: String
)

data class SignInOutput(
    val data: User
)

data class MeOutput(
    val data: User
)

data class ListMemoOutput(
    val data: List<Memo>
)

data class CreateMemoInput(
    val content: String,
    val visibility: MemosVisibility? = null
)

data class CreateMemoOutput(
    val data: Memo
)

interface MemosApi {
    @POST("/api/auth/signin")
    suspend fun signIn(@Body body: SignInInput): ApiResponse<SignInOutput>

    @POST("/api/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @GET("/api/user/me")
    suspend fun me(): ApiResponse<MeOutput>

    @GET("/api/memo")
    suspend fun listMemo(
        @Query("creatorId") creatorId: Int? = null,
        @Query("rowStatus") rowStatus: MemosRowStatus? = null,
        @Query("visibility") visibility: MemosVisibility? = null
    ): ApiResponse<ListMemoOutput>

    @POST("/api/memo")
    suspend fun createMemo(@Body body: CreateMemoInput): ApiResponse<CreateMemoOutput>
}