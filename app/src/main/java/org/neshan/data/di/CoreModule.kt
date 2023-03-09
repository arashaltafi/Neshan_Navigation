package org.neshan.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.neshan.data.network.ApiClient
import org.neshan.data.network.RetrofitConfig
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