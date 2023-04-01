package com.arash.neshan.test2.hilt

import com.arash.neshan.test2.domain.service.NeshanService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Retrofit

@Module
@InstallIn(ViewModelComponent::class)
object ServiceModule {

    @ViewModelScoped
    @Provides
    fun provideAppService(retrofit: Retrofit): NeshanService =
        retrofit.create(NeshanService::class.java)

}