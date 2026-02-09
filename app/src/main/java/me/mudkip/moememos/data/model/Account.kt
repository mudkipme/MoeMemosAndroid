package me.mudkip.moememos.data.model

import java.time.Instant

sealed class Account {
    fun accountKey(): String = when (this) {
        is MemosV0 -> "memos:${this.info.host}:${this.info.id}"
        is MemosV1 -> "memos:${this.info.host}:${this.info.id}"
        is Local -> "local"
    }

    fun toUserData(): UserData = when (this) {
        is MemosV0 -> UserData(accountKey = accountKey(), memosV0 = this.info)
        is MemosV1 -> UserData(accountKey = accountKey(), memosV1 = this.info)
        is Local -> UserData(accountKey = accountKey(), local = this.info)
    }

    fun getAccountInfo(): MemosAccount? = when (this) {
        is MemosV0 -> this.info
        is MemosV1 -> this.info
        else -> null
    }

    fun toUser(): User = when (this) {
        is MemosV0 -> info.toUser()
        is MemosV1 -> info.toUser()
        is Local -> info.toUser()
    }

    fun withUser(user: User): Account = when (this) {
        is MemosV0 -> MemosV0(info.withUser(user))
        is MemosV1 -> MemosV1(info.withUser(user))
        is Local -> Local(info.withUser(user))
    }

    companion object {
        fun parseUserData(userData: UserData): Account? = when (userData.accountCase) {
            UserData.AccountCase.MEMOS_V0 -> userData.memosV0?.let { MemosV0(it) }
            UserData.AccountCase.MEMOS_V1 -> userData.memosV1?.let { MemosV1(it) }
            UserData.AccountCase.LOCAL -> Local(userData.local ?: LocalAccount())
            else -> null
        }
    }

    class MemosV0(val info: MemosAccount) : Account()
    class MemosV1(val info: MemosAccount) : Account()
    class Local(val info: LocalAccount = LocalAccount()) : Account()
}

private fun MemosAccount.toUser(): User {
    val visibility = MemoVisibility.entries.firstOrNull { it.name == defaultVisibility }
        ?: MemoVisibility.PRIVATE
    val startDate = if (startDateEpochSecond > 0L) {
        Instant.ofEpochSecond(startDateEpochSecond)
    } else {
        Instant.now()
    }
    return User(
        identifier = id.toString(),
        name = name,
        startDate = startDate,
        defaultVisibility = visibility
    )
}

private fun MemosAccount.withUser(user: User): MemosAccount {
    return copy(
        name = user.name,
        startDateEpochSecond = user.startDate.epochSecond,
        defaultVisibility = user.defaultVisibility.name
    )
}

private fun LocalAccount.toUser(): User {
    val startDate = Instant.ofEpochSecond(startDateEpochSecond.coerceAtLeast(0L))
    return User(
        identifier = "local",
        name = "Local Account",
        startDate = startDate
    )
}

private fun LocalAccount.withUser(user: User): LocalAccount {
    return copy(
        startDateEpochSecond = user.startDate.epochSecond
    )
}
