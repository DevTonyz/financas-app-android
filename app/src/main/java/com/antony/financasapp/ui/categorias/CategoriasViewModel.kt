package com.antony.financasapp.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.CategoriaRepository
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.dto.CategoriaResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Qual diálogo está aberto. Fica no estado (e não na tela) para sobreviver a girar o aparelho
sealed interface DialogoCategoria {
    data class Nova(val nome: String = "") : DialogoCategoria
    data class Renomear(val id: Long, val nome: String) : DialogoCategoria
    data class Excluir(val categoria: CategoriaResponse) : DialogoCategoria
}

data class CategoriasUiState(
    val carregando: Boolean = true,
    // Erro ao carregar a lista: ocupa a tela inteira, com "Tentar de novo"
    val erro: String? = null,
    val categorias: List<CategoriaResponse> = emptyList(),
    val dialogo: DialogoCategoria? = null,
    // Esperando a API responder a um salvar/excluir: trava os botões contra toque duplo
    val salvando: Boolean = false,
    // Erro de uma ação (ex.: 409 ao excluir): aparece num aviso embaixo, sem sumir com a lista
    val aviso: String? = null
)

class CategoriasViewModel(private val repository: CategoriaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val resultado = repository.listar()) {
                is Resultado.Sucesso -> _uiState.update { it.copy(carregando = false, categorias = resultado.dados) }
                is Resultado.Erro -> _uiState.update { it.copy(carregando = false, erro = resultado.mensagem) }
            }
        }
    }

    fun abrirNova() = _uiState.update { it.copy(dialogo = DialogoCategoria.Nova()) }

    fun abrirRenomear(categoria: CategoriaResponse) =
        _uiState.update { it.copy(dialogo = DialogoCategoria.Renomear(categoria.id, categoria.nome)) }

    fun abrirExcluir(categoria: CategoriaResponse) =
        _uiState.update { it.copy(dialogo = DialogoCategoria.Excluir(categoria)) }

    fun fecharDialogo() = _uiState.update { it.copy(dialogo = null) }

    fun avisoMostrado() = _uiState.update { it.copy(aviso = null) }

    fun aoMudarNome(nome: String) = _uiState.update {
        when (val d = it.dialogo) {
            is DialogoCategoria.Nova -> it.copy(dialogo = d.copy(nome = nome))
            is DialogoCategoria.Renomear -> it.copy(dialogo = d.copy(nome = nome))
            else -> it
        }
    }

    // Botão de confirmar do diálogo aberto: criar, renomear ou excluir
    fun confirmar() {
        val dialogo = _uiState.value.dialogo ?: return
        if (_uiState.value.salvando) return

        _uiState.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val resultado: Resultado<Any> = when (dialogo) {
                is DialogoCategoria.Nova -> repository.criar(dialogo.nome.trim())
                is DialogoCategoria.Renomear -> repository.renomear(dialogo.id, dialogo.nome.trim())
                is DialogoCategoria.Excluir -> repository.excluir(dialogo.categoria.id)
            }
            when (resultado) {
                is Resultado.Sucesso -> {
                    _uiState.update { it.copy(salvando = false, dialogo = null) }
                    // Recarrega da API para a lista vir na ordem alfabética do servidor
                    atualizarListaSemSpinner()
                }
                // Fecha o diálogo e mostra o motivo (ex.: "Esta categoria tem gastos...")
                is Resultado.Erro -> _uiState.update {
                    it.copy(salvando = false, dialogo = null, aviso = resultado.mensagem)
                }
            }
        }
    }

    private suspend fun atualizarListaSemSpinner() {
        when (val resultado = repository.listar()) {
            is Resultado.Sucesso -> _uiState.update { it.copy(categorias = resultado.dados) }
            is Resultado.Erro -> _uiState.update { it.copy(aviso = resultado.mensagem) }
        }
    }
}
