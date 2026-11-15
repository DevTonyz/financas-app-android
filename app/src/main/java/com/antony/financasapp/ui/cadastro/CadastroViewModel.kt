package com.antony.financasapp.ui.cadastro

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.AuthRepository
import com.antony.financasapp.data.ResultadoAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CadastroUiState(
    val email: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val contaCriada: Boolean = false
)

class CadastroViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState> = _uiState.asStateFlow()

    fun aoMudarEmail(valor: String) = _uiState.update { it.copy(email = valor, erro = null) }
    fun aoMudarSenha(valor: String) = _uiState.update { it.copy(senha = valor, erro = null) }
    fun aoMudarConfirmarSenha(valor: String) = _uiState.update { it.copy(confirmarSenha = valor, erro = null) }

    // Confere os dados antes de gastar uma chamada na API (null = tudo certo)
    private fun validar(estado: CadastroUiState): String? = when {
        !Patterns.EMAIL_ADDRESS.matcher(estado.email.trim()).matches() -> "Digite um e-mail válido."
        estado.senha.length < 6 -> "A senha precisa ter pelo menos 6 caracteres."
        estado.senha != estado.confirmarSenha -> "As senhas não são iguais."
        else -> null
    }

    fun criarConta() {
        val estado = _uiState.value
        if (estado.carregando) return
        validar(estado)?.let { erro ->
            _uiState.update { it.copy(erro = erro) }
            return
        }

        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.cadastrar(estado.email.trim(), estado.senha)) {
                ResultadoAuth.Sucesso -> _uiState.update { it.copy(carregando = false, contaCriada = true) }
                is ResultadoAuth.Erro -> _uiState.update { it.copy(carregando = false, erro = resultado.mensagem) }
            }
        }
    }
}
