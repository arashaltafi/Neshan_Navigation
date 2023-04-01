package com.arash.neshan.test2.hilt

import com.arash.neshan.test2.domain.repository.NeshanRepository
import com.arash.neshan.test2.domain.service.NeshanService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {

    @ViewModelScoped
    @Provides
    fun provideNeshanRepository(
        service: NeshanService
    ) = NeshanRepository(service)

}