package com.goaltv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goaltv.data.repository.DataRepository
import com.goaltv.data.repository.DataSourceMode
import com.goaltv.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FIXED_SERVER_URL = "http://zeta.proxy-pass.xyz:80"

data class AuthUiState(
    val serverUrl: String = FIXED_SERVER_URL,
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val authResponse: AuthResponse? = null,
    val activeMode: DataSourceMode = DataSourceMode.XTREAM
)

class AuthViewModel(
    private val repository: DataRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val isDohEnabled: StateFlow<Boolean> = repository.isDohEnabled

    fun setDohEnabled(enabled: Boolean) {
        repository.setDohEnabled(enabled)
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(activeMode = mode)
            }
        }
        viewModelScope.launch {
            repository.activeXtreamAccount.collect { acc ->
                if (acc != null) {
                    _uiState.value = _uiState.value.copy(
                        serverUrl = acc.serverUrl.ifBlank { FIXED_SERVER_URL },
                        username = acc.username,
                        password = acc.password,
                        authResponse = acc.authResponse
                    )
                }
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, errorMessage = null)
    }

    fun onUsernameChange(user: String) {
        _uiState.value = _uiState.value.copy(username = user, errorMessage = null)
    }

    fun onPasswordChange(pass: String) {
        _uiState.value = _uiState.value.copy(password = pass, errorMessage = null)
    }

    fun connectXtream(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Por favor ingrese Nombre de Usuario y Contraseña")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true, errorMessage = null, successMessage = null)
                val targetUrl = state.serverUrl.ifBlank { FIXED_SERVER_URL }
                val result = repository.connectXtreamServer(targetUrl, state.username, state.password)
                result.onSuccess { auth ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authResponse = auth,
                        successMessage = "¡Conectado exitosamente al Servidor!",
                        errorMessage = null
                    )
                    onSuccess()
                }.onFailure { err ->
                    val rawMsg = err.message ?: ""
                    val displayMsg = if (rawMsg.contains("Empty response body", ignoreCase = true)) {
                        "Fallo de conexion con el servidor"
                    } else if (rawMsg.isNotBlank()) {
                        rawMsg
                    } else {
                        "Fallo de conexion con el servidor"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = displayMsg
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fallo de conexion con el servidor"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.clearSession()
                _uiState.value = _uiState.value.copy(
                    username = "",
                    password = "",
                    authResponse = null,
                    successMessage = "Sesión cerrada."
                )
            } catch (_: Exception) {}
        }
    }

    fun importBackup(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val success = repository.importLocalBackup()
            if (success) {
                val acc = repository.activeXtreamAccount.value
                val user = acc?.username ?: ""
                val pass = acc?.password ?: ""
                if (user.isNotBlank() && pass.isNotBlank()) {
                    _uiState.value = _uiState.value.copy(
                        username = user,
                        password = pass,
                        isLoading = false
                    )
                    connectXtream(onSuccess)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Ajustes importados correctamente."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se encontró copia local para restaurar."
                )
            }
        }
    }
}
