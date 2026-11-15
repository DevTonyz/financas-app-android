package com.antony.financasapp.network

import okhttp3.Interceptor
import okhttp3.Response
import com.antony.financasapp.core.SessionEvents
import com.antony.financasapp.core.SessionManager

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Pega a requisição original que estava saindo do aplicativo
        val requestBuilder = chain.request().newBuilder()

        // Abre o cofre e tenta pegar o token
        val token = sessionManager.buscarToken()

        // Se o token existir (o usuário está logado), carimbamos o cabeçalho padrão do Spring Security
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // Libera a requisição modificada para seguir para o servidor
        val response = chain.proceed(requestBuilder.build())

        // Se mandamos um token e a API respondeu 401, o token venceu ou é inválido:
        // limpamos o cofre e avisamos o app para voltar ao login
        if (response.code() == 401 && token != null) {
            sessionManager.limparToken()
            SessionEvents.avisarSessaoExpirada()
        }

        return response
    }
}