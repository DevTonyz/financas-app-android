package com.antony.financasapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.antony.financasapp.core.SessionManager

object RetrofitClient {

    // 10.0.2.2 é o "localhost" do PC visto de dentro do emulador
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Monta o cliente da API. É chamado uma única vez, pela FinancasApplication.
    fun criarApiService(sessionManager: SessionManager): ApiService {
        // Cliente HTTP com o nosso interceptador, que coloca o token em toda requisição
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
