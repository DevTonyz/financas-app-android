package com.antony.financasapp.core

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    // Cria (ou abre) o arquivo invisível chamado "financas_prefs"
    // O MODE_PRIVATE garante que nenhum outro aplicativo do celular consiga ler esse arquivo
    private val prefs: SharedPreferences = context.getSharedPreferences("financas_prefs", Context.MODE_PRIVATE)

    // Função para guardar o token no cofre
    fun salvarToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    // Função para buscar o token (retorna nulo se o usuário nunca tiver logado)
    fun buscarToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    // Função para quando o usuário clicar em "Sair da Conta"
    fun limparToken() {
        prefs.edit().remove("jwt_token").apply()
    }
}