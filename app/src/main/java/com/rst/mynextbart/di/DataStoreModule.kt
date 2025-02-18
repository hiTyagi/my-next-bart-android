package com.rst.mynextbart.di

import android.content.Context
import com.rst.mynextbart.data.FavoritesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideFavoritesDataStore(
        @ApplicationContext context: Context
    ): FavoritesDataStore {
        return FavoritesDataStore(context)
    }
} 