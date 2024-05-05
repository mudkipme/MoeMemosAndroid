package me.mudkip.moememos.data.model

sealed class Account {
    fun accountKey(): String = when (this) {
        is Memos -> "memos:${this.info.host}:${this.info.id}"
        Local -> "local"
    }

    fun toUserData(): UserData = when (this) {
        is Memos -> UserData.newBuilder().setAccountKey(accountKey()).setMemos(this.info).build()
        Local -> UserData.newBuilder().setAccountKey(accountKey()).setLocal(LocalAccount.getDefaultInstance()).build()
    }

    companion object {
        fun parseUserData(userData: UserData): Account? = when (userData.accountCase) {
            UserData.AccountCase.MEMOS -> Memos(userData.memos)
            UserData.AccountCase.LOCAL -> Local
            else -> null
        }
    }

    class Memos(val info: MemosAccount) : Account()
    data object Local : Account()
}