package com.antony.financasapp.network

import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/login") // A rota exata que existe no seu Spring Boot
    suspend fun fazerLogin(@Body request: LoginRequest): Response<LoginResponse>

}