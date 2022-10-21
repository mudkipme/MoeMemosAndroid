package me.mudkip.moememos.ext

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.message
import com.skydoves.sandwich.serialization.deserializeErrorBody
import me.mudkip.moememos.data.constant.MoeMemosException
import me.mudkip.moememos.data.model.ErrorMessage
import timber.log.Timber

suspend inline fun <T> ApiResponse<T>.suspendOnErrorMessage(crossinline block: suspend (message: String) -> Unit): ApiResponse<T> {
    if (this is ApiResponse.Failure.Error<T>) {
        try {
            val errorMessage: ErrorMessage? = this.deserializeErrorBody()
            if (errorMessage != null) {
                block(errorMessage.message)
                return this
            }
        } catch (e: Throwable) {
            Timber.d(e)
        }
        response.errorBody()?.string()?.let {
            block(it)
            return this
        }
        block(this.message())
    }

    if (this is ApiResponse.Failure.Exception<T>) {
        block(this.message())
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