package me.mudkip.moememos.data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.mudkip.moememos.data.mtls.MtlsManager
import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool {
        return ConnectionPool()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        connectionPool: ConnectionPool
    ): OkHttpClient {

        /*
         * Give MtlsManager access to the same connection pool
         * used by OkHttp.
         *
         * When the certificate changes, MtlsManager will call
         * connectionPool.evictAll().
         */
        MtlsManager.registerConnectionPool(
            connectionPool
        )

        val cookieManager =
            CookieManager()

        cookieManager.setCookiePolicy(
            CookiePolicy.ACCEPT_ALL
        )

        val builder =
            OkHttpClient.Builder()
                .cookieJar(
                    JavaNetCookieJar(
                        cookieManager
                    )
                )
                .connectionPool(
                    connectionPool
                )

        /*
         * Install our KeyChain-aware TLS configuration.
         *
         * The normal Android TrustManager is retained.
         */
        val (sslContext, trustManager) =
            MtlsManager.createSslContext(
                context
            )

        builder.sslSocketFactory(
            sslContext.socketFactory,
            trustManager
        )

        return builder.build()
    }
}
