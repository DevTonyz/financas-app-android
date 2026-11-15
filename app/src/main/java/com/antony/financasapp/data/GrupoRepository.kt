package com.antony.financasapp.data

import com.antony.financasapp.dto.AcertoResponse
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.dto.GrupoRequest
import com.antony.financasapp.dto.GrupoResponse
import com.antony.financasapp.dto.NovoMembroRequest
import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.network.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Response
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

/**
 * Fala com a API sobre grupos. Mesmo padrão do TransacaoRepository: devolve Sucesso/Erro
 * e tem um contador [mudancas] para a lista de grupos se recarregar ao criar ou sair de um.
 */
class GrupoRepository(private val api: ApiService) {

    private val _mudancas = MutableStateFlow(0)
    val mudancas: StateFlow<Int> = _mudancas.asStateFlow()

    suspend fun listar(): Resultado<List<GrupoResponse>> =
        chamar({ api.listarGrupos() }) { it.body().orEmpty() }

    suspend fun criar(nome: String, descricao: String): Resultado<GrupoResponse> =
        chamar({ api.criarGrupo(GrupoRequest(nome.trim(), descricao.trim().ifBlank { null })) }, avisaMudanca = true) {
            it.body()!!
        }

    suspend fun detalhar(id: Long): Resultado<GrupoDetalheResponse> =
        chamar({ api.detalharGrupo(id) }) { it.body()!! }

    suspend fun adicionarMembro(id: Long, email: String): Resultado<GrupoDetalheResponse> =
        chamar({ api.adicionarMembro(id, NovoMembroRequest(email.trim())) }) { it.body()!! }

    suspend fun sair(id: Long): Resultado<Unit> =
        chamar({ api.sairDoGrupo(id) }, avisaMudanca = true) { }

    // Total, gastos e acerto de um mês. As três chamadas vão em paralelo (async), em vez de uma depois da outra
    suspend fun buscarMes(id: Long, mes: Int, ano: Int): Resultado<DadosDoGrupoNoMes> = coroutineScope {
        val inicio = String.format(Locale.US, "%04d-%02d-01", ano, mes)
        val ultimoDia = Calendar.getInstance().apply { set(ano, mes - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val fim = String.format(Locale.US, "%04d-%02d-%02d", ano, mes, ultimoDia)

        val resumo = async { chamar({ api.resumoDoGrupo(id, mes, ano) }) { it.body()?.totalGasto ?: BigDecimal.ZERO } }
        val lista = async { chamar({ api.transacoesDoGrupo(id, inicio, fim) }) { it.body()?.content.orEmpty() } }
        val acerto = async { chamar({ api.acertoDoGrupo(id, mes, ano) }) { it.body()!! } }

        val r = resumo.await()
        val l = lista.await()
        val a = acerto.await()
        when {
            r is Resultado.Erro -> r
            l is Resultado.Erro -> l
            a is Resultado.Erro -> a
            else -> Resultado.Sucesso(
                DadosDoGrupoNoMes(
                    total = (r as Resultado.Sucesso).dados,
                    transacoes = (l as Resultado.Sucesso).dados,
                    acerto = (a as Resultado.Sucesso).dados
                )
            )
        }
    }

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
                // 404 (e-mail sem conta, grupo apagado), 403 (não é membro) e 409 (já é membro)
                // trazem uma mensagem pronta no corpo
                resposta.code() in listOf(403, 404, 409) -> Resultado.Erro(
                    resposta.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Grupo não encontrado."
                )
                resposta.code() == 400 -> Resultado.Erro("Dados inválidos. Confira os campos.")
                // 401 já é tratado pelo AuthInterceptor, que leva ao login
                else -> Resultado.Erro("Erro no servidor (${resposta.code()}). Tente de novo.")
            }
        } catch (e: Exception) {
            Resultado.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }
}

data class DadosDoGrupoNoMes(
    val total: BigDecimal,
    val transacoes: List<TransacaoResponse>,
    val acerto: AcertoResponse
)
