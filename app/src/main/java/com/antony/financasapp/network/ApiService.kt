package com.antony.financasapp.network

import com.antony.financasapp.dto.CadastroRequest
import com.antony.financasapp.dto.CadastroResponse
import com.antony.financasapp.dto.CategoriaRequest
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.dto.AcertoResponse
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.dto.GrupoRequest
import com.antony.financasapp.dto.GrupoResponse
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.dto.NovoMembroRequest
import com.antony.financasapp.dto.PagamentoRequest
import com.antony.financasapp.dto.PagamentoResponse
import com.antony.financasapp.dto.SaldosResponse
import com.antony.financasapp.dto.LoginResponse
import com.antony.financasapp.dto.PaginaResponse
import com.antony.financasapp.dto.ResumoResponse
import com.antony.financasapp.dto.EscopoParcelas
import com.antony.financasapp.dto.TransacaoRequest
import com.antony.financasapp.dto.TransacaoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    // Todas as transações do usuário, da mais nova para a mais antiga
    @GET("/transacoes")
    suspend fun listarTransacoes(
        @Query("page") pagina: Int = 0,
        @Query("size") tamanho: Int = 10,
        @Query("sort") ordem: String = "data,desc"
    ): Response<PaginaResponse<TransacaoResponse>>

    @GET("/transacoes/categoria/{id}")
    suspend fun transacoesPorCategoria(
        @Path("id") categoriaId: Long,
        @Query("page") pagina: Int = 0,
        @Query("size") tamanho: Int = 10,
        @Query("sort") ordem: String = "data,desc"
    ): Response<PaginaResponse<TransacaoResponse>>

    // 404 se não existe ou é de outra pessoa
    @GET("/transacoes/{id}")
    suspend fun buscarTransacao(@Path("id") id: Long): Response<TransacaoResponse>

    // Devolve uma LISTA: no crédito parcelado a API cria uma transação por mês
    @POST("/transacoes")
    suspend fun criarTransacao(@Body request: TransacaoRequest): Response<List<TransacaoResponse>>

    // O escopo só faz diferença em parcelas de uma compra (ESTA ou PROXIMAS)
    @PUT("/transacoes/{id}")
    suspend fun atualizarTransacao(
        @Path("id") id: Long,
        @Body request: TransacaoRequest,
        @Query("escopo") escopo: EscopoParcelas = EscopoParcelas.ESTA
    ): Response<TransacaoResponse>

    // Em parcelas: ESTA, PROXIMAS ou TODAS (a compra inteira)
    @DELETE("/transacoes/{id}")
    suspend fun excluirTransacao(
        @Path("id") id: Long,
        @Query("escopo") escopo: EscopoParcelas = EscopoParcelas.ESTA
    ): Response<Void>

    // Categorias: a API só devolve e altera as do usuário logado
    @GET("/categorias")
    suspend fun listarCategorias(): Response<List<CategoriaResponse>>

    @POST("/categorias")
    suspend fun criarCategoria(@Body request: CategoriaRequest): Response<CategoriaResponse>

    // @Path troca o {id} da URL pelo valor do parâmetro
    @PUT("/categorias/{id}")
    suspend fun renomearCategoria(
        @Path("id") id: Long,
        @Body request: CategoriaRequest
    ): Response<CategoriaResponse>

    // 204 = apagada | 404 = não existe (ou é de outra pessoa) | 409 = tem gastos ligados
    // Void porque a resposta de sucesso não tem corpo
    @DELETE("/categorias/{id}")
    suspend fun excluirCategoria(@Path("id") id: Long): Response<Void>

    // --- Grupos (só os de que o usuário faz parte) ---
    @GET("/grupos")
    suspend fun listarGrupos(): Response<List<GrupoResponse>>

    @POST("/grupos")
    suspend fun criarGrupo(@Body request: GrupoRequest): Response<GrupoResponse>

    // 403 se não for membro, 404 se não existe
    @GET("/grupos/{id}")
    suspend fun detalharGrupo(@Path("id") id: Long): Response<GrupoDetalheResponse>

    // 404 = e-mail sem conta | 409 = já é membro
    @POST("/grupos/{id}/membros")
    suspend fun adicionarMembro(
        @Path("id") id: Long,
        @Body request: NovoMembroRequest
    ): Response<GrupoDetalheResponse>

    // Sai do grupo; se ninguém sobrar, a API apaga o grupo
    @DELETE("/grupos/{id}/membros/me")
    suspend fun sairDoGrupo(@Path("id") id: Long): Response<Void>

    @GET("/transacoes/grupo/{id}/resumo")
    suspend fun resumoDoGrupo(
        @Path("id") id: Long,
        @Query("mes") mes: Int,
        @Query("ano") ano: Int
    ): Response<ResumoResponse>

    // Gastos do grupo num período ("yyyy-MM-dd"). size grande: a tela mostra o mês inteiro de uma vez
    @GET("/transacoes/grupo/{id}")
    suspend fun transacoesDoGrupo(
        @Path("id") id: Long,
        @Query("inicio") inicio: String,
        @Query("fim") fim: String,
        @Query("size") tamanho: Int = 100,
        @Query("sort") ordem: String = "data,desc"
    ): Response<PaginaResponse<TransacaoResponse>>

    @GET("/transacoes/grupo/{id}/acerto")
    suspend fun acertoDoGrupo(
        @Path("id") id: Long,
        @Query("mes") mes: Int,
        @Query("ano") ano: Int
    ): Response<AcertoResponse>

    // --- Acerto acumulado ---
    @GET("/grupos/{id}/saldos")
    suspend fun saldosDoGrupo(@Path("id") id: Long): Response<SaldosResponse>

    @GET("/grupos/{id}/pagamentos")
    suspend fun pagamentosDoGrupo(
        @Path("id") id: Long,
        @Query("inicio") inicio: String,
        @Query("fim") fim: String
    ): Response<List<PagamentoResponse>>

    // Só quem pagou ou quem recebeu pode registrar (senão 403)
    @POST("/grupos/{id}/pagamentos")
    suspend fun registrarPagamento(
        @Path("id") id: Long,
        @Body request: PagamentoRequest
    ): Response<PagamentoResponse>

    @DELETE("/grupos/{id}/pagamentos/{idPagamento}")
    suspend fun excluirPagamento(
        @Path("id") id: Long,
        @Path("idPagamento") idPagamento: Long
    ): Response<Void>
}
