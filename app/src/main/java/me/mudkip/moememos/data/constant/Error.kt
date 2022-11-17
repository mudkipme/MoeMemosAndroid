package me.mudkip.moememos.data.constant

class MoeMemosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MoeMemosException("NOT_LOGIN")
        val invalidOpenAPI = MoeMemosException("INVALID_OPEN_API")
    }

    override fun getLocalizedMessage(): String? {
        return when (this) {
            invalidOpenAPI -> "Invalid Open API"
            else -> {
                super.getLocalizedMessage()
            }
        }
    }
}