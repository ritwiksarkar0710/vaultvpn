package com.vaultvpn.di

import android.content.Context
import com.google.gson.GsonBuilder
import com.vaultvpn.network.VpnApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://api.vaultvpn.com/v1/"

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext ctx: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                // Attach auth token from DataStore
                val token = "" // retrieve from DataStore/encrypted prefs
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("X-App-Version", "1.0.0")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setLenient().create()
                )
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideVpnApi(retrofit: Retrofit): VpnApiService =
        retrofit.create(VpnApiService::class.java)
}
