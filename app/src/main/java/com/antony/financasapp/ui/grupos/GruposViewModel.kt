package com.antony.financasapp.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.GrupoRepository
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.dto.GrupoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GruposUiState(
    val carregando: Boolean = true,
    val erro: String? = null,
    val grupos: List<GrupoResponse> = emptyList(),
    // Diálogo "Novo grupo"
    val criando: Boolean = false,
    val nome: String = "",
    val descricao: String = "",
    val salvando: Boolean = false,
    val erroDialogo: String? = null
)

class GruposViewModel(private val repository: GrupoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GruposUiState())
    val uiState: StateFlow<GruposUiState> = _uiState.asStateFlow()

    init {
        carregar()
        // Criou um grupo aqui ou saiu de um na tela do grupo? Recarrega a lista sozinha
        viewModelScope.launch { repository.mudancas.drop(1).collect { carregar() } }
    }

    fun carregar() {
        _uiState.update { it.copy(carregando = it.grupos.isEmpty(), erro = null) }
        viewModelScope.launch {
            when (val r = repository.listar()) {
                is Resultado.Sucesso -> _uiState.update { it.copy(carregando = false, grupos = r.dados) }
                is Resultado.Erro -> _uiState.update { it.copy(carregando = false, erro = r.mensagem) }
            }
        }
    }

    fun abrirNovo() = _uiState.update { it.copy(criando = true, nome = "", descricao = "", erroDialogo = null) }

    fun fecharNovo() = _uiState.update { it.copy(criando = false) }

    fun aoMudarNome(texto: String) = _uiState.update { it.copy(nome = texto.take(60), erroDialogo = null) }

    fun aoMudarDescricao(texto: String) = _uiState.update { it.copy(descricao = texto.take(255)) }

    fun criar() {
        val estado = _uiState.value
        if (estado.salvando || estado.nome.isBlank()) return
        _uiState.update { it.copy(salvando = true, erroDialogo = null) }
        viewModelScope.launch {
            when (val r = repository.criar(estado.nome, estado.descricao)) {
                // A lista recarrega pelo "mudancas"; aqui só fechamos o diálogo
                is Resultado.Sucesso -> _uiState.update { it.copy(salvando = false, criando = false) }
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, erroDialogo = r.mensagem) }
            }
        }
    }
}
