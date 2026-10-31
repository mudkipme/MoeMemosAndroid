package me.mudkip.moememos.data.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Memos 26.10.0 (calendar versioning) dropped the `v1` marker from the REST API:
 * `/api/v1/...` became `/api/...`, and the old routes answer `410 Gone`.
 *
 * MemosV1Api keeps its `api/v1/...` annotations so Memos 0.27–0.31 keep working.
 * This interceptor detects, once per server, whether the unversioned API is in use
 * and rewrites the request paths accordingly. A `410 Gone` on a legacy path also
 * switches the server to the unversioned API and retries the request.
 */
class MemosApiPathInterceptor(host: String) : Interceptor {
    private val baseUrl: HttpUrl? = host.toHttpUrlOrNull()
    private val basePath: String = baseUrl?.encodedPath?.let { if (it.endsWith("/")) it else "$it/" } ?: "/"
    private val legacyPrefix = basePath + "api/v1/"
    private val cacheKey: String = baseUrl?.let { "${it.scheme}://${it.host}:${it.port}$basePath" } ?: host

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val base = baseUrl
        if (base == null || url.host != base.host || url.port != base.port ||
            !url.encodedPath.startsWith(legacyPrefix)
        ) {
            return chain.proceed(request)
        }

        val unversioned = unversionedServers[cacheKey] ?: probe(chain, base)?.also {
            unversionedServers[cacheKey] = it
        }
        if (unversioned == true) {
            return chain.proceed(rewrite(request))
        }

        val response = chain.proceed(request)
        if (response.code == 410) {
            response.close()
            unversionedServers[cacheKey] = true
            return chain.proceed(rewrite(request))
        }
        return response
    }

    private fun rewrite(request: Request): Request {
        val url = request.url
        val newPath = basePath + "api/" + url.encodedPath.removePrefix(legacyPrefix)
        return request.newBuilder()
            .url(url.newBuilder().encodedPath(newPath).build())
            .build()
    }

    /**
     * Returns true when `/api/instance/profile` answers with JSON and a calendar
     * version (26.x or later), false when it does not, and null on network errors
     * so the next request probes again.
     */
    private fun probe(chain: Interceptor.Chain, base: HttpUrl): Boolean? {
        val probeUrl = base.newBuilder()
            .encodedPath(basePath + "api/instance/profile")
            .query(null)
            .build()
        val probeRequest = Request.Builder()
            .url(probeUrl)
            .get()
            .header("Accept", "application/json")
            .build()
        return try {
            chain.proceed(probeRequest).use { response ->
                if (!response.isSuccessful) return false
                val contentType = response.header("Content-Type").orEmpty()
                if (!contentType.contains("json", ignoreCase = true)) return false
                val body = response.peekBody(64 * 1024).string()
                val major = VERSION_REGEX.find(body)?.groupValues?.get(1)?.toIntOrNull()
                major != null && major >= CALVER_FIRST_MAJOR
            }
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        private const val CALVER_FIRST_MAJOR = 26
        private val VERSION_REGEX = Regex("\"version\"\\s*:\\s*\"v?(\\d+)\\.")
        private val unversionedServers = ConcurrentHashMap<String, Boolean>()
    }
}
