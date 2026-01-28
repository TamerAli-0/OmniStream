package com.omnistream.di

import android.content.Context
import com.omnistream.core.network.OmniHttpClient
import com.omnistream.source.SourceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOmniHttpClient(): OmniHttpClient {
        return OmniHttpClient()
    }

    @Provides
    @Singleton
    fun provideSourceManager(
        @ApplicationContext context: Context,
        httpClient: OmniHttpClient
    ): SourceManager {
        return SourceManager(context, httpClient)
    }
}
