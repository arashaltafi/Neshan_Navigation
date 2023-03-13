package com.arash.neshan.test2.data.di

import com.arash.neshan.test2.data.network.ApiClient
import com.arash.neshan.test2.data.network.RetrofitConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideApiClient(retrofitConfig: RetrofitConfig): ApiClient {
        retrofitConfig.initialize()

        return retrofitConfig.createService(ApiClient::class.java)

    }

}