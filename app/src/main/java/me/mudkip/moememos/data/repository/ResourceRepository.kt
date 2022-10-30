package me.mudkip.moememos.data.repository

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mapSuccess
import me.mudkip.moememos.data.api.MemosApiService
import me.mudkip.moememos.data.model.Resource
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ResourceRepository @Inject constructor(private val memosApiService: MemosApiService) {
    suspend fun loadResources(): ApiResponse<List<Resource>> = memosApiService.call { api ->
        api.getResources().mapSuccess { data }
    }

    suspend fun uploadResource(resourceData: ByteArray, filename: String, mediaType: MediaType): ApiResponse<Resource> = memosApiService.call { api ->
        val file = MultipartBody.Part.createFormData("file", filename, resourceData.toRequestBody(mediaType))
        api.uploadResource(file).mapSuccess { data }
    }
}