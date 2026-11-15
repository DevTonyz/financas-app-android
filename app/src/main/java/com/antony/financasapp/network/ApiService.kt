package com.antony.financasapp.network

import com.antony.financasapp.dto.CadastroRequest
import com.antony.financasapp.dto.CadastroResponse
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/login") // A rota exata que existe no seu Spring Boot
    suspend fun fazerLogin(@Body request: LoginRequest): Response<LoginResponse>

    // 201 = criado | 400 = dados inválidos | 409 = e-mail já cadastrado
    @POST("/usuarios")
    suspend fun cadastrar(@Body request: CadastroRequest): Response<CadastroResponse>

}