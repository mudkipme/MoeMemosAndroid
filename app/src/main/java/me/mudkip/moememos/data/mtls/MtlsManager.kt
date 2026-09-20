package me.mudkip.moememos.data.mtls

import android.app.Activity
import android.content.Context
import android.security.KeyChain
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManagerFactory
import okhttp3.ConnectionPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MtlsManager {

    private const val PREFS = "mtls"
    private const val ALIAS = "client_certificate_alias"

    /*
     * The OkHttp connection pool is registered here when the
     * network client is created.
     *
     * When the certificate changes, existing connections are
     * evicted so the next request performs a fresh TLS handshake.
     */
    private val connectionPool =
        AtomicReference<ConnectionPool?>(null)
    private val keyManager =
        AtomicReference<DynamicKeyChainKeyManager?>(null)

    fun registerConnectionPool(
        pool: ConnectionPool
    ) {
        connectionPool.set(pool)
    }

    private fun evictConnections() {
        CoroutineScope(Dispatchers.IO).launch {
            connectionPool.get()?.evictAll()
        }
    }

    /**
     * Returns true when a client certificate alias has been selected.
     *
     * Only the alias is stored by the app.
     * The private key remains inside Android KeyChain.
     */
    fun hasSelectedCertificate(
        context: Context
    ): Boolean {
        return getSelectedAlias(context) != null
    }

    /**
     * Opens Android's system client-certificate selector.
     */
    fun chooseCertificate(
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        KeyChain.choosePrivateKeyAlias(
            activity,
            { alias ->
                if (alias == null) {
                    onComplete(false)
                    return@choosePrivateKeyAlias
                }

                activity
                    .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                    )
                    .edit()
                    .putString(
                        ALIAS,
                        alias
                    )
                    .apply()
                keyManager.get()?.clearCache()
                /*
                 * Existing HTTP/1.1 and HTTP/2 connections may have
                 * been created using the previous certificate state.
                 * Evict them so the next request gets a fresh
                 * TLS handshake.
                 */
                evictConnections()

                onComplete(true)
            },
            null,
            null,
            null,
            -1,
            null
        )
    }

    /**
     * Clears the selected certificate alias.
     *
     * This does not delete anything from Android KeyChain.
     */
    fun clearSelectedCertificate(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(ALIAS)
            .apply()
        keyManager.get()?.clearCache()
        /*
         * Prevent an existing authenticated connection from being
         * reused after the certificate has been removed.
         */
        evictConnections()
    }

    /**
     * Creates the TLS configuration used by OkHttp.
     *
     * Android's normal TrustManager is retained.
     * Only the client-side KeyManager is replaced so that
     * Android KeyChain can supply the client certificate.
     */
    fun createSslContext(
        context: Context
    ): Pair<SSLContext, X509TrustManager> {

        val appContext =
            context.applicationContext

        val dynamicKeyManager =
            keyManager.updateAndGet { existing ->
                existing ?: DynamicKeyChainKeyManager(appContext)
            }

        val trustManagerFactory =
            TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )

        /*
         * null means use the platform's normal trusted CA store.
         */
        trustManagerFactory.init(
            null as java.security.KeyStore?
        )

        val trustManager =
            trustManagerFactory.trustManagers
                .filterIsInstance<X509TrustManager>()
                .single()

        val sslContext =
            SSLContext.getInstance("TLS")

        sslContext.init(
            arrayOf(dynamicKeyManager),
            arrayOf(trustManager),
            null
        )

        return sslContext to trustManager
    }

    /**
     * X509ExtendedKeyManager backed by Android KeyChain.
     */
    private class DynamicKeyChainKeyManager(
        private val context: Context
    ) : X509ExtendedKeyManager() {

        private val lock =
            Any()

        private var cachedAlias:
                String? = null

        private var cachedPrivateKey:
                PrivateKey? = null

        private var cachedCertificateChain:
                Array<X509Certificate>? = null

        /**
         * Read the currently selected alias.
         */
        private fun selectedAlias():
                String? {
            return getSelectedAlias(
                context
            )
        }

        /**
         * If the user selected a different certificate,
         * discard the cached KeyChain material.
         */
        fun clearCache() {
            synchronized(lock) {
                cachedAlias = null
                cachedPrivateKey = null
                cachedCertificateChain = null
            }
        }

        private fun updateCacheIfAliasChanged(
            alias: String?
        ) {
            synchronized(lock) {
                if (cachedAlias != alias) {
                    cachedAlias = alias
                    cachedPrivateKey = null
                    cachedCertificateChain = null
                }
            }
        }

        /**
         * Retrieve the private key from Android KeyChain.
         *
         * This method is invoked by the TLS key-manager path.
         */
        private fun privateKey(
            alias: String
        ): PrivateKey? {
            synchronized(lock) {
                if (
                    cachedAlias == alias &&
                    cachedPrivateKey != null
                ) {
                    return cachedPrivateKey
                }
            }

            val key =
                KeyChain.getPrivateKey(
                    context,
                    alias
                )

            synchronized(lock) {
                if (cachedAlias == alias) {
                    cachedPrivateKey = key
                }
            }

            return key
        }

        /**
         * Retrieve the certificate chain from Android KeyChain.
         */
        private fun certificateChain(
            alias: String
        ): Array<X509Certificate>? {
            synchronized(lock) {
                if (
                    cachedAlias == alias &&
                    cachedCertificateChain != null
                ) {
                    return cachedCertificateChain
                }
            }

            val chain =
                KeyChain.getCertificateChain(
                    context,
                    alias
                )

            synchronized(lock) {
                if (cachedAlias == alias) {
                    cachedCertificateChain = chain
                }
            }

            return chain
        }

        override fun chooseClientAlias(
            keyType: Array<String>?,
            issuers: Array<Principal>?,
            socket: Socket?
        ): String? {
            val alias =
                selectedAlias()

            updateCacheIfAliasChanged(
                alias
            )

            return alias
        }

        override fun chooseEngineClientAlias(
            keyType: Array<String>?,
            issuers: Array<Principal>?,
            engine: SSLEngine?
        ): String? {
            val alias =
                selectedAlias()

            updateCacheIfAliasChanged(
                alias
            )

            return alias
        }

        override fun getClientAliases(
            keyType: String?,
            issuers: Array<Principal>?
        ): Array<String>? {
            val alias =
                selectedAlias()
                    ?: return null

            updateCacheIfAliasChanged(
                alias
            )

            return arrayOf(alias)
        }

        override fun getPrivateKey(
            alias: String?
        ): PrivateKey? {
            if (alias == null) {
                return null
            }

            val selected =
                selectedAlias()

            if (alias != selected) {
                return null
            }

            updateCacheIfAliasChanged(
                alias
            )

            return privateKey(
                alias
            )
        }

        override fun getCertificateChain(
            alias: String?
        ): Array<X509Certificate>? {
            if (alias == null) {
                return null
            }

            val selected =
                selectedAlias()

            if (alias != selected) {
                return null
            }

            updateCacheIfAliasChanged(
                alias
            )

            return certificateChain(
                alias
            )
        }

        override fun chooseServerAlias(
            keyType: String?,
            issuers: Array<Principal>?,
            socket: Socket?
        ): String? {
            return null
        }

        override fun chooseEngineServerAlias(
            keyType: String?,
            issuers: Array<Principal>?,
            engine: SSLEngine?
        ): String? {
            return null
        }

        override fun getServerAliases(
            keyType: String?,
            issuers: Array<Principal>?
        ): Array<String>? {
            return null
        }
    }

    private fun getSelectedAlias(
        context: Context
    ): String? {
        return context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .getString(
                ALIAS,
                null
            )
    }
}
