package me.mudkip.moememos.data.model

sealed class Account {
    fun accountKey(): String = when (this) {
        is MemosV0 -> "memos:${this.info.host}:${this.info.id}"
        is MemosV1 -> "memos:${this.info.host}:${this.info.id}"
        Local -> "local"
    }

    fun toUserData(): UserData = when (this) {
        is MemosV0 -> UserData.newBuilder().setAccountKey(accountKey()).setMemosV0(this.info).build()
        is MemosV1 -> UserData.newBuilder().setAccountKey(accountKey()).setMemosV1(this.info).build()
        Local -> UserData.newBuilder().setAccountKey(accountKey()).setLocal(LocalAccount.getDefaultInstance()).build()
    }

    companion object {
        fun parseUserData(userData: UserData): Account? = when (userData.accountCase) {
            UserData.AccountCase.MEMOS_V0 -> MemosV0(userData.memosV0)
            UserData.AccountCase.MEMOS_V1 -> MemosV1(userData.memosV1)
            UserData.AccountCase.LOCAL -> Local
            else -> null
        }
    }

    class MemosV0(val info: MemosAccount) : Account()
    class MemosV1(val info: MemosAccount) : Account()
    data object Local : Account()
}