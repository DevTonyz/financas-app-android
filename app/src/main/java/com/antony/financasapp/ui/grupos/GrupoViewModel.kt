package com.antony.financasapp.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.DadosDoGrupoNoMes
import com.antony.financasapp.data.GrupoRepository
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.dto.PagamentoRequest
import com.antony.financasapp.dto.PagamentoResponse
import com.antony.financasapp.dto.SaldosResponse
import com.antony.financasapp.dto.SugestaoDePagamento
import com.antony.financasapp.ui.deslocarMes
import com.antony.financasapp.ui.hojeIso
import com.antony.financasapp.ui.textoParaValor
import com.antony.financasapp.ui.valorParaTexto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

// Diálogo "Registrar pagamento": de quem, para quem e o valor (editável, para pagamento parcial)
data class RegistroDePagamento(
    val deUsuarioId: Long,
    val deNome: String,
    val paraUsuarioId: Long,
    val paraNome: String,
    val valor: String,
    val erro: String? = null
)

data class GrupoUiState(
    val mes: Int,
    val ano: Int,
    // Carregando/erro do grupo (membros). Sem ele, a tela não tem o que mostrar
    val carregando: Boolean = true,
    val erro: String? = null,
    val grupo: GrupoDetalheResponse? = null,
    // Saldo acumulado (não depende do mês). Nulo enquanto carrega ou se falhar
    val saldos: SaldosResponse? = null,
    val erroSaldos: String? = null,
    // Carregando/erro do mês (total, gastos, quem pagou, pagamentos), que muda ao trocar de mês
    val carregandoMes: Boolean = true,
    val erroMes: String? = null,
    val dadosDoMes: DadosDoGrupoNoMes? = null,
    // Diálogo "Adicionar por e-mail"
    val adicionando: Boolean = false,
    val email: String = "",
    val erroEmail: String? = null,
    // Quando a pessoa entrou ("yyyy-MM-dd"). Gastos de antes dessa data não são divididos com ela
    val dataEntrada: String = hojeIso(),
    val escolhendoDataEntrada: Boolean = false,
    val salvando: Boolean = false,
    val confirmandoSaida: Boolean = false,
    val registrando: RegistroDePagamento? = null,
    val excluindoPagamento: PagamentoResponse? = null,
    // Mensagem rápida (Snackbar), ex.: "fulano foi adicionado"
    val aviso: String? = null,
    // Saiu do grupo: a tela volta para a lista
    val saiu: Boolean = false
) {
    // A API marca quem sou eu na lista de membros: serve para saber quais gastos posso editar
    val meuId: Long? get() = grupo?.membros?.firstOrNull { it.voce }?.id

    // "você" no lugar do meu e-mail, para os textos ficarem naturais
    fun nome(usuarioId: Long, login: String) = if (usuarioId == meuId) "você" else login

    // Igual, mas para começo de frase: "Você pagou..." (o e-mail fica como está)
    fun nomeNoInicio(usuarioId: Long, login: String) = if (usuarioId == meuId) "Você" else login
}

class GrupoViewModel(
    private val id: Long,
    private val repository: GrupoRepository,
    transacaoRepository: TransacaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        Calendar.getInstance().let { GrupoUiState(mes = it.get(Calendar.MONTH) + 1, ano = it.get(Calendar.YEAR)) }
    )
    val uiState: StateFlow<GrupoUiState> = _uiState.asStateFlow()

    private var buscaDoMes: Job? = null

    init {
        carregar()
        // Lançou, editou ou excluiu um gasto (no formulário)? Total e saldos mudam: recarrega
        viewModelScope.launch { transacaoRepository.mudancas.drop(1).collect { recarregarValores() } }
    }

    fun carregar() {
        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val r = repository.detalhar(id)) {
                is Resultado.Sucesso -> {
                    _uiState.update { it.copy(carregando = false, grupo = r.dados) }
                    recarregarValores()
                }
                is Resultado.Erro -> _uiState.update { it.copy(carregando = false, erro = r.mensagem) }
            }
        }
    }

    // Saldo acumulado + o mês aberto. Usado depois de qualquer mudança de valores
    private fun recarregarValores() {
        carregarSaldos()
        carregarMes()
    }

    fun carregarSaldos() {
        _uiState.update { it.copy(erroSaldos = null) }
        viewModelScope.launch {
            when (val r = repository.saldos(id)) {
                is Resultado.Sucesso -> _uiState.update { it.copy(saldos = r.dados) }
                is Resultado.Erro -> _uiState.update { it.copy(erroSaldos = r.mensagem) }
            }
        }
    }

    fun mesAnterior() = trocarMes(-1)

    fun proximoMes() = trocarMes(+1)

    private fun trocarMes(delta: Int) {
        _uiState.update {
            val (mes, ano) = deslocarMes(it.mes, it.ano, delta)
            it.copy(mes = mes, ano = ano)
        }
        carregarMes()
    }

    fun carregarMes() {
        // Cancela a busca anterior para a resposta de um mês velho não sobrescrever a do atual
        buscaDoMes?.cancel()
        _uiState.update { it.copy(carregandoMes = true, erroMes = null) }
        val (mes, ano) = _uiState.value.let { it.mes to it.ano }
        buscaDoMes = viewModelScope.launch {
            when (val r = repository.buscarMes(id, mes, ano)) {
                is Resultado.Sucesso -> _uiState.update { it.copy(carregandoMes = false, dadosDoMes = r.dados) }
                is Resultado.Erro -> _uiState.update { it.copy(carregandoMes = false, erroMes = r.mensagem) }
            }
        }
    }

    // --- Adicionar membro ---
    fun abrirAdicionar() = _uiState.update {
        it.copy(adicionando = true, email = "", erroEmail = null, dataEntrada = hojeIso())
    }

    fun abrirDataEntrada() = _uiState.update { it.copy(escolhendoDataEntrada = true) }

    fun fecharDataEntrada() = _uiState.update { it.copy(escolhendoDataEntrada = false) }

    fun aoMudarDataEntrada(iso: String) = _uiState.update {
        // Comparar strings "yyyy-MM-dd" funciona como comparar datas. Entrada no futuro não faz sentido
        val data = if (iso > hojeIso()) hojeIso() else iso
        it.copy(dataEntrada = data, escolhendoDataEntrada = false)
    }

    fun fecharAdicionar() = _uiState.update { it.copy(adicionando = false) }

    fun aoMudarEmail(texto: String) = _uiState.update { it.copy(email = texto.trim(), erroEmail = null) }

    fun adicionar() {
        val email = _uiState.value.email
        if (_uiState.value.salvando) return
        // Mesma checagem simples do cadastro: o resto (se a conta existe) quem diz é a API
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(erroEmail = "Digite um e-mail válido") }
            return
        }
        _uiState.update { it.copy(salvando = true, erroEmail = null) }
        viewModelScope.launch {
            when (val r = repository.adicionarMembro(id, email, _uiState.value.dataEntrada)) {
                is Resultado.Sucesso -> {
                    _uiState.update {
                        it.copy(salvando = false, adicionando = false, grupo = r.dados, aviso = "$email agora faz parte do grupo")
                    }
                    // Com mais um membro, a parte de cada um muda
                    recarregarValores()
                }
                // 404 (sem conta) e 409 (já é membro) aparecem embaixo do campo
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, erroEmail = r.mensagem) }
            }
        }
    }

    // --- Registrar pagamento (a partir de uma sugestão "fulano paga X a ciclano") ---
    fun abrirRegistro(sugestao: SugestaoDePagamento) = _uiState.update {
        it.copy(
            registrando = RegistroDePagamento(
                deUsuarioId = sugestao.deUsuarioId,
                deNome = it.nomeNoInicio(sugestao.deUsuarioId, sugestao.deLogin),
                paraUsuarioId = sugestao.paraUsuarioId,
                paraNome = it.nome(sugestao.paraUsuarioId, sugestao.paraLogin),
                // Já vem com o valor que falta; dá para mudar se o pagamento foi só de uma parte
                valor = valorParaTexto(sugestao.valor)
            )
        )
    }

    fun fecharRegistro() = _uiState.update { it.copy(registrando = null) }

    fun aoMudarValorDoPagamento(texto: String) = _uiState.update {
        if (texto.all { c -> c.isDigit() || c == ',' || c == '.' }) {
            it.copy(registrando = it.registrando?.copy(valor = texto, erro = null))
        } else it
    }

    fun confirmarRegistro() {
        val registro = _uiState.value.registrando ?: return
        if (_uiState.value.salvando) return
        val valor = textoParaValor(registro.valor)
        if (valor == null || valor <= BigDecimal.ZERO) {
            _uiState.update { it.copy(registrando = registro.copy(erro = "Informe um valor maior que zero")) }
            return
        }
        _uiState.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val request = PagamentoRequest(registro.deUsuarioId, registro.paraUsuarioId, valor, hojeIso())
            when (val r = repository.registrarPagamento(id, request)) {
                is Resultado.Sucesso -> {
                    _uiState.update { it.copy(salvando = false, registrando = null, aviso = "Pagamento registrado") }
                    recarregarValores()
                }
                is Resultado.Erro -> _uiState.update {
                    it.copy(salvando = false, registrando = registro.copy(erro = r.mensagem))
                }
            }
        }
    }

    // --- Apagar um pagamento registrado por engano ---
    fun pedirExclusaoDePagamento(p: PagamentoResponse) = _uiState.update { it.copy(excluindoPagamento = p) }

    fun cancelarExclusaoDePagamento() = _uiState.update { it.copy(excluindoPagamento = null) }

    fun confirmarExclusaoDePagamento() {
        val pagamento = _uiState.value.excluindoPagamento ?: return
        _uiState.update { it.copy(excluindoPagamento = null, salvando = true) }
        viewModelScope.launch {
            when (val r = repository.excluirPagamento(id, pagamento.id)) {
                is Resultado.Sucesso -> {
                    _uiState.update { it.copy(salvando = false, aviso = "Pagamento apagado") }
                    recarregarValores()
                }
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, aviso = r.mensagem) }
            }
        }
    }

    // --- Sair do grupo ---
    fun pedirSaida() = _uiState.update { it.copy(confirmandoSaida = true) }

    fun cancelarSaida() = _uiState.update { it.copy(confirmandoSaida = false) }

    fun confirmarSaida() {
        _uiState.update { it.copy(confirmandoSaida = false, salvando = true) }
        viewModelScope.launch {
            when (val r = repository.sair(id)) {
                is Resultado.Sucesso -> _uiState.update { it.copy(salvando = false, saiu = true) }
                // 409: ainda tem saldo pendente. A mensagem da API já explica o que fazer
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, aviso = r.mensagem) }
            }
        }
    }

    fun avisoMostrado() = _uiState.update { it.copy(aviso = null) }
}
