package com.smartsolar.stations.auth.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.1.90:5205/"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val authInterceptor = Interceptor { chain ->

        val originalRequest = chain.request()

        val context = appContext

        if (context == null) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val sessionManager = SessionManager(context)

        val user = sessionManager.current()

        val token = user?.token.orEmpty()

        if (token.isBlank()) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val cleanToken = token.removePrefix("Bearer ")

        val authenticatedRequest =
            originalRequest.newBuilder()
                .addHeader(
                    "Authorization",
                    "Bearer $cleanToken"
                )
                .build()

        chain.proceed(authenticatedRequest)
    }

    private val okHttpClient: OkHttpClient by lazy {

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    val api: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}