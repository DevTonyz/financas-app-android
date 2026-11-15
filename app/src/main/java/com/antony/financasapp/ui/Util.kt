package com.antony.financasapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.antony.financasapp.FinancasApplication

/**
 * Atalho para pegar a FinancasApplication (onde ficam o repositório, a API etc.).
 * Usado ao criar os ViewModels: viewModel { LoginViewModel(app().authRepository) }
 */
fun CreationExtras.app(): FinancasApplication = this[APPLICATION_KEY] as FinancasApplication

@Composable
fun appAtual(): FinancasApplication = LocalContext.current.applicationContext as FinancasApplication
