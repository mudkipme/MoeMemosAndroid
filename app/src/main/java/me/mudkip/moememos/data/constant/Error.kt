package me.mudkip.moememos.data.constant

class MoeMemosException(string: String) : Exception(string) {
    companion object {
        val notLogin = MoeMemosException("NOT_LOGIN")
    }
}