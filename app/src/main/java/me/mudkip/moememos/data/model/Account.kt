package me.mudkip.moememos.data.model

sealed class Account {
    fun accountKey(): String = when (this) {
        is MemosV0 -> "memos:${this.info.host}:${this.info.id}"
        is MemosV1 -> "memos:${this.info.host}:${this.info.id}"
        Local -> "local"
    }

    fun toUserData(): UserData = when (this) {
        is MemosV0 -> UserData(accountKey = accountKey(), memosV0 = this.info)
        is MemosV1 -> UserData(accountKey = accountKey(), memosV1 = this.info)
        Local -> UserData(accountKey = accountKey(), local = LocalAccount())
    }

    fun getAccountInfo(): MemosAccount? = when (this) {
        is MemosV0 -> this.info
        is MemosV1 -> this.info
        else -> null
    }

    companion object {
        fun parseUserData(userData: UserData): Account? = when (userData.accountCase) {
            UserData.AccountCase.MEMOS_V0 -> userData.memosV0?.let { MemosV0(it) }
            UserData.AccountCase.MEMOS_V1 -> userData.memosV1?.let { MemosV1(it) }
            UserData.AccountCase.LOCAL -> Local
            else -> null
        }
    }

    class MemosV0(val info: MemosAccount) : Account()
    class MemosV1(val info: MemosAccount) : Account()
    data object Local : Account()
}
