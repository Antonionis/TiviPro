package com.goaltv.ui.screens

import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tv
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
import com.goaltv.data.repository.DataSourceMode
import com.goaltv.ui.components.GoalTvLogo
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.OrangePrimary
import com.goaltv.ui.theme.StatusError
import com.goaltv.ui.theme.StatusSuccess
import com.goaltv.ui.theme.SurfaceDark
import com.goaltv.ui.theme.SurfaceDarkCard
import com.goaltv.ui.theme.TextMuted
import com.goaltv.ui.viewmodel.AuthViewModel

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
            GoalTvLogo(sizeScale = 1.2f, showGlow = true)



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
                        val userInfo = uiState.authResponse?.userInfo
                        val displayUsername = userInfo?.username?.ifBlank { uiState.username } ?: uiState.username.ifBlank { "Usuario Activo" }
                        val displayStatus = userInfo?.status ?: "Activo"
                        val displayExpDate = formatLoginExpDate(userInfo?.expDate)
                        val displayConns = "${userInfo?.activeCons ?: "1"} / ${userInfo?.maxConnections ?: "1"}"

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
                                        text = "Cuenta Conectada",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                AccountDetailRow("Nombre de Usuario", displayUsername)
                                AccountDetailRow("Estado de Cuenta", displayStatus)
                                AccountDetailRow("Caducidad", displayExpDate)
                                AccountDetailRow("Conexiones Activas", displayConns)

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
                        // Form Title
                        Text(
                            text = "Iniciar Sesión",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // VPNBook OpenVPN Toggle Card
                        val isVpnActive by com.goaltv.data.network.VpnBookManager.isVpnActive.collectAsState()
                        val vpnStatusMsg by com.goaltv.data.network.VpnBookManager.vpnStatusMessage.collectAsState()
                        val loginContext = LocalContext.current
                        val loginScope = rememberCoroutineScope()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isVpnActive) Color(0xFF13281E) else SurfaceDarkCard
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVpnActive) Icons.Rounded.Security else Icons.Rounded.Lock,
                                        contentDescription = "VPNBook",
                                        tint = if (isVpnActive) StatusSuccess else OrangePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "VPNBook Anti-bloqueo (FR231)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = vpnStatusMsg,
                                            fontSize = 10.sp,
                                            color = if (isVpnActive) StatusSuccess else TextMuted
                                        )
                                    }
                                }

                                Switch(
                                    checked = isVpnActive,
                                    onCheckedChange = {
                                        loginScope.launch {
                                            com.goaltv.data.network.VpnBookManager.toggleVpn(loginContext)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StatusSuccess,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = SurfaceDark
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.connectXtream(onLoginSuccess) },
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
                                    text = "CONECTAR AL SERVIDOR",
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
                return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")).format(date)
            }
        }
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    } catch (_: Exception) {
        val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
        SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")).format(oneYearAhead)
    }
}
