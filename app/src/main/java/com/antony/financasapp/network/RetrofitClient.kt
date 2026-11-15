package com.antony.financasapp.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.antony.financasapp.core.SessionManager

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Agora o nosso cliente é gerado sob demanda, passando pelo Interceptador
    fun getApiService(context: Context): ApiService {
        val sessionManager = SessionManager(context)
        val authInterceptor = AuthInterceptor(sessionManager)

        // Criamos o cliente HTTP e adicionamos o nosso interceptador nele
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // <-- Aqui nós conectamos o OkHttp configurado ao Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}