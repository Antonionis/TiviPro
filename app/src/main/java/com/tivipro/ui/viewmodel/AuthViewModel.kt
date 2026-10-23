package com.tivipro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tivipro.data.repository.DataRepository
import com.tivipro.data.repository.DataSourceMode
import com.tivipro.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoginTab {
    XTREAM,
    M3U,
    QR
}

data class AuthUiState(
    val selectedTab: LoginTab = LoginTab.XTREAM,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val authResponse: AuthResponse? = null,
    val activeMode: DataSourceMode = DataSourceMode.XTREAM
)

class AuthViewModel(
    val repository: DataRepository
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
                        serverUrl = acc.serverUrl,
                        username = acc.username,
                        password = acc.password,
                        authResponse = acc.authResponse
                    )
                }
            }
        }
    }

    fun onTabSelected(tab: LoginTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, errorMessage = null, successMessage = null)
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

    fun onM3uUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(m3uUrl = url, errorMessage = null)
    }

    fun connectM3u(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.m3uUrl.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Por favor ingrese la URL de la lista M3U")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true, errorMessage = null, successMessage = null)
                val result = repository.loadM3uFromUrl(state.m3uUrl)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "¡Lista M3U cargada exitosamente!",
                        errorMessage = null
                    )
                    onSuccess()
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Error al descargar o procesar la lista M3U"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fallo de conexión al descargar la lista M3U"
                )
            }
        }
    }

    fun connectXtream(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Por favor ingrese la URL del Servidor, Usuario y Contraseña")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true, errorMessage = null, successMessage = null)
                val result = repository.connectXtreamServer(state.serverUrl, state.username, state.password)
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
                    val displayMsg = when {
                        rawMsg.contains("502", ignoreCase = true) || rawMsg.contains("Bad Gateway", ignoreCase = true) ->
                            "Fallo de conexión con el servidor (HTTP 502 Bad Gateway). Compruebe la URL del servidor."
                        rawMsg.contains("Empty response body", ignoreCase = true) ->
                            "Fallo de conexión con el servidor"
                        rawMsg.isNotBlank() ->
                            rawMsg
                        else ->
                            "Fallo de conexión con el servidor"
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
