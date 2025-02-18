package com.rst.mynextbart.di

import com.rst.mynextbart.network.BartService
import com.rst.mynextbart.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideBartService(): BartService {
        return RetrofitClient.create()
    }
} 