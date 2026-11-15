package com.antony.financasapp.data

import com.antony.financasapp.dto.CategoriaRequest
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.network.ApiService
import retrofit2.Response

/**
 * Fala com a API sobre categorias. Traduz os códigos HTTP em mensagens prontas para a tela.
 */
class CategoriaRepository(private val api: ApiService) {

    suspend fun listar(): Resultado<List<CategoriaResponse>> =
        chamar({ api.listarCategorias() }) { it.body().orEmpty() }

    suspend fun criar(nome: String): Resultado<CategoriaResponse> =
        chamar({ api.criarCategoria(CategoriaRequest(nome)) }) { it.body()!! }

    suspend fun renomear(id: Long, nome: String): Resultado<CategoriaResponse> =
        chamar({ api.renomearCategoria(id, CategoriaRequest(nome)) }) { it.body()!! }

    suspend fun excluir(id: Long): Resultado<Unit> =
        chamar({ api.excluirCategoria(id) }) { }

    // Faz a chamada e trata os erros num lugar só, para não repetir o mesmo try/when em cada função
    private suspend fun <R, T> chamar(
        requisicao: suspend () -> Response<R>,
        aoDarCerto: (Response<R>) -> T
    ): Resultado<T> {
        return try {
            val resposta = requisicao()
            when {
                resposta.isSuccessful -> Resultado.Sucesso(aoDarCerto(resposta))
                // A API responde 409 ao excluir uma categoria que ainda tem gastos ligados a ela
                resposta.code() == 409 -> Resultado.Erro("Esta categoria tem gastos e não pode ser excluída.")
                resposta.code() == 404 -> Resultado.Erro("Categoria não encontrada. Ela pode ter sido apagada.")
                resposta.code() == 400 -> Resultado.Erro("Nome inválido (de 1 a 40 caracteres).")
                // 401 já é tratado pelo AuthInterceptor, que leva ao login
                else -> Resultado.Erro("Erro no servidor (${resposta.code()}). Tente de novo.")
            }
        } catch (e: Exception) {
            Resultado.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }
}
