package me.mudkip.moememos.ext

import me.mudkip.moememos.MoeMemosApp
import me.mudkip.moememos.util.AppLanguage

/**
 * Get the string resources by the R.string.xx.string
 *
 * Resolved against a Context that follows the in-app language selection, so a
 * language switch takes effect without restarting the process.
 * @author Xeu<thankrain@qq.com>
 */
val Int.string get() = AppLanguage.localized(MoeMemosApp.CONTEXT).getString(this)