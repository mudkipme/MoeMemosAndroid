package me.mudkip.moememos.util

import java.security.MessageDigest

private const val INT53_MASK = 0x1FFFFFFFFFFFFFL

fun String.toInt53Hash(): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    var hash = 0L

    repeat(8) { index ->
        hash = (hash shl 8) or (digest[index].toLong() and 0xFF)
    }

    return hash and INT53_MASK
}
