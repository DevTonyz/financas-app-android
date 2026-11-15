package com.antony.financasapp.data

import com.antony.financasapp.core.SessionManager
import com.antony.financasapp.dto.CadastroRequest
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.network.ApiService

/**
 * Repositório = o único lugar que sabe falar com a API sobre login e conta.
 * As telas não veem Retrofit, códigos HTTP nem token: só recebem um resultado simples.
 */
class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager
) {

    fun estaLogado(): Boolean = sessionManager.buscarToken() != null

    suspend fun entrar(email: String, senha: String): ResultadoAuth {
        return try {
            val resposta = api.fazerLogin(LoginRequest(email, senha))
            val token = resposta.body()?.token
            when {
                resposta.isSuccessful && token != null -> {
                    sessionManager.salvarToken(token)
                    ResultadoAuth.Sucesso
                }
                resposta.code() == 401 -> ResultadoAuth.Erro("E-mail ou senha incorretos.")
                else -> ResultadoAuth.Erro("Erro no servidor (${resposta.code()}). Tente de novo.")
            }
        } catch (e: Exception) {
            ResultadoAuth.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }

    // Cria a conta e já entra com ela
    suspend fun cadastrar(email: String, senha: String): ResultadoAuth {
        return try {
            val resposta = api.cadastrar(CadastroRequest(email, senha))
            when (resposta.code()) {
                201 -> entrar(email, senha)
                409 -> ResultadoAuth.Erro("Este e-mail já tem uma conta. Faça login.")
                400 -> ResultadoAuth.Erro("Confira o e-mail e a senha.")
                else -> ResultadoAuth.Erro("Erro no servidor (${resposta.code()}). Tente de novo.")
            }
        } catch (e: Exception) {
            ResultadoAuth.Erro("Erro de conexão. Confira se a API está rodando.")
        }
    }

    fun sair() {
        sessionManager.limparToken()
    }
}

// "sealed" = só existem estes dois tipos de resultado; o compilador obriga a tratar os dois
sealed interface ResultadoAuth {
    data object Sucesso : ResultadoAuth
    data class Erro(val mensagem: String) : ResultadoAuth
}
