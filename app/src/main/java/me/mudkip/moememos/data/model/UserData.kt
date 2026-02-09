package me.mudkip.moememos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val settings: UserSettings = UserSettings(),
    val accountKey: String = "",
    val memosV0: MemosAccount? = null,
    val memosV1: MemosAccount? = null,
    val local: LocalAccount? = null,
) {
    enum class AccountCase {
        MEMOS_V0,
        MEMOS_V1,
        LOCAL,
        ACCOUNT_NOT_SET,
    }

    val accountCase: AccountCase
        get() = when {
            memosV0 != null -> AccountCase.MEMOS_V0
            memosV1 != null -> AccountCase.MEMOS_V1
            local != null -> AccountCase.LOCAL
            else -> AccountCase.ACCOUNT_NOT_SET
        }
}
