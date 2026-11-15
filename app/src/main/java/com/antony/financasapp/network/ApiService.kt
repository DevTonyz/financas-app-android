package com.antony.financasapp.network

import com.antony.financasapp.dto.CadastroRequest
import com.antony.financasapp.dto.CadastroResponse
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.dto.LoginResponse
import com.antony.financasapp.dto.PaginaResponse
import com.antony.financasapp.dto.ResumoResponse
import com.antony.financasapp.dto.TransacaoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/login") // A rota exata que existe no seu Spring Boot
    suspend fun fazerLogin(@Body request: LoginRequest): Response<LoginResponse>

    // 201 = criado | 400 = dados inválidos | 409 = e-mail já cadastrado
    @POST("/usuarios")
    suspend fun cadastrar(@Body request: CadastroRequest): Response<CadastroResponse>

    // Total gasto no mês. @Query vira "?mes=11&ano=2026" na URL
    @GET("/transacoes/resumo")
    suspend fun resumoDoMes(
        @Query("mes") mes: Int,
        @Query("ano") ano: Int
    ): Response<ResumoResponse>

    // Transações entre duas datas. Aqui a data ENTRA em "yyyy-MM-dd"
    @GET("/transacoes/periodo")
    suspend fun transacoesPorPeriodo(
        @Query("inicio") inicio: String,
        @Query("fim") fim: String,
        @Query("page") pagina: Int = 0,
        @Query("size") tamanho: Int = 10,
        @Query("sort") ordem: String = "data,desc"
    ): Response<PaginaResponse<TransacaoResponse>>
}
