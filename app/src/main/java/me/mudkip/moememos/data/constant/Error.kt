package me.mudkip.moememos.data.constant

import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

class MoeMemosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MoeMemosException("NOT_LOGIN")
        val invalidAccessToken = MoeMemosException("INVALID_ACCESS_TOKEN")
        val invalidParameter = MoeMemosException("INVALID_PARAMETER")
        val invalidServer = MoeMemosException("INVALID_SERVER")
    }

    override fun getLocalizedMessage(): String? {
        return when (this) {
            invalidAccessToken -> R.string.invalid_access_token.string
            else -> {
                super.getLocalizedMessage()
            }
        }
    }
}