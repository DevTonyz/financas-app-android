package com.antony.financasapp.data

import com.antony.financasapp.dto.PaginaResponse
import com.antony.financasapp.dto.TransacaoRequest
import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Response
import java.math.BigDecimal
import java.util.Calendar

// Qual lista buscar. A API tem uma rota para cada filtro, então só um vale por vez
sealed interface FiltroTransacoes {
    data object Todas : FiltroTransacoes
    data class PorCategoria(val id: Long, val nome: String) : FiltroTransacoes
    // Datas em "yyyy-MM-dd", que é como a API recebe
    data class PorPeriodo(val inicio: String, val fim: String) : FiltroTransacoes
}

/**
 * Fala com a API sobre transações. Devolve resultados simples (Sucesso/Erro),
 * igual ao AuthRepository, para o ViewModel não lidar com HTTP.
 */
class TransacaoRepository(private val api: ApiService) {

    // Muda de valor a cada criar/editar/excluir. O painel e a lista escutam isso para se
    // recarregar sozinhos, sem que uma tela precise avisar a outra
    private val _mudancas = MutableStateFlow(0)
    val mudancas: StateFlow<Int> = _mudancas.asStateFlow()

    // Total + últimas transações de um mês (mes de 1 a 12)
    suspend fun buscarMes(mes: Int, ano: Int): Resultado<DadosDoMes> {
        return try {
            val resumo = api.resumoDoMes(mes, ano)
            val inicio = String.format("%04d-%02d-01", ano, mes)
            val fim = String.format("%04d-%02d-%02d", ano, mes, ultimoDiaDoMes(mes, ano))
            val lista = api.transacoesPorPeriodo(inicio, fim)

            when {
                resumo.isSuccessful && lista.isSuccessful -> Resultado.Sucesso(
                    DadosDoMes(
                        total = resumo.body()?.totalGasto ?: BigDecimal.ZERO,
                        transacoes = lista.body()?.content.orEmpty()
                    )
                )
                // 401 já é tratado pelo AuthInterceptor, que leva ao login
                else -> Resultado.Erro("Erro no servidor (${if (!resumo.isSuccessful) resumo.code() else lista.code()}).")
            }
        } catch (e: Exception) {
            Resultado.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }

    // Uma página da lista (pagina começa em 0)
    suspend fun listar(filtro: FiltroTransacoes, pagina: Int): Resultado<PaginaResponse<TransacaoResponse>> =
        chamar({
            when (filtro) {
                FiltroTransacoes.Todas -> api.listarTransacoes(pagina)
                is FiltroTransacoes.PorCategoria -> api.transacoesPorCategoria(filtro.id, pagina)
                is FiltroTransacoes.PorPeriodo -> api.transacoesPorPeriodo(filtro.inicio, filtro.fim, pagina)
            }
        }) { it.body()!! }

    suspend fun buscar(id: Long): Resultado<TransacaoResponse> =
        chamar({ api.buscarTransacao(id) }) { it.body()!! }

    // Devolve quantas transações a API criou (mais de 1 no crédito parcelado)
    suspend fun criar(request: TransacaoRequest): Resultado<Int> =
        chamar({ api.criarTransacao(request) }, avisaMudanca = true) { it.body().orEmpty().size }

    suspend fun atualizar(id: Long, request: TransacaoRequest): Resultado<TransacaoResponse> =
        chamar({ api.atualizarTransacao(id, request) }, avisaMudanca = true) { it.body()!! }

    suspend fun excluir(id: Long): Resultado<Unit> =
        chamar({ api.excluirTransacao(id) }, avisaMudanca = true) { }

    // Faz a chamada e trata os erros num lugar só (mesma ideia do CategoriaRepository)
    private suspend fun <R, T> chamar(
        requisicao: suspend () -> Response<R>,
        avisaMudanca: Boolean = false,
        aoDarCerto: (Response<R>) -> T
    ): Resultado<T> {
        return try {
            val resposta = requisicao()
            when {
                resposta.isSuccessful -> {
                    if (avisaMudanca) _mudancas.update { it + 1 }
                    Resultado.Sucesso(aoDarCerto(resposta))
                }
                // 404 e 403 trazem a mensagem no corpo (ex.: "Categoria não encontrada")
                resposta.code() == 404 || resposta.code() == 403 -> Resultado.Erro(
                    resposta.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Transação não encontrada."
                )
                resposta.code() == 400 -> Resultado.Erro("Dados inválidos. Confira os campos.")
                // 401 já é tratado pelo AuthInterceptor, que leva ao login
                else -> Resultado.Erro("Erro no servidor (${resposta.code()}). Tente de novo.")
            }
        } catch (e: Exception) {
            Resultado.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }

    // Usamos Calendar (e não java.time) porque o app roda a partir do Android 7 (API 24)
    private fun ultimoDiaDoMes(mes: Int, ano: Int): Int =
        Calendar.getInstance().apply { set(ano, mes - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

data class DadosDoMes(
    val total: BigDecimal,
    val transacoes: List<TransacaoResponse>
)

// Resultado genérico para os próximos repositórios: <T> é o tipo do dado que volta no sucesso
sealed interface Resultado<out T> {
    data class Sucesso<T>(val dados: T) : Resultado<T>
    data class Erro(val mensagem: String) : Resultado<Nothing>
}
