package ru.vasiliev.sandbox.network

import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient
import ru.vasiliev.sandbox.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
object ClientFactory {

    val defaultOkHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .connectTimeout(BuildConfig.OKHTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(BuildConfig.OKHTTP_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(BuildConfig.OKHTTP_READ_TIMEOUT_SEC, TimeUnit.SECONDS).build()

    val okHttpClientWithStetho: OkHttpClient
        get() = OkHttpClient.Builder()
            .connectTimeout(BuildConfig.OKHTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(BuildConfig.OKHTTP_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(BuildConfig.OKHTTP_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addNetworkInterceptor(StethoInterceptor()).build()
}
