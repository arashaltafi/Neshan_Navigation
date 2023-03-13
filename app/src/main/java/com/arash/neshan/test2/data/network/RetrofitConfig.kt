package com.arash.neshan.test2.data.network

import com.arash.neshan.test2.BuildConfig
import com.arash.neshan.test2.data.AppConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
open class RetrofitConfig @Inject constructor() {
    protected val builder: Retrofit.Builder by lazy {
        getRetrofitBuilder()
    }
    protected val httpClient: OkHttpClient.Builder by lazy {
        getHttpClientBuilder()
    }

    fun initialize() {
        // add main interceptor
        httpClient.addInterceptor(getMainInterceptor())

        // add logger interceptor
        httpClient.addInterceptor(getLogger())
    }

    private fun isLogEnabled(): Boolean {
        return BuildConfig.DEBUG
    }

    fun getBaseUrl(): String {
        return AppConfig.API_URL
    }

    /**
     * initialize OkHttpClient instance and setup configurations like connection timeout etc.
     * */
    open fun getRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(getConverterFactory())
            .client(httpClient.build())
    }

    /**
     * initialize OkHttpClient instance and setup configurations like connection timeout etc.
     * */
    open fun getHttpClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
    }

    /**
     * initialize main interceptor
     * */
    open fun getMainInterceptor(): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()

            // add required headers
            getHeaders()?.forEach { header ->
                requestBuilder.addHeader(header.first, header.second)
            }

            val request: Request = requestBuilder.build()

            val response = chain.proceed(request)

            onResponse(response)

            response
        }
    }

    /**
     * initialize logger interceptor
     * */
    open fun getLogger(): Interceptor {
        return if (isLogEnabled()) HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        } else HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * initialize converter factory
     * */
    open fun getConverterFactory(): Converter.Factory {
        val gson: Gson = GsonBuilder().setLenient().create()
        return GsonConverterFactory.create(gson)
    }

    /**
     * override onResponse() to do actions before upper layer observers be triggered.
     * (ie, check if response.code() == 401 then Authentication required)
     * */
    open fun onResponse(response: Response) {
    }

    /**
     * override this method and initialize your headers if necessary
     * */
    open fun getHeaders(): List<Pair<String, String>>? {

        val headers = mutableListOf<Pair<String, String>>()
        headers.add(Pair("Api-Key", "service.934a1ea696064b40952db4a293efe56d"))

        return headers

    }

    /**
     * create Service for a retrofit interface
     * */
    fun <T> createService(serviceClass: Class<T>): T {
        val retrofit = builder.build()
        return retrofit.create(serviceClass)
    }

}