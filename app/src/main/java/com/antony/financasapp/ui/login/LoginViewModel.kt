package com.antony.financasapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.AuthRepository
import com.antony.financasapp.data.ResultadoAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Tudo o que a tela de login precisa mostrar, num objeto só
data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    val erro: String? = null,
    val entrou: Boolean = false
)

/**
 * O ViewModel guarda o estado e a lógica da tela. A tela (Composable) só desenha
 * o estado e avisa quando o usuário faz algo. Assim o estado não se perde ao girar o celular.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun aoMudarEmail(valor: String) = _uiState.update { it.copy(email = valor, erro = null) }

    fun aoMudarSenha(valor: String) = _uiState.update { it.copy(senha = valor, erro = null) }

    fun entrar() {
        val estado = _uiState.value
        if (estado.carregando) return
        if (estado.email.isBlank() || estado.senha.isBlank()) {
            _uiState.update { it.copy(erro = "Preencha e-mail e senha.") }
            return
        }

        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.entrar(estado.email.trim(), estado.senha)) {
                ResultadoAuth.Sucesso -> _uiState.update { it.copy(carregando = false, entrou = true) }
                is ResultadoAuth.Erro -> _uiState.update { it.copy(carregando = false, erro = resultado.mensagem) }
            }
        }
    }
}
