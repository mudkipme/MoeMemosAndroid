package me.mudkip.moememos.data.constant

import android.content.Context
import me.mudkip.moememos.R
import net.swiftzer.semver.SemVer

object MemosVersionSupport {
    const val MEMOS_V0_MIN_VERSION_NAME = "0.21.0"
    const val MEMOS_V1_MIN_VERSION_NAME = "0.27.0"
    const val MEMOS_V1_MAX_VERSION_NAME = "0.31.0"
    const val MEMOS_CALVER_MAX_VERSION_NAME = "26.10"

    val MEMOS_V0_MIN_VERSION = SemVer(0, 21, 0)
    val MEMOS_V1_MIN_VERSION = SemVer(0, 27, 0)
    val MEMOS_V1_MAX_VERSION = SemVer(0, 31, 0)

    // Memos switched to calendar versions (YY.MM.N) with 26.10.0 and dropped the
    // `/api/v1` prefix; MemosApiPathInterceptor handles the new paths.
    val MEMOS_CALVER_MIN_VERSION = SemVer(26, 10, 0)
    val MEMOS_CALVER_MAX_VERSION = SemVer(26, 10, 999)

    fun supportedVersionsMessage(context: Context): String {
        return context.getString(
            R.string.memos_supported_versions,
            MEMOS_V0_MIN_VERSION_NAME,
            MEMOS_V1_MIN_VERSION_NAME,
            "$MEMOS_V1_MAX_VERSION_NAME / $MEMOS_CALVER_MAX_VERSION_NAME",
        )
    }
}
