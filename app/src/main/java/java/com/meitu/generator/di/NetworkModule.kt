package com.meitu.generator.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import com.meitu.generator.data.remote.DeepSeekBalanceService
import com.meitu.generator.data.remote.GitHubService
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.OpenAIMessage
import com.meitu.generator.data.remote.dto.OpenAIMessageDeserializer
import com.meitu.generator.data.remote.dto.OpenAIMessageSerializer
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("securePrefs")
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
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

    // ============ 自定义 Gson（支持多模态消息） ============
    @Provides
    @Singleton
    @Named("openaiGson")
    fun provideOpenAIGson(): com.google.gson.Gson {
        return GsonBuilder()
            .registerTypeAdapter(OpenAIMessage::class.java, OpenAIMessageSerializer())
            .registerTypeAdapter(OpenAIMessage::class.java, OpenAIMessageDeserializer())

            .create()
    }

    // ============ OpenAI 兼容 API（主力 AI 大脑） ============
    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAIRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIService(@Named("openai") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ GitHub API (云端编译) ============
    @Provides
    @Singleton
    @Named("github")
    fun provideGitHubRetrofit(@Named("securePrefs") securePrefs: SharedPreferences): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val githubClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(okhttp3.Interceptor { chain ->
                val token = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "").ifBlank { Constants.DEFAULT_GITHUB_TOKEN }
                val request = if (token.isNotEmpty()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Accept", "application/vnd.github+json")
                        .addHeader("X-GitHub-API-Version", "2022-11-28")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.GITHUB_API_BASE_URL)
            .client(githubClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubService(@Named("github") retrofit: Retrofit): GitHubService {
        return retrofit.create(GitHubService::class.java)
    }

    // ============ DeepSeek 余额查询 ============
    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDeepSeekBalanceService(@Named("deepseek") retrofit: Retrofit): DeepSeekBalanceService {
        return retrofit.create(DeepSeekBalanceService::class.java)
    }

    // ============ Google Gemini API ============
    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.meitu.generator.util.Constants.GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiService(@Named("gemini") retrofit: Retrofit): com.meitu.generator.data.remote.GeminiService {
        return retrofit.create(com.meitu.generator.data.remote.GeminiService::class.java)
    }
}
