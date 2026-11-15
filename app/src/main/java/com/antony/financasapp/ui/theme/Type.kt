package com.antony.financasapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Parte da tipografia padrão do Material 3 e só reforça títulos e valores.
// Fonte do sistema (sem baixar fonte): o app fica leve e combina com o resto do Android
private val Base = Typography()

val Tipografia = Typography(
    // Valores grandes (total do mês)
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)
