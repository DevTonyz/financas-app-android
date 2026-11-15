package com.antony.financasapp

import android.app.Application
import com.antony.financasapp.core.SessionManager
import com.antony.financasapp.data.AuthRepository
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.network.ApiService
import com.antony.financasapp.network.RetrofitClient

/**
 * Classe que o Android cria uma única vez, quando o app abre (registrada no AndroidManifest).
 * Aqui montamos as peças compartilhadas do app, uma vez só, e as telas pegam daqui.
 * Antes, cada clique criava um Retrofit novo; agora todos usam o mesmo.
 */
class FinancasApplication : Application() {

    val sessionManager by lazy { SessionManager(this) }

    val api: ApiService by lazy { RetrofitClient.criarApiService(sessionManager) }

    val authRepository by lazy { AuthRepository(api, sessionManager) }

    val transacaoRepository by lazy { TransacaoRepository(api) }
}
