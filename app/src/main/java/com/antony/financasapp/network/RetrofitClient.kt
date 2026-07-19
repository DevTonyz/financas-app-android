package com.antony.financasapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // NOVO: Essa é a variável que você vai chamar quando quiser fazer requisições
    val apiService: ApiService by lazy {
        api.create(ApiService::class.java)
    }
}