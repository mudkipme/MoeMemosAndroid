package me.mudkip.moememos.data.api

import android.net.Uri
import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.mudkip.moememos.data.model.User
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant

@Keep
data class MemosV0SignInInput(
    val email: String,
    var username: String,
    val password: String,
    val remember: Boolean
)

@Keep
data class MemosV0CreateMemoInput(
    val content: String,
    val visibility: MemosVisibility? = null,
    val resourceIdList: List<Long>? = null
)

@Keep
data class MemosV0UpdateMemoOrganizerInput(
    val pinned: Boolean
)

@Keep
data class MemosV0UpdateTagInput(
    val name: String
)

@Keep
data class MemosV0DeleteTagInput(
    val name: String
)

@Keep
data class MemosV0PatchMemoInput(
    val id: Long,
    val createdTs: Long? = null,
    val rowStatus: MemosRowStatus? = null,
    val content: String? = null,
    val visibility: MemosVisibility? = null,
    val resourceIdList: List<Long>? = null,
)

interface MemosV0Api {
    @GET("api/v1/user/me")
    suspend fun me(): ApiResponse<MemosV0User>

    @GET("api/v1/memo")
    suspend fun listMemo(
        @Query("creatorId") creatorId: Long? = null,
        @Query("rowStatus") rowStatus: MemosRowStatus? = null,
        @Query("visibility") visibility: MemosVisibility? = null
    ): ApiResponse<List<MemosV0Memo>>

    @POST("api/v1/memo")
    suspend fun createMemo(@Body body: MemosV0CreateMemoInput): ApiResponse<MemosV0Memo>

    @GET("api/v1/tag")
    suspend fun getTags(@Query("creatorId") creatorId: Long? = null): ApiResponse<List<String>>

    @POST("api/v1/tag")
    suspend fun updateTag(@Body body: MemosV0UpdateTagInput): ApiResponse<String>

    @POST("api/v1/tag/delete")
    suspend fun deleteTag(@Body body: MemosV0DeleteTagInput): ApiResponse<Unit>

    @POST("api/v1/memo/{id}/organizer")
    suspend fun updateMemoOrganizer(@Path("id") memoId: Long, @Body body: MemosV0UpdateMemoOrganizerInput): ApiResponse<MemosV0Memo>

    @PATCH("api/v1/memo/{id}")
    suspend fun patchMemo(@Path("id") memoId: Long, @Body body: MemosV0PatchMemoInput): ApiResponse<MemosV0Memo>

    @DELETE("api/v1/memo/{id}")
    suspend fun deleteMemo(@Path("id") memoId: Long): ApiResponse<Unit>

    @GET("api/v1/resource")
    suspend fun getResources(): ApiResponse<List<MemosV0Resource>>

    @Multipart
    @POST("api/v1/resource/blob")
    suspend fun uploadResource(@Part file: MultipartBody.Part): ApiResponse<MemosV0Resource>

    @DELETE("api/v1/resource/{id}")
    suspend fun deleteResource(@Path("id") resourceId: Long): ApiResponse<Unit>

    @GET("api/v1/status")
    suspend fun status(): ApiResponse<MemosV0Status>

    @GET("api/v1/memo/all")
    suspend fun listAllMemo(
        @Query("pinned") pinned: Boolean? = null,
        @Query("tag") tag: String? = null,
        @Query("visibility") visibility: MemosVisibility? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ApiResponse<List<MemosV0Memo>>
}

@Keep
data class MemosV0User(
    val createdTs: Long,
    val email: String?,
    val id: Long,
    val name: String?,
    val role: MemosRole = MemosRole.USER,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Long?,
    val userSettingList: List<MemosV0UserSetting>? = null,
    val nickname: String?,
    val username: String?,
    val avatarUrl: String?,
) {
    val displayEmail get() = email ?: username ?: ""
    val displayName get() = nickname ?: name ?: ""

    private val memoVisibility: MemosVisibility
        get() = userSettingList?.find { it.key == MemosV0UserSettingKey.MEMO_VISIBILITY }?.let {
        try {
            MemosVisibility.valueOf(it.value.removePrefix("\"").removeSuffix("\""))
        } catch (_: IllegalArgumentException) {
            MemosVisibility.PRIVATE
        }
    } ?: MemosVisibility.PRIVATE

    fun toUser(): User {
        return User(
            identifier = id.toString(),
            name = displayName,
            startDate = Instant.ofEpochSecond(createdTs),
            defaultVisibility = memoVisibility.toMemoVisibility()
        )
    }
}

@JsonClass(generateAdapter = false)
enum class MemosV0UserSettingKey {
    @field:Json(name = "locale")
    LOCALE,
    @field:Json(name = "memo-visibility")
    MEMO_VISIBILITY,
    @field:Json(name = "editorFontStyle")
    EDITOR_FONT_STYLE,
    UNKNOWN
}

@Keep
data class MemosV0UserSetting(
    val key: MemosV0UserSettingKey = MemosV0UserSettingKey.UNKNOWN,
    val value: String
)

@Keep
data class MemosV0Memo(
    val id: Long,
    val createdTs: Long,
    val creatorId: Long,
    val creatorName: String? = null,
    var content: String,
    var pinned: Boolean,
    val rowStatus: MemosRowStatus = MemosRowStatus.NORMAL,
    val updatedTs: Long,
    val visibility: MemosVisibility = MemosVisibility.PRIVATE,
    val resourceList: List<MemosV0Resource>? = null
)

@Keep
data class MemosV0Resource(
    val id: Long,
    val createdTs: Long,
    val creatorId: Long,
    val filename: String,
    val size: Long,
    val type: String,
    val updatedTs: Long,
    val externalLink: String?,
    val publicId: String?,
    var name: String?,
    var uid: String?
) {
    fun uri(host: String): Uri {
        if (!externalLink.isNullOrEmpty()) {
            return Uri.parse(externalLink)
        }
        if (!uid.isNullOrEmpty()) {
            return Uri.parse(host)
                .buildUpon().appendPath("o").appendPath("r")
                .appendPath(uid.toString()).build()
        }
        if (!name.isNullOrEmpty()) {
            return Uri.parse(host)
                .buildUpon().appendPath("o").appendPath("r")
                .appendPath(name.toString()).build()
        }
        if (!publicId.isNullOrEmpty()) {
            return Uri.parse(host)
                .buildUpon().appendPath("o").appendPath("r")
                .appendPath(id.toString()).appendPath(publicId).build()
        }
        return Uri.parse(host)
            .buildUpon().appendPath("o").appendPath("r")
            .appendPath(id.toString()).appendPath(filename).build()
    }
}

@Keep
data class MemosV0Status(
    val profile: MemosProfile
)