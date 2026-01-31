package com.omnistream.di

import android.content.Context
import androidx.room.Room
import com.omnistream.core.network.OmniHttpClient
import com.omnistream.data.local.AppDatabase
import com.omnistream.data.local.DownloadDao
import com.omnistream.data.local.FavoriteDao
import com.omnistream.data.local.SearchHistoryDao
import com.omnistream.data.local.UserPreferences
import com.omnistream.data.local.WatchHistoryDao
import com.omnistream.data.remote.ApiService
import com.omnistream.data.repository.AuthRepository
import com.omnistream.data.repository.SyncRepository
import com.omnistream.data.repository.WatchHistoryRepository
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

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiService()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepository(apiService, userPreferences)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        apiService: ApiService,
        userPreferences: UserPreferences
    ): SyncRepository {
        return SyncRepository(apiService, userPreferences)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "omnistream.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryRepository(watchHistoryDao: WatchHistoryDao): WatchHistoryRepository {
        return WatchHistoryRepository(watchHistoryDao)
    }
}
