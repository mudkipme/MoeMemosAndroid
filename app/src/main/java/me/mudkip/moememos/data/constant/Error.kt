package me.mudkip.moememos.data.constant

import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string

class MoeMemosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MoeMemosException("NOT_LOGIN")
        val invalidOpenAPI = MoeMemosException("INVALID_OPEN_API")
    }

    override fun getLocalizedMessage(): String? {
        return when (this) {
            invalidOpenAPI -> R.string.invaild_open_api.string
            else -> {
                super.getLocalizedMessage()
            }
        }
    }
}