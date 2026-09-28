package com.example.data.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
  const val DEFAULT_BASE_URL = "https://ais-dev-2ljvajswfzzevis7aqilho-382375357313.asia-southeast1.run.app/api/v1/"
  private var currentBaseUrl = DEFAULT_BASE_URL

  private var apiInstance: PixelPathsalaApi? = null
  var authToken: String? = null

  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder = original.newBuilder()
    authToken?.let { token ->
      if (token.isNotBlank()) {
        builder.header("Authorization", "Bearer $token")
      }
    }
    chain.proceed(builder.build())
  }

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .addInterceptor(authInterceptor)
    .addInterceptor(HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

  fun init(context: Context) {
    getApi()
  }

  fun getBaseUrl(): String = currentBaseUrl

  fun setBaseUrl(newUrl: String, context: Context? = null) {
    var formatted = newUrl.trim()
    if (!formatted.endsWith("/")) {
      formatted += "/"
    }
    currentBaseUrl = formatted
    apiInstance = null
    getApi()
  }

  fun getApi(): PixelPathsalaApi {
    if (apiInstance == null) {
      apiInstance = Retrofit.Builder()
        .baseUrl(currentBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PixelPathsalaApi::class.java)
    }
    return apiInstance!!
  }
}
