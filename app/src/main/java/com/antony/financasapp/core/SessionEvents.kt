package com.antony.financasapp.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// "Alto-falante" do app: avisa as telas quando a sessão termina
// (ex.: o token venceu e a API respondeu 401). Quem escuta é a MainActivity,
// que leva o usuário de volta para o login.
object SessionEvents {

    private val _sessaoExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessaoExpirada: SharedFlow<Unit> = _sessaoExpirada.asSharedFlow()

    // tryEmit não suspende, então pode ser chamado de dentro do interceptor (que não é coroutine)
    fun avisarSessaoExpirada() {
        _sessaoExpirada.tryEmit(Unit)
    }
}
