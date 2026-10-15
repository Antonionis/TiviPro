package com.poltivi.ui.screens

import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poltivi.data.repository.DataSourceMode
import com.poltivi.ui.components.PolTiviLogo
import com.poltivi.ui.theme.DarkBackground
import com.poltivi.ui.theme.OrangePrimary
import com.poltivi.ui.theme.StatusError
import com.poltivi.ui.theme.StatusSuccess
import com.poltivi.ui.theme.SurfaceDark
import com.poltivi.ui.theme.SurfaceDarkCard
import com.poltivi.ui.theme.TextMuted
import com.poltivi.ui.viewmodel.AuthViewModel
import com.poltivi.ui.viewmodel.LoginTab

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            PolTiviLogo(sizeScale = 1.2f, showGlow = true)

            Spacer(modifier = Modifier.height(18.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(0.82f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Account Info Card if authenticated or logged in
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    val isConnected = isLoggedIn || (uiState.activeMode == DataSourceMode.XTREAM && uiState.authResponse?.userInfo != null)

                    if (isConnected) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Conectado",
                                        tint = StatusSuccess
                                    )
                                    Text(
                                        text = if (uiState.activeMode == DataSourceMode.M3U) "Lista M3U Conectada" else "Cuenta Xtream Conectada",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (uiState.activeMode == DataSourceMode.M3U) {
                                    val m3uDisplayUrl = uiState.m3uUrl.ifBlank { "Lista Remota M3U" }
                                    AccountDetailRow("Tipo de Fuente", "Lista M3U")
                                    AccountDetailRow("URL / Fuente", m3uDisplayUrl)
                                    AccountDetailRow("Estado", "Activo / Cargado")
                                } else {
                                    val userInfo = uiState.authResponse?.userInfo
                                    val displayUsername = userInfo?.username?.ifBlank { uiState.username } ?: uiState.username.ifBlank { "Usuario Activo" }
                                    val displayStatus = userInfo?.status ?: "Activo"
                                    val displayExpDate = formatLoginExpDate(userInfo?.expDate)
                                    val displayConns = "${userInfo?.activeCons ?: "1"} / ${userInfo?.maxConnections ?: "1"}"

                                    AccountDetailRow("URL del Servidor", uiState.serverUrl)
                                    AccountDetailRow("Nombre de Usuario", displayUsername)
                                    AccountDetailRow("Estado de Cuenta", displayStatus)
                                    AccountDetailRow("Caducidad", displayExpDate)
                                    AccountDetailRow("Conexiones Activas", displayConns)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = onLoginSuccess,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                    ) {
                                        Text("ABRIR PANEL PRINCIPAL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.logout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PowerSettingsNew,
                                            contentDescription = "Desconectarse",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Option Selector Tabs (Xtream Codes vs Lista M3U)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDarkCard, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Xtream Codes Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(
                                        if (uiState.selectedTab == LoginTab.XTREAM) OrangePrimary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.onTabSelected(LoginTab.XTREAM) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Dns,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTab == LoginTab.XTREAM) Color.White else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Xtream Codes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (uiState.selectedTab == LoginTab.XTREAM) Color.White else TextMuted
                                    )
                                }
                            }

                            // M3U Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(
                                        if (uiState.selectedTab == LoginTab.M3U) OrangePrimary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.onTabSelected(LoginTab.M3U) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTab == LoginTab.M3U) Color.White else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Lista M3U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (uiState.selectedTab == LoginTab.M3U) Color.White else TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.selectedTab == LoginTab.XTREAM) {
                            // Form Title
                            Text(
                                text = "Iniciar Sesión con Xtream Codes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Server URL Field
                            OutlinedTextField(
                                value = uiState.serverUrl,
                                onValueChange = { viewModel.onServerUrlChange(it) },
                                label = { Text("URL del Servidor") },
                                placeholder = { Text("URL", color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Dns, contentDescription = "URL del Servidor", tint = OrangePrimary)
                                },
                                colors = customTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Username Field
                            OutlinedTextField(
                                value = uiState.username,
                                onValueChange = { viewModel.onUsernameChange(it) },
                                label = { Text("Nombre de Usuario") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = "Nombre de Usuario", tint = OrangePrimary)
                                },
                                colors = customTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Field
                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = { viewModel.onPasswordChange(it) },
                                label = { Text("Contraseña") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, contentDescription = "Contraseña", tint = OrangePrimary)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = customTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // M3U Form Title
                            Text(
                                text = "Cargar Lista M3U",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // M3U URL Field
                            OutlinedTextField(
                                value = uiState.m3uUrl,
                                onValueChange = { viewModel.onM3uUrlChange(it) },
                                label = { Text("URL de la Lista M3U (.m3u / .m3u8)") },
                                placeholder = { Text("https://ejemplo.com/lista.m3u", color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Link, contentDescription = "URL M3U", tint = OrangePrimary)
                                },
                                colors = customTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connect Button
                        Button(
                            onClick = {
                                if (uiState.selectedTab == LoginTab.XTREAM) {
                                    viewModel.connectXtream(onLoginSuccess)
                                } else {
                                    viewModel.connectM3u(onLoginSuccess)
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (uiState.selectedTab == LoginTab.XTREAM) "CONECTAR AL SERVIDOR" else "CARGAR LISTA M3U",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Error or Success Messages
                        uiState.errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = msg,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        uiState.successMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = msg,
                                color = StatusSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangePrimary,
    unfocusedBorderColor = Color(0xFF33334A),
    focusedLabelColor = OrangePrimary,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = OrangePrimary,
    focusedContainerColor = SurfaceDarkCard,
    unfocusedContainerColor = SurfaceDarkCard
)

private fun formatLoginExpDate(expDate: String?): String {
    return try {
        if (!expDate.isNullOrBlank() && expDate != "0" && !expDate.equals("null", ignoreCase = true)) {
            val ts = expDate.toLongOrNull()
            if (ts != null && ts > 0) {
                val millis = if (ts < 100000000000L) ts * 1000L else ts
                val date = Date(millis)
                return SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(date)
            }
        }
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    } catch (_: Exception) {
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    }
}
