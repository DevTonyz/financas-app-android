package com.antony.financasapp.data

import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.network.ApiService
import java.math.BigDecimal
import java.util.Calendar

/**
 * Fala com a API sobre transações. Devolve resultados simples (Sucesso/Erro),
 * igual ao AuthRepository, para o ViewModel não lidar com HTTP.
 */
class TransacaoRepository(private val api: ApiService) {

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
