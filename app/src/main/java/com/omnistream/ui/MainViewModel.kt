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
            val hasToken = userPreferences.authToken.first() != null
            val hasLoggedInBefore = userPreferences.hasLoggedInBefore.first()

            _startDestination.value = when {
                // Currently logged in - go to home
                hasToken -> StartDestination.Home
                // Not logged in, but has logged in before - skip passcode, go to login
                hasLoggedInBefore -> StartDestination.Login
                // Brand new install, never logged in - require passcode
                else -> StartDestination.AccessGate
            }
        }
    }
}
