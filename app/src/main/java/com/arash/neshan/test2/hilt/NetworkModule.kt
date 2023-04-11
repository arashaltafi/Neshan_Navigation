package com.arash.neshan.test2.hilt

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideGSon(): Gson = Gson()


    @Singleton
    @Provides
    @Named("NeshanURL")
    fun provideNeshanURL(): String {
        return "https://api.neshan.org/"
    }

    @Singleton
    @Provides
    fun provideNeshanRetrofit(
        okHttpClient: OkHttpClient, gSon: Gson,
        @Named("NeshanURL") baseURL: String
    ): Retrofit {
        return Retrofit
            .Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create(gSon))
            .client(okHttpClient)
            .build()
    }

    @Singleton
    @Provides
    fun provideNeshanOkHttp() = OkHttpClient.Builder()
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder().run {
                addHeader("Api-Key", "service.****************")
                build()
            }
            chain.proceed(request)
        }
        .build()

}