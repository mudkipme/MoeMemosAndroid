package me.mudkip.moememos.ext

import me.mudkip.moememos.MoeMemosApp

/**
 * Get the string resources by the R.string.xx.string
 *
 * To support i18n
 * @author Xeu<thankrain@qq.com>
 */
val Int.string get() = MoeMemosApp.CONTEXT.getString(this)