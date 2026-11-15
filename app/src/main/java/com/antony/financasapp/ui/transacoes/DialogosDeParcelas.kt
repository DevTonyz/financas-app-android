package com.antony.financasapp.ui.transacoes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.antony.financasapp.dto.EscopoParcelas

// Dados de uma parcela que os diálogos precisam. Nulo = gasto comum, sem parcelas
data class InfoParcela(val numero: Int, val total: Int) {
    val ehUltima get() = numero >= total
}

/**
 * Confirmação de exclusão. Num gasto comum é só "Excluir/Cancelar"; numa parcela,
 * pergunta o alcance, como nos apps de finanças do mercado: só esta, esta e as próximas ou a compra toda.
 */
@Composable
fun DialogoExcluirTransacao(
    descricao: String,
    parcela: InfoParcela?,
    aoEscolher: (EscopoParcelas) -> Unit,
    aoCancelar: () -> Unit
) {
    if (parcela == null) {
        AlertDialog(
            onDismissRequest = aoCancelar,
            title = { Text("Excluir transação?") },
            text = { Text("\"$descricao\" será apagada.") },
            confirmButton = { TextButton(onClick = { aoEscolher(EscopoParcelas.ESTA) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
        )
        return
    }

    val opcoes = buildList {
        add(EscopoParcelas.ESTA to "Só esta parcela (${parcela.numero}/${parcela.total})")
        // Na última parcela, "esta e as próximas" seria igual a "só esta"
        if (!parcela.ehUltima) add(EscopoParcelas.PROXIMAS to "Esta e as próximas")
        add(EscopoParcelas.TODAS to "A compra inteira (${parcela.total} parcelas)")
    }
    DialogoDeOpcoes(
        titulo = "Excluir parcela?",
        texto = "\"$descricao\" faz parte de uma compra em ${parcela.total}x. O que deseja excluir?",
        opcoes = opcoes,
        aoEscolher = aoEscolher,
        aoCancelar = aoCancelar
    )
}

// Pergunta ao salvar a edição de uma parcela que não é a última
@Composable
fun DialogoEscopoDaEdicao(
    parcela: InfoParcela,
    aoEscolher: (EscopoParcelas) -> Unit,
    aoCancelar: () -> Unit
) {
    DialogoDeOpcoes(
        titulo = "Aplicar a mudança a…",
        texto = "Esta é a parcela ${parcela.numero} de ${parcela.total}.",
        opcoes = listOf(
            EscopoParcelas.ESTA to "Só esta parcela",
            EscopoParcelas.PROXIMAS to "Esta e as próximas"
        ),
        aoEscolher = aoEscolher,
        aoCancelar = aoCancelar
    )
}

// AlertDialog só tem dois botões; para três opções, colocamos a lista no corpo
@Composable
private fun DialogoDeOpcoes(
    titulo: String,
    texto: String,
    opcoes: List<Pair<EscopoParcelas, String>>,
    aoEscolher: (EscopoParcelas) -> Unit,
    aoCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            Column {
                Text(texto)
                opcoes.forEach { (escopo, rotulo) ->
                    TextButton(onClick = { aoEscolher(escopo) }, modifier = Modifier.fillMaxWidth()) {
                        Text(rotulo, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

// Atalho: monta a InfoParcela a partir do que a API devolve
fun infoParcela(numero: Int?, total: Int?): InfoParcela? =
    if (numero != null && total != null) InfoParcela(numero, total) else null
