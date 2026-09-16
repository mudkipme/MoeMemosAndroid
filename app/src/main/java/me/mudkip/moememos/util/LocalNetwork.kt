package me.mudkip.moememos.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress
import java.net.UnknownHostException

internal fun isLocalNetworkAddress(address: InetAddress): Boolean {
    // IPv6 unique-local addresses (fc00::/7) are not covered by isSiteLocalAddress.
    val bytes = address.address
    return !address.isLoopbackAddress && (
        address.isSiteLocalAddress || address.isLinkLocalAddress ||
            (bytes.size == 16 && (bytes[0].toInt() and 0xfe) == 0xfc)
    )
}

internal suspend fun usesLocalNetwork(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
    val host = serverUrl.toHttpUrlOrNull()?.host ?: return@withContext false
    if (host.trimEnd('.').endsWith(".local", ignoreCase = true)) {
        // mDNS resolution itself requires the permission.
        return@withContext true
    }
    try {
        InetAddress.getAllByName(host).any(::isLocalNetworkAddress)
    } catch (_: UnknownHostException) {
        // Let the normal connection flow report DNS failures.
        false
    }
}
