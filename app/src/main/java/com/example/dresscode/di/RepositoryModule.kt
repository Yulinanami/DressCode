package com.example.dresscode.di

import android.content.Context
import com.example.dresscode.BuildConfig
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.TaggingRepository
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository = WeatherRepository()

    @Provides
    @Singleton
    fun provideOutfitRepository(): OutfitRepository = OutfitRepository()

    @Provides
    @Singleton
    fun provideTryOnRepository(): TryOnRepository = TryOnRepository()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaggingRepository(client: OkHttpClient): TaggingRepository =
        TaggingRepository(client, BuildConfig.TAGGING_BASE_URL)

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context
    ): UserRepository = UserRepository(context)
}
