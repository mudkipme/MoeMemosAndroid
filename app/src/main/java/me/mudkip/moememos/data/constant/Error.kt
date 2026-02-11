package me.mudkip.moememos.data.constant

import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

class MoeMemosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MoeMemosException("NOT_LOGIN")
        val invalidAccessToken = MoeMemosException("INVALID_ACCESS_TOKEN")
        val accessTokenInvalid = MoeMemosException("ACCESS_TOKEN_INVALID")
        val invalidParameter = MoeMemosException("INVALID_PARAMETER")
        val invalidServer = MoeMemosException("INVALID_SERVER")
    }

    override fun getLocalizedMessage(): String? {
        return when (this) {
            invalidAccessToken -> R.string.invalid_access_token.string
            accessTokenInvalid -> R.string.access_token_invalid_relogin.string
            invalidServer -> R.string.invalid_server.string
            else -> {
                super.getLocalizedMessage()
            }
        }
    }
}
