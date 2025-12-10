package com.example.dresscode.di

import android.content.Context
import com.example.dresscode.BuildConfig
import com.example.dresscode.data.local.db.DressCodeDatabase
import com.example.dresscode.data.remote.OutfitApiService
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.SettingsRepository
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
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import androidx.room.Room

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    @Named("weatherBaseUrl")
    fun provideWeatherBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    @Named("apiBaseUrl")
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideWeatherRepository(
        client: OkHttpClient,
        @Named("weatherBaseUrl") baseUrl: String
    ): WeatherRepository = WeatherRepository(client, baseUrl)

    @Provides
    @Singleton
    fun provideOutfitRepository(
        api: OutfitApiService,
        database: DressCodeDatabase,
        userRepository: UserRepository,
        settingsRepository: SettingsRepository
    ): OutfitRepository = OutfitRepository(api, database, userRepository, settingsRepository)

    @Provides
    @Singleton
    fun provideTryOnRepository(
        client: OkHttpClient,
        @Named("apiBaseUrl") baseUrl: String
    ): TryOnRepository = TryOnRepository(client, baseUrl)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .callTimeout(360, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaggingRepository(
        client: OkHttpClient,
        settingsRepository: SettingsRepository
    ): TaggingRepository =
        TaggingRepository(client, BuildConfig.TAGGING_BASE_URL, settingsRepository)

    @Provides
    @Singleton
    @Named("authBaseUrl")
    fun provideAuthBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideUserRepository(
        @ApplicationContext context: Context,
        client: OkHttpClient,
        @Named("authBaseUrl") authBaseUrl: String
    ): UserRepository = UserRepository(context, client, authBaseUrl)

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        moshi: Moshi,
        @Named("apiBaseUrl") baseUrl: String
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideOutfitApiService(retrofit: Retrofit): OutfitApiService =
        retrofit.create(OutfitApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DressCodeDatabase =
        Room.databaseBuilder(context, DressCodeDatabase::class.java, "dresscode.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}
