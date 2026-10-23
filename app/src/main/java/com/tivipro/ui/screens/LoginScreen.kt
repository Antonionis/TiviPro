package com.tivipro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.theme.OrangeGlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivipro.data.repository.DataSourceMode
import com.tivipro.ui.components.TiviProLogo
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusError
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.viewmodel.AuthViewModel
import com.tivipro.ui.viewmodel.LoginTab

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val loginContext = LocalContext.current

    // Polling continuo desde TV/Móvil al arrancar la pantalla sin sesión activa
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2000)
            try {
                val creds = com.tivipro.data.network.DeviceManager.pollQrLoginCredentials(loginContext)
                if (creds != null && creds.status == "success" && creds.url.isNotBlank()) {
                    val formattedUrl = if (!creds.url.startsWith("http://") && !creds.url.startsWith("https://")) "http://${creds.url}" else creds.url
                    if (creds.metodo == "xtream" && creds.usuario.isNotBlank()) {
                        viewModel.onServerUrlChange(formattedUrl)
                        viewModel.onUsernameChange(creds.usuario)
                        viewModel.onPasswordChange(creds.password)
                        viewModel.connectXtream(onLoginSuccess)
                        break
                    } else if (creds.metodo == "m3u") {
                        viewModel.onM3uUrlChange(formattedUrl)
                        viewModel.connectM3u(onLoginSuccess)
                        break
                    }
                }
            } catch (_: Exception) {}
        }
    }

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
            TiviProLogo(sizeScale = 1.2f, showGlow = true)

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

                                var showSwitchPlaylistDialog by remember { mutableStateOf(false) }

                                if (showSwitchPlaylistDialog) {
                                    SwitchPlaylistDialog(
                                        viewModel = viewModel,
                                        onDismiss = { showSwitchPlaylistDialog = false }
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onLoginSuccess,
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                                    ) {
                                        Text("ABRIR PANEL PRINCIPAL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showSwitchPlaylistDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ListAlt,
                                            contentDescription = "Cambiar de lista",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CAMBIAR DE LISTA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
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
                        // Option Selector Tabs (Xtream Codes vs Lista M3U vs Iniciar con QR)
                        val xtreamTabFocus = remember { FocusRequester() }
                        val m3uTabFocus = remember { FocusRequester() }
                        val qrTabFocus = remember { FocusRequester() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDarkCard, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Xtream Codes Tab
                            FocusableCard(
                                onClick = { viewModel.onTabSelected(LoginTab.XTREAM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusRequester(xtreamTabFocus)
                                    .focusProperties { right = m3uTabFocus }
                                    .onFocusChanged { state ->
                                        if (state.isFocused) viewModel.onTabSelected(LoginTab.XTREAM)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                elevation = 0.dp,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.selectedTab == LoginTab.XTREAM) OrangePrimary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Dns,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTab == LoginTab.XTREAM) Color.White else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Xtream",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (uiState.selectedTab == LoginTab.XTREAM) Color.White else TextMuted
                                    )
                                }
                            }

                            // M3U Tab
                            FocusableCard(
                                onClick = { viewModel.onTabSelected(LoginTab.M3U) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusRequester(m3uTabFocus)
                                    .focusProperties {
                                        left = xtreamTabFocus
                                        right = qrTabFocus
                                    }
                                    .onFocusChanged { state ->
                                        if (state.isFocused) viewModel.onTabSelected(LoginTab.M3U)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                elevation = 0.dp,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.selectedTab == LoginTab.M3U) OrangePrimary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTab == LoginTab.M3U) Color.White else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lista M3U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (uiState.selectedTab == LoginTab.M3U) Color.White else TextMuted
                                    )
                                }
                            }

                            // QR Tab
                            FocusableCard(
                                onClick = { viewModel.onTabSelected(LoginTab.QR) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusRequester(qrTabFocus)
                                    .focusProperties { left = m3uTabFocus }
                                    .onFocusChanged { state ->
                                        if (state.isFocused) viewModel.onTabSelected(LoginTab.QR)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                elevation = 0.dp,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.selectedTab == LoginTab.QR) OrangePrimary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTab == LoginTab.QR) Color.White else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Iniciar con QR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (uiState.selectedTab == LoginTab.QR) Color.White else TextMuted
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Connect Button
                            Button(
                                onClick = { viewModel.connectXtream(onLoginSuccess) },
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("CONECTAR AL SERVIDOR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else if (uiState.selectedTab == LoginTab.M3U) {
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Connect Button
                            Button(
                                onClick = { viewModel.connectM3u(onLoginSuccess) },
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("CARGAR LISTA M3U", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            // QR TAB CONTENT
                            val context = LocalContext.current
                            val deviceId = remember { com.tivipro.data.network.DeviceManager.getDeviceId(context) }
                            val qrUrl = remember(deviceId) { "http://84.235.239.152/tivipro/config.php?id=$deviceId" }
                            val qrCodeApiUrl = remember(qrUrl) { com.tivipro.ui.components.QrCodeGenerator.getQrCodeUrl(qrUrl) }

                            LaunchedEffect(uiState.selectedTab) {
                                while (isActive) {
                                    delay(2000)
                                    try {
                                        val creds = com.tivipro.data.network.DeviceManager.pollQrLoginCredentials(context)
                                        if (creds != null) {
                                            if (creds.usuario.isNotBlank() && creds.password.isNotBlank()) {
                                                val serverUrl = if (creds.url.isNotBlank()) creds.url else "http://84.235.239.152"
                                                viewModel.onServerUrlChange(serverUrl)
                                                viewModel.onUsernameChange(creds.usuario)
                                                viewModel.onPasswordChange(creds.password)
                                                viewModel.connectXtream(onLoginSuccess)
                                                break
                                            } else if (creds.url.isNotBlank()) {
                                                viewModel.onM3uUrlChange(creds.url)
                                                viewModel.connectM3u(onLoginSuccess)
                                                break
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Iniciar Sesión con Código QR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Box(
                                    modifier = Modifier
                                        .size(210.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = qrCodeApiUrl,
                                        contentDescription = "Código QR de inicio de sesión",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Text(
                                    text = "Escanea con el móvil e introduce los datos",
                                    color = OrangeGlow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
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

@Composable
fun SwitchPlaylistDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val repository = viewModel.repository
    var savedPlaylists by remember { mutableStateOf(repository.getSavedPlaylists()) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).clip(RoundedCornerShape(16.dp)),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SELECCIONAR O CAMBIAR PLAYLIST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Text("Tus listas y servidores guardados:", fontSize = 12.sp, color = TextMuted)

                savedPlaylists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkCard, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = pl.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                if (pl.isActive) {
                                    Surface(color = Color(0xFF00C853), shape = RoundedCornerShape(4.dp)) {
                                        Text("ACTIVA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(
                                text = if (pl.mode == DataSourceMode.XTREAM) "Xtream: ${pl.serverUrl}" else "M3U: ${pl.m3uUrl}",
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!pl.isActive) {
                            FocusableCard(
                                onClick = {
                                    scope.launch {
                                        statusMsg = "Conectando a ${pl.name}..."
                                        val ok = repository.switchPlaylist(pl.id)
                                        if (ok) {
                                            statusMsg = "Conectado a ${pl.name}"
                                            onDismiss()
                                        } else {
                                            statusMsg = "Error al conectar"
                                            savedPlaylists = repository.getSavedPlaylists()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Text("CONECTAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                statusMsg?.let { msg ->
                    Text(text = msg, color = OrangeGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
