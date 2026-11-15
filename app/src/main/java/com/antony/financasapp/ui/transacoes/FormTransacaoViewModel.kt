package com.antony.financasapp.ui.transacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.CategoriaRepository
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.dto.FormaPagamento
import com.antony.financasapp.dto.TransacaoRequest
import com.antony.financasapp.ui.dataDaApiParaIso
import com.antony.financasapp.ui.hojeIso
import com.antony.financasapp.ui.textoParaValor
import com.antony.financasapp.ui.valorParaTexto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class FormTransacaoUiState(
    val editando: Boolean,
    val carregando: Boolean = true,
    // Erro ao abrir (ex.: transação não existe mais): ocupa a tela toda
    val erroAoCarregar: String? = null,
    val categorias: List<CategoriaResponse> = emptyList(),
    // Campos do formulário. O valor fica como texto para o usuário poder digitar "12," sem o campo brigar
    val descricao: String = "",
    val valor: String = "",
    val data: String = hojeIso(), // sempre "yyyy-MM-dd" aqui dentro
    val categoriaId: Long? = null,
    val formaPagamento: FormaPagamento = FormaPagamento.PIX,
    val parcelas: String = "1",
    // Erros da validação, um por campo, para aparecer embaixo de cada um
    val erroDescricao: String? = null,
    val erroValor: String? = null,
    val erroCategoria: String? = null,
    val erroParcelas: String? = null,
    // Erro vindo da API ao salvar ou excluir
    val erro: String? = null,
    val salvando: Boolean = false,
    val confirmandoExclusao: Boolean = false,
    // Preenchido quando salvou/excluiu: a tela mostra a mensagem e volta
    val concluido: String? = null
)

/** [id] nulo = criar; com id = editar aquela transação. */
class FormTransacaoViewModel(
    private val id: Long?,
    private val repository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormTransacaoUiState(editando = id != null))
    val uiState: StateFlow<FormTransacaoUiState> = _uiState.asStateFlow()

    // Guardado só para não perder o grupo ao editar (a tela de grupos vem na tarefa 07)
    private var grupoId: Long? = null

    init {
        carregar()
    }

    fun carregar() {
        _uiState.update { it.copy(carregando = true, erroAoCarregar = null) }
        viewModelScope.launch {
            val categorias = categoriaRepository.listar()
            if (categorias is Resultado.Erro) {
                _uiState.update { it.copy(carregando = false, erroAoCarregar = categorias.mensagem) }
                return@launch
            }
            _uiState.update { it.copy(categorias = (categorias as Resultado.Sucesso).dados) }

            if (id == null) {
                _uiState.update { it.copy(carregando = false) }
                return@launch
            }
            when (val resultado = repository.buscar(id)) {
                is Resultado.Sucesso -> {
                    val t = resultado.dados
                    grupoId = t.grupoId
                    _uiState.update {
                        it.copy(
                            carregando = false,
                            descricao = t.descricao,
                            valor = valorParaTexto(t.valor),
                            // A API devolve "dd/MM/yyyy"; convertemos para o formato que ela recebe
                            data = dataDaApiParaIso(t.data),
                            categoriaId = t.categoriaId,
                            formaPagamento = t.formaPagamento ?: FormaPagamento.PIX,
                            parcelas = (t.parcelas ?: 1).toString()
                        )
                    }
                }
                is Resultado.Erro -> _uiState.update { it.copy(carregando = false, erroAoCarregar = resultado.mensagem) }
            }
        }
    }

    fun aoMudarDescricao(texto: String) = _uiState.update { it.copy(descricao = texto, erroDescricao = null) }

    // Só deixa digitar números, vírgula e ponto
    fun aoMudarValor(texto: String) = _uiState.update {
        if (texto.all { c -> c.isDigit() || c == ',' || c == '.' }) it.copy(valor = texto, erroValor = null) else it
    }

    fun aoMudarData(dataIso: String) = _uiState.update { it.copy(data = dataIso) }

    fun aoMudarCategoria(id: Long) = _uiState.update { it.copy(categoriaId = id, erroCategoria = null) }

    fun aoMudarFormaPagamento(forma: FormaPagamento) = _uiState.update { it.copy(formaPagamento = forma) }

    fun aoMudarParcelas(texto: String) = _uiState.update {
        if (texto.length <= 2 && texto.all(Char::isDigit)) it.copy(parcelas = texto, erroParcelas = null) else it
    }

    fun salvar() {
        val estado = _uiState.value
        if (estado.salvando) return

        // Validação antes de enviar: a API também valida, mas assim o erro aparece no campo certo
        val valor = textoParaValor(estado.valor)
        // Parcelas só valem no crédito; nas outras formas a API recebe 1
        val parcelas = if (estado.formaPagamento == FormaPagamento.CREDITO) estado.parcelas.toIntOrNull() else 1
        val erroDescricao = if (estado.descricao.isBlank()) "Informe a descrição" else null
        val erroValor = if (valor == null || valor <= BigDecimal.ZERO) "Informe um valor maior que zero" else null
        val erroCategoria = if (estado.categoriaId == null) "Escolha uma categoria" else null
        val erroParcelas = if (parcelas == null || parcelas !in 1..48) "De 1 a 48 parcelas" else null

        if (erroDescricao != null || erroValor != null || erroCategoria != null || erroParcelas != null) {
            _uiState.update {
                it.copy(
                    erroDescricao = erroDescricao, erroValor = erroValor,
                    erroCategoria = erroCategoria, erroParcelas = erroParcelas
                )
            }
            return
        }

        val request = TransacaoRequest(
            descricao = estado.descricao.trim(),
            valor = valor!!,
            data = estado.data,
            categoriaId = estado.categoriaId!!,
            formaPagamento = estado.formaPagamento,
            parcelas = parcelas!!,
            grupoId = grupoId
        )

        _uiState.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            val resultado: Resultado<String> = if (id == null) {
                when (val r = repository.criar(request)) {
                    is Resultado.Sucesso ->
                        Resultado.Sucesso(if (r.dados > 1) "Criadas ${r.dados} parcelas mensais" else "Transação criada")
                    is Resultado.Erro -> r
                }
            } else {
                when (val r = repository.atualizar(id, request)) {
                    is Resultado.Sucesso -> Resultado.Sucesso("Transação atualizada")
                    is Resultado.Erro -> r
                }
            }
            terminar(resultado)
        }
    }

    fun pedirExclusao() = _uiState.update { it.copy(confirmandoExclusao = true) }

    fun cancelarExclusao() = _uiState.update { it.copy(confirmandoExclusao = false) }

    fun confirmarExclusao() {
        val id = id ?: return
        _uiState.update { it.copy(confirmandoExclusao = false, salvando = true, erro = null) }
        viewModelScope.launch {
            terminar(
                when (val r = repository.excluir(id)) {
                    is Resultado.Sucesso -> Resultado.Sucesso("Transação excluída")
                    is Resultado.Erro -> r
                }
            )
        }
    }

    private fun terminar(resultado: Resultado<String>) = _uiState.update {
        when (resultado) {
            is Resultado.Sucesso -> it.copy(salvando = false, concluido = resultado.dados)
            is Resultado.Erro -> it.copy(salvando = false, erro = resultado.mensagem)
        }
    }
}
