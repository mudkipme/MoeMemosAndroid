package me.mudkip.moememos.ext

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import com.skydoves.sandwich.serialization.deserializeErrorBody
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.ErrorMessage
import timber.log.Timber

fun <T> ApiResponse<T>.getErrorMessage(): String {
    if (this is ApiResponse.Failure.Error<T>) {
        try {
            val errorMessage: ErrorMessage? = this.deserializeErrorBody()
            if (errorMessage != null) {
                return errorMessage.message
            }
        } catch (e: Throwable) {
            Timber.d(e)
        }
        return response.errorBody()?.string() ?: this.message()
    }

    if (this is ApiResponse.Failure.Exception<T>) {
        return this.exception.localizedMessage ?: this.message()
    }
    return ""
}

suspend inline fun <T> ApiResponse<T>.suspendOnErrorMessage(crossinline block: suspend (message: String) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Failure.Error<T>) {
        block(getErrorMessage())
    } else if (this is ApiResponse.Failure.Exception<T>) {
        block(getErrorMessage())
    }

    return this
}

suspend inline fun <T> ApiResponse<T>.suspendOnNotLogin(crossinline block: suspend ApiResponse.Failure<T>.() -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Failure.Exception<T>) {
        if (this.exception == MoeMemosException.notLogin) {
            block(this)
        }
    }
    if (this is ApiResponse.Failure.Error<T>) {
        if (this.response.code() == 401) {
            block(this)
        }
    }
    return this
}