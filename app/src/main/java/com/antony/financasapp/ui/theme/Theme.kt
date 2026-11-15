package com.antony.financasapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = PetroleoClaro,
    onPrimary = OnPetroleoClaro,
    primaryContainer = PetroleoContainerClaro,
    onPrimaryContainer = OnPetroleoContainerClaro,
    secondary = PetroleoClaro,
    secondaryContainer = PetroleoContainerClaro,
    onSecondaryContainer = OnPetroleoContainerClaro,
    tertiary = AmbarClaro,
    tertiaryContainer = AmbarContainerClaro,
    onTertiaryContainer = OnAmbarContainerClaro,
    background = FundoClaro,
    onBackground = OnSuperficieClaro,
    surface = FundoClaro,
    onSurface = OnSuperficieClaro,
    surfaceVariant = SuperficieVariantClaro,
    onSurfaceVariant = OnSuperficieVariantClaro,
    outline = ContornoClaro,
    error = ErroClaro
)

private val EsquemaEscuro = darkColorScheme(
    primary = PetroleoEscuro,
    onPrimary = OnPetroleoEscuro,
    primaryContainer = PetroleoContainerEscuro,
    onPrimaryContainer = OnPetroleoContainerEscuro,
    secondary = PetroleoEscuro,
    secondaryContainer = PetroleoContainerEscuro,
    onSecondaryContainer = OnPetroleoContainerEscuro,
    tertiary = AmbarEscuro,
    tertiaryContainer = AmbarContainerEscuro,
    onTertiaryContainer = OnAmbarContainerEscuro,
    background = FundoEscuro,
    onBackground = OnSuperficieEscuro,
    surface = FundoEscuro,
    onSurface = OnSuperficieEscuro,
    surfaceVariant = SuperficieVariantEscuro,
    onSurfaceVariant = OnSuperficieVariantEscuro,
    outline = ContornoEscuro,
    error = ErroEscuro
)

// O Material 3 não tem uma cor de "positivo". Esta guarda o verde de "tem a receber",
// mais escuro no modo claro e mais claro no escuro, para ser legível nos dois
private val LocalCorReceber = staticCompositionLocalOf { ReceberClaro }

// Uso: MaterialTheme.colorScheme.receber (lê como se fosse uma cor do próprio tema)
val ColorScheme.receber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCorReceber.current

/**
 * Tema do app. Não usa a cor dinâmica do Android 12+ (que pinta o app com as cores do papel de parede)
 * de propósito: assim o app tem a mesma cara em qualquer celular, o que importa para a identidade dele.
 */
@Composable
fun FinancasTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalCorReceber provides if (escuro) ReceberEscuro else ReceberClaro) {
        MaterialTheme(
            colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
            typography = Tipografia,
            content = content
        )
    }
}
