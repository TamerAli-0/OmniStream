package com.omnistream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    sealed class StartDestination {
        data object Loading : StartDestination()
        data object AccessGate : StartDestination()
        data object Login : StartDestination()
        data object Home : StartDestination()
    }

    private val _startDestination = MutableStateFlow<StartDestination>(StartDestination.Loading)
    val startDestination: StateFlow<StartDestination> = _startDestination

    val colorScheme = userPreferences.colorScheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "purple")

    val darkMode = userPreferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    init {
        viewModelScope.launch {
            val isUnlocked = userPreferences.isUnlocked.first()
            val hasToken = userPreferences.authToken.first() != null

            _startDestination.value = when {
                !isUnlocked -> StartDestination.AccessGate
                !hasToken -> StartDestination.Login
                else -> StartDestination.Home
            }
        }
    }
}
