package me.mudkip.moememos.data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gotev.cookiestore.SharedPreferencesCookieStore
import net.gotev.cookiestore.WebKitSyncCookieManager
import net.gotev.cookiestore.okhttp.JavaNetCookieJar
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.CookiePolicy

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cookieManager = WebKitSyncCookieManager(
            store = SharedPreferencesCookieStore(context, "cookies"),
            cookiePolicy = CookiePolicy.ACCEPT_ALL
        )

        CookieManager.setDefault(cookieManager)

        return OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()
    }
}