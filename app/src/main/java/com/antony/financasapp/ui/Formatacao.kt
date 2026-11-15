package com.antony.financasapp.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.text.NumberFormat
import java.util.Locale

// Formatações usadas em todas as telas, num lugar só

private val formatoReais = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

// 1234.5 -> "R$ 1.234,50"
fun formatarReais(valor: BigDecimal): String = formatoReais.format(valor)

private val nomesDosMeses = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
)

// (11, 2026) -> "novembro de 2026"
fun mesPorExtenso(mes: Int, ano: Int): String = "${nomesDosMeses[mes - 1]} de $ano"

// --- Datas ---
// A API devolve "dd/MM/yyyy" e recebe "yyyy-MM-dd". No app guardamos sempre "yyyy-MM-dd" (ISO)
// e só convertemos na hora de mostrar. Strings em vez de java.time por causa do minSdk 24.

// "10/11/2026" -> "2026-11-10"
fun dataDaApiParaIso(data: String): String {
    val (dia, mes, ano) = data.split("/")
    return "$ano-$mes-$dia"
}

// "2026-11-10" -> "10/11/2026"
fun dataIsoParaExibir(data: String): String {
    val (ano, mes, dia) = data.split("-")
    return "$dia/$mes/$ano"
}

fun hojeIso(): String = Calendar.getInstance().let {
    String.format(
        Locale.US, "%04d-%02d-%02d",
        it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH)
    )
}

// O DatePicker do Material 3 trabalha com milissegundos em UTC (meia-noite do dia escolhido).
// Por isso usamos o fuso UTC nas duas conversões: com o fuso local, o dia poderia "voltar um".
private fun formatoIsoUtc() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun isoParaMillisUtc(data: String): Long = formatoIsoUtc().parse(data)!!.time

fun millisUtcParaIso(millis: Long): String = formatoIsoUtc().format(Date(millis))

// --- Valor digitado ---
// Aceita "12,50", "12.50" e "1.234,56". Devolve null se não for um número
fun textoParaValor(texto: String): BigDecimal? {
    val limpo = texto.trim().replace(" ", "").replace("R$", "")
    val normalizado = if (limpo.contains(',')) limpo.replace(".", "").replace(',', '.') else limpo
    return normalizado.toBigDecimalOrNull()
}

// 12.5 -> "12,50" (para preencher o campo ao editar)
fun valorParaTexto(valor: BigDecimal): String =
    valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
