package com.antony.financasapp.ui

import java.math.BigDecimal
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
