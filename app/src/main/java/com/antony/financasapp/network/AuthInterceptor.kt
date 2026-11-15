package com.antony.financasapp.network

import okhttp3.Interceptor
import okhttp3.Response
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
        return chain.proceed(requestBuilder.build())
    }
}