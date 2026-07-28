package com.meitu.generator.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import com.meitu.generator.data.remote.GitHubService
import com.meitu.generator.data.remote.DeepSeekBalanceService
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

    // ============ 加密存储 ============
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

    // ============ Gson（含自定义序列化器） ============
    @Provides
    @Singleton
    @Named("openaiGson")
    fun provideOpenAIGson(): com.google.gson.Gson {
        return GsonBuilder()
            .registerTypeAdapter(OpenAIMessage::class.java, OpenAIMessageSerializer())
            .registerTypeAdapter(OpenAIMessage::class.java, OpenAIMessageDeserializer())
            .create()
    }

    // ============ 基础 OkHttpClient ============
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ============ DeepSeek ============
    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("deepseekService")
    fun provideDeepSeekService(@Named("deepseek") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ DeepSeek Balance Service ============
    @Provides
    @Singleton
    fun provideDeepSeekBalanceService(client: OkHttpClient): DeepSeekBalanceService {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekBalanceService::class.java)
    }

    // ============ Google AI (Gemini) ============
    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.GOOGLE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("googleService")
    fun provideGoogleService(@Named("google") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ OpenAI ============
    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAIRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.OPENAI_REAL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("openaiService")
    fun provideOpenAIService(@Named("openai") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ Groq ============
    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.GROQ_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("groqService")
    fun provideGroqService(@Named("groq") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ SiliconFlow (硅基流动) ============
    @Provides
    @Singleton
    @Named("siliconflow")
    fun provideSiliconFlowRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.SILICONFLOW_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("siliconflowService")
    fun provideSiliconFlowService(@Named("siliconflow") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ Moonshot (Kimi) ============
    @Provides
    @Singleton
    @Named("moonshot")
    fun provideMoonshotRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.MOONSHOT_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("moonshotService")
    fun provideMoonshotService(@Named("moonshot") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ Zhipu AI (智谱) ============
    @Provides
    @Singleton
    @Named("zhipu")
    fun provideZhipuRetrofit(client: OkHttpClient, @Named("openaiGson") gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.ZHIPU_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("zhipuService")
    fun provideZhipuService(@Named("zhipu") retrofit: Retrofit): OpenAIService {
        return retrofit.create(OpenAIService::class.java)
    }

    // ============ GitHub API ============
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
}
