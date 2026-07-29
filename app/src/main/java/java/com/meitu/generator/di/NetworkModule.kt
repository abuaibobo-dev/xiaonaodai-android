package com.meitu.generator.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meitu.generator.data.remote.CozeApiClient
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("securePrefs")
    fun provideSecurePrefs(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // 流式响应需要更长超时
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCozeApiClient(
        @Named("securePrefs") securePrefs: SharedPreferences,
        settingsRepo: SettingsRepository,
        httpClient: OkHttpClient
    ): CozeApiClient {
        val pat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
        val botId = runBlocking { settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "") }
        return CozeApiClient(
            baseUrl = Constants.COZE_API_BASE_URL,
            pat = pat,
            botId = botId,
            httpClient = httpClient
        )
    }
}
