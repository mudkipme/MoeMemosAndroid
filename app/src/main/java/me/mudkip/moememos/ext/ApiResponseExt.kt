package me.mudkip.moememos.ext

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.StatusCode
import com.skydoves.sandwich.message
import com.skydoves.sandwich.retrofit.errorBody
import com.skydoves.sandwich.retrofit.serialization.deserializeErrorBody
import com.skydoves.sandwich.retrofit.statusCode
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.ErrorMessage
import timber.log.Timber

fun <T> ApiResponse<T>.getErrorMessage(): String {
    if (this is ApiResponse.Failure.Error) {
        try {
            val errorMessage: ErrorMessage? = this.deserializeErrorBody()
            if (errorMessage != null) {
                return errorMessage.message
            }
        } catch (e: Throwable) {
            Timber.d(e)
        }
        return this.errorBody?.string() ?: this.message()
    }

    if (this is ApiResponse.Failure.Exception) {
        return this.throwable.localizedMessage ?: this.message()
    }
    return ""
}

suspend inline fun <T> ApiResponse<T>.suspendOnErrorMessage(crossinline block: suspend (message: String) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Failure.Error) {
        block(getErrorMessage())
    } else if (this is ApiResponse.Failure.Exception) {
        block(getErrorMessage())
    }

    return this
}

suspend inline fun <T> ApiResponse<T>.suspendOnNotLogin(crossinline block: suspend ApiResponse.Failure<Nothing>.() -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Failure.Exception) {
        if (this.throwable == MoeMemosException.notLogin) {
            block(this)
        }
    }
    if (this is ApiResponse.Failure.Error) {
        if (this.statusCode == StatusCode.Unauthorized) {
            block(this)
        }
    }
    return this
}