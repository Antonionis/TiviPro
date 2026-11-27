package com.tivipro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import com.tivipro.ui.components.FocusableCard
import com.tivipro.ui.components.TiviProLogo
import com.tivipro.ui.theme.OrangeGlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.tivipro.data.repository.DataSourceMode
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.tivipro.data.network.UpdateInfo
import com.tivipro.data.network.UpdateManager
import com.tivipro.ui.components.UpdateDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivipro.ui.theme.DarkBackground
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.StatusError
import com.tivipro.ui.theme.StatusSuccess
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.SurfaceDarkCard
import com.tivipro.ui.theme.TextMuted
import com.tivipro.ui.theme.TextSecondary
import com.tivipro.ui.viewmodel.AppScreen
import com.tivipro.ui.viewmodel.MainViewModel
import coil.compose.AsyncImage

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val isPremium by viewModel.repository.isPremium.collectAsState()

    var selectedDecoder by remember { mutableStateOf("Hardware (HW)") }
    var selectedAspectRatio by remember { mutableStateOf("Ajustar a Pantalla") }
    var selectedBuffer by remember { mutableStateOf("Mediano (3s)") }

    val context = LocalContext.current

    var categories by remember { mutableStateOf<List<com.tivipro.model.Category>>(emptyList()) }
    var hiddenCategoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var telegramUser by remember { mutableStateOf(com.tivipro.data.network.TelegramAlertManager.getTelegramUser(context)) }

    LaunchedEffect(Unit) {
        try {
            val status = com.tivipro.data.network.DeviceManager.verificarDispositivo(context)
            viewModel.repository.setPremiumStatus(status.isPremium)
            if (!status.isPremium) {
                val playlists = viewModel.repository.getSavedPlaylists()
                val active = playlists.firstOrNull { it.isActive }
                if (active != null) {
                    playlists.forEach {
                        if (it.id != active.id) {
                            viewModel.repository.deletePlaylist(it.id)
                        }
                    }
                }
            }
            categories = viewModel.repository.getAllLiveCategories()
            hiddenCategoryIds = viewModel.repository.getHiddenCategoryIds()

            // Comprobar siempre en la BD si existe ID de Telegram configurada para este dispositivo
            val dbTelegramUser = com.tivipro.data.network.DeviceManager.pollTelegramCredentials(context)
            if (!dbTelegramUser.isNullOrBlank()) {
                com.tivipro.data.network.TelegramAlertManager.saveTelegramConfig(context, dbTelegramUser)
                telegramUser = dbTelegramUser
            } else {
                telegramUser = com.tivipro.data.network.TelegramAlertManager.getTelegramUser(context)
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPremium) {
        if (!isPremium) {
            if (com.tivipro.data.network.TelegramAlertManager.isConfigured(context)) {
                com.tivipro.data.network.TelegramAlertManager.clearTelegramConfig(context)
                telegramUser = ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Ajustes",
                    tint = OrangePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ajustes de Reproductor y Servidor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            TiviProLogo(sizeScale = 0.75f, showGlow = false)
        }

        // Section 1: Player Decoder Settings
        SettingsCard(
            title = "MODO DE DECODIFICADOR",
            icon = Icons.Rounded.Memory
        ) {
            val decoders = listOf("Hardware (HW)", "Software (SW)", "Automático")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                decoders.forEach { mode ->
                    val isSelected = selectedDecoder == mode
                    FocusableCard(
                        onClick = { selectedDecoder = mode },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2: Aspect Ratio Preference
        SettingsCard(
            title = "RELACIÓN DE ASPECTO",
            icon = Icons.Rounded.AspectRatio
        ) {
            val aspectRatios = listOf("Ajustar a Pantalla", "16:9", "Zoom", "Estirar")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                aspectRatios.forEach { ratio ->
                    val isSelected = selectedAspectRatio == ratio
                    FocusableCard(
                        onClick = { selectedAspectRatio = ratio },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: Buffer Settings
        SettingsCard(
            title = "TAMAÑO DE BÚFER",
            icon = Icons.Rounded.Speed
        ) {
            val buffers = listOf("Pequeño (1s)", "Mediano (3s)", "Grande (5s)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buffers.forEach { buf ->
                    val isSelected = selectedBuffer == buf
                    FocusableCard(
                        onClick = { selectedBuffer = buf },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) OrangePrimary else Color(0xFF222232))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OrangeGlow else Color(0xFF33334A),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buf,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 4: FUENTE DE GUÍA DE PROGRAMACIÓN (EPG)
        val currentEpgSource by viewModel.repository.epgSourceMode.collectAsState()

        SettingsCard(
            title = "FUENTE DE GUÍA DE PROGRAMACIÓN (EPG)",
            icon = Icons.Rounded.Schedule,
            iconTint = Color(0xFF0088FF)
        ) {
            Column {
                Text(
                    text = "Selecciona el origen de la guía EPG para la lista de canales de TV:",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val sources = listOf(
                        com.tivipro.data.repository.EpgSourceMode.TIVIPRO to "TiviPro (Guía Integrada)",
                        com.tivipro.data.repository.EpgSourceMode.PROVIDER to "Proveedor IPTV (Servidor)"
                    )
                    sources.forEach { (mode, label) ->
                        val isSelected = currentEpgSource == mode
                        FocusableCard(
                            onClick = { viewModel.repository.setEpgSourceMode(mode) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) Color(0xFF0088FF) else Color(0xFF1F1F30))
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else Color(0xFF33334A),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Gestión de Múltiples Playlists
        var savedPlaylists by remember { mutableStateOf<List<com.tivipro.model.SavedPlaylist>>(emptyList()) }
        var showAddPlaylistDialog by remember { mutableStateOf(false) }
        var editingPlaylist by remember { mutableStateOf<com.tivipro.model.SavedPlaylist?>(null) }
        var playlistStatusMessage by remember { mutableStateOf<String?>(null) }
        val settingsScope = rememberCoroutineScope()

        LaunchedEffect(isPremium) {
            try {
                if (!isPremium) {
                    val playlists = viewModel.repository.getSavedPlaylists()
                    if (playlists.size > 1) {
                        val active = playlists.firstOrNull { it.isActive } ?: playlists.firstOrNull()
                        if (active != null) {
                            playlists.forEach {
                                if (it.id != active.id) {
                                    viewModel.repository.deletePlaylist(it.id)
                                }
                            }
                        }
                    }
                }
                savedPlaylists = viewModel.repository.getSavedPlaylists()
            } catch (_: Exception) {}
        }

        if (showAddPlaylistDialog) {
            AddPlaylistDialog(
                initialPlaylist = editingPlaylist,
                onSave = { newPl ->
                    viewModel.repository.savePlaylist(newPl)
                    savedPlaylists = viewModel.repository.getSavedPlaylists()
                    showAddPlaylistDialog = false
                    editingPlaylist = null
                    settingsScope.launch {
                        playlistStatusMessage = "Conectando a ${newPl.name}..."
                        val ok = viewModel.repository.switchPlaylist(newPl.id)
                        playlistStatusMessage = if (ok) "Conectado con éxito a ${newPl.name}" else "Error al conectar a ${newPl.name}"
                        savedPlaylists = viewModel.repository.getSavedPlaylists()
                    }
                },
                onDismiss = {
                    showAddPlaylistDialog = false
                    editingPlaylist = null
                }
            )
        }

        SettingsCard(
            title = "GESTIÓN DE MÚLTIPLES PLAYLISTS / LISTAS",
            icon = Icons.Rounded.ListAlt
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Añade varios servidores o listas M3U/M3U8 y cambia entre ellos rápidamente.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

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
                                Text(
                                    text = pl.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
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

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!pl.isActive) {
                                FocusableCard(
                                    onClick = {
                                        settingsScope.launch {
                                            playlistStatusMessage = "Conectando a ${pl.name}..."
                                            val ok = viewModel.repository.switchPlaylist(pl.id)
                                            playlistStatusMessage = if (ok) "Cambiado con éxito a ${pl.name}" else "Error al conectar a ${pl.name}"
                                            savedPlaylists = viewModel.repository.getSavedPlaylists()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("USAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            FocusableCard(
                                onClick = {
                                    editingPlaylist = pl
                                    showAddPlaylistDialog = true
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("EDITAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            if (!pl.isActive) {
                                FocusableCard(
                                    onClick = {
                                        viewModel.repository.deletePlaylist(pl.id)
                                        savedPlaylists = viewModel.repository.getSavedPlaylists()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1E28))
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("BORRAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3366))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                FocusableCard(
                    onClick = {
                        val maxAllowed = if (isPremium) 5 else 1
                        if (savedPlaylists.size >= maxAllowed) {
                            playlistStatusMessage = if (isPremium) {
                                "Límite de listas Premium alcanzado (máximo 5 listas)."
                            } else {
                                "Límite alcanzado: La versión gratuita permite 1 lista. Pásate a Premium para añadir hasta 5 listas."
                            }
                        } else {
                            editingPlaylist = null
                            showAddPlaylistDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("+ AÑADIR NUEVA LISTA / SERVIDOR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }

                playlistStatusMessage?.let { msg ->
                    Text(text = msg, color = OrangeGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 5: Gestión y Orden de Categorías
        var showCategoryDialog by remember { mutableStateOf(false) }

        if (showCategoryDialog) {
            CategoryManagementDialog(
                categories = categories,
                hiddenCategoryIds = hiddenCategoryIds,
                isPremium = isPremium,
                onToggleHidden = { catId ->
                    viewModel.repository.toggleCategoryHidden(catId)
                    hiddenCategoryIds = viewModel.repository.getHiddenCategoryIds()
                },
                onToggleAllHidden = { hideAll ->
                    viewModel.repository.setAllCategoriesHidden(categories.map { it.id }, hideAll)
                    hiddenCategoryIds = viewModel.repository.getHiddenCategoryIds()
                },
                onReorder = { newOrderList ->
                    categories = newOrderList
                    viewModel.repository.saveCategoryOrder(newOrderList.map { it.id })
                },
                onDismiss = { showCategoryDialog = false }
            )
        }

        if (categories.isNotEmpty()) {
            SettingsCard(
                title = "GESTIÓN Y ORDEN DE CATEGORÍAS DE TV",
                icon = Icons.Rounded.VisibilityOff
            ) {
                Column {
                    Text(
                        text = "Ordena las categorías de canales a tu gusto y oculta las que no te interesen.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FocusableCard(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("GESTIONAR Y ORDENAR CATEGORÍAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 6: Backup Local
        var backupMessage by remember { mutableStateOf<String?>(null) }

        SettingsCard(
            title = "COPIA DE SEGURIDAD Y RESTAURACIÓN LOCAL",
            icon = Icons.Rounded.Save
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Guarda tu selección de favoritos y el orden de canales para restaurarlo fácilmente cuando quieras",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusableCard(
                        onClick = {
                            if (!isPremium) {
                                backupMessage = "Función disponible solo en la versión Premium"
                                return@FocusableCard
                            }
                            val success = viewModel.repository.exportLocalBackup()
                            backupMessage = if (success) {
                                "Copia de seguridad guardada con éxito en Descargas (tivipro_backup.json)"
                            } else {
                                "Error al guardar copia local de ajustes"
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("EXPORTAR AJUSTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    FocusableCard(
                        onClick = {
                            if (!isPremium) {
                                backupMessage = "Función disponible solo en la versión Premium"
                                return@FocusableCard
                            }
                            val success = viewModel.repository.importLocalBackup()
                            if (success) {
                                backupMessage = "Ajustes y favoritos restaurados con éxito desde Descargas (tivipro_backup.json)"
                            } else {
                                backupMessage = "No se encontró el archivo tivipro_backup.json en Descargas"
                            }
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("RESTAURAR AJUSTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                backupMessage?.let { msg ->
                    val isError = !isPremium || msg.contains("Error") || msg.contains("No se encontró")
                    Text(
                        text = msg,
                        color = if (isError) OrangeGlow else StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 7: Software Update
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var updateStatusMessage by remember { mutableStateOf<String?>(null) }
        var manualUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var showCurrentReleaseNotes by remember { mutableStateOf(false) }
        val updateContext = LocalContext.current
        val updateScope = rememberCoroutineScope()
        val deviceId = remember { com.tivipro.data.network.DeviceManager.getDeviceId(updateContext) }

        val appVersionName = remember {
            try {
                val pInfo = updateContext.packageManager.getPackageInfo(updateContext.packageName, 0)
                pInfo.versionName ?: "1.6"
            } catch (_: Exception) {
                "1.6"
            }
        }

        val currentReleaseNotesInfo = remember(appVersionName) {
            UpdateInfo(
                isUpdateAvailable = false,
                versionCode = 9,
                versionName = appVersionName,
                apkUrl = "",
                changelog = "• TiviPro v$appVersionName Versión Oficial\n• Buscador de Canales de TV en Tiempo Real: Barra de búsqueda en Televisión que filtra dinámicamente los canales conforme escribes\n• Gestión Global de Categorías: Botón 'Ocultar todas / Mostrar todas' en el menú de categorías para cambiar la visibilidad a la vez\n• Recarga Completa e Instantánea: El botón de recargar en la barra superior refresca en tiempo real canales de TV, guía EPG y catálogo VOD\n• Vinculación Remota de Telegram Mejorada: Detección automática del ID de Telegram desde la web y cierre inteligente de la ventana de configuración\n• Alertas en Segundo Plano y Control Premium: Sistema de notificaciones programadas vía WorkManager que entrega avisos en Telegram incluso con la app cerrada"
            )
        }

        var dynamicReleaseNotesInfo by remember { mutableStateOf<UpdateInfo?>(null) }

        if (showCurrentReleaseNotes) {
            UpdateDialog(
                updateInfo = dynamicReleaseNotesInfo ?: currentReleaseNotesInfo,
                onDismiss = { showCurrentReleaseNotes = false },
                isReleaseNotesOnly = true
            )
        }

        if (manualUpdateInfo != null) {
            UpdateDialog(
                updateInfo = manualUpdateInfo!!,
                onDismiss = { manualUpdateInfo = null }
            )
        }

        SettingsCard(
            title = "ACTUALIZACIÓN DE SOFTWARE",
            icon = Icons.Rounded.SystemUpdate
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comprueba si existe una versión más reciente de TiviPro:",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isPremium) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.tivipro.R.drawable.logoprem),
                                contentDescription = "Logo Premium",
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FocusableCard(
                                onClick = {
                                    updateScope.launch {
                                        val notes = com.tivipro.data.network.UpdateManager.fetchCurrentReleaseNotes(updateContext)
                                        dynamicReleaseNotesInfo = notes
                                        showCurrentReleaseNotes = true
                                    }
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2A2A40))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Versión actual: v$appVersionName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Text(
                                text = "ID: $deviceId",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeGlow
                            )
                        }
                    }
                }

                FocusableCard(
                    onClick = {
                        isCheckingUpdate = true
                        updateStatusMessage = null
                        updateScope.launch {
                            val info = UpdateManager.checkForUpdate(updateContext)
                            isCheckingUpdate = false
                            if (info.isUpdateAvailable) {
                                manualUpdateInfo = info
                            } else {
                                updateStatusMessage = "TiviPro está actualizado a la última versión."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("BUSCAR ACTUALIZACIÓN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                updateStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = StatusSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Notificaciones y Alertas de Telegram
        var showTelegramQrDialog by remember { mutableStateOf(false) }

        if (showTelegramQrDialog) {
            TelegramSetupDialog(
                onDismiss = {
                    telegramUser = com.tivipro.data.network.TelegramAlertManager.getTelegramUser(context)
                    showTelegramQrDialog = false
                }
            )
        }

        SettingsCard(
            title = "ALERTAS DE EVENTOS DEPORTIVOS (@tivipro_bot)",
            icon = Icons.Rounded.Send
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isPremium) {
                    Surface(
                        color = Color(0xFF2B1A0E),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⭐", fontSize = 15.sp)
                            Text(
                                text = "La función de alertas automáticas por Telegram está disponible exclusivamente para usuarios Premium.",
                                color = OrangeGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (telegramUser.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkCard, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ID DE TELEGRAM VINCULADO:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(color = Color(0xFF00C853), shape = RoundedCornerShape(4.dp)) {
                                    Text("ACTIVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                Text(
                                    text = telegramUser,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FocusableCard(
                                onClick = {
                                    if (!isPremium) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "La función de alertas por Telegram está disponible solo en la versión Premium",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        showTelegramQrDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text("CAMBIAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            FocusableCard(
                                onClick = {
                                    com.tivipro.data.network.TelegramAlertManager.clearTelegramConfig(context)
                                    telegramUser = ""
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1E28))
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text("ELIMINAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3366))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Vincula tu dispositivo TiviPro para recibir avisos automáticos en Telegram 10 minutos antes del comienzo de tus eventos deportivos favoritos:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    FocusableCard(
                        onClick = {
                            if (!isPremium) {
                                android.widget.Toast.makeText(
                                    context,
                                    "La función de alertas por Telegram está disponible solo en la versión Premium",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showTelegramQrDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "CONFIGURAR ALERTAS DE EVENTOS DEPORTIVOS A TRAVÉS DE TELEGRAM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 8: Guía de uso / Soporte
        var showAboutDialog by remember { mutableStateOf(false) }
        var showSupportQrDialog by remember { mutableStateOf(false) }

        if (showAboutDialog) {
            AboutInstructionsDialog(
                onDismiss = { showAboutDialog = false }
            )
        }

        if (showSupportQrDialog) {
            SupportQrDialog(
                onDismiss = { showSupportQrDialog = false }
            )
        }

        SettingsCard(
            title = "GUÍA DE USO/SOPORTE",
            icon = Icons.Rounded.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Consulta las instrucciones de uso, trucos de navegación D-Pad y soporte técnico oficial.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusableCard(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("CÓMO USAR LA APP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    FocusableCard(
                        onClick = { showSupportQrDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        enableScale = false,
                        colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("SOPORTE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 4: Server & Account Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "GESTIÓN DE SERVIDOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                FocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("MI CUENTA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FocusableCard(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enableScale = false,
                    colors = CardDefaults.cardColors(containerColor = StatusError),
                    focusedBorderColor = Color(0xFFFFD700)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESCONECTARSE DE LA CUENTA",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = OrangePrimary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun CategoryManagementDialog(
    categories: List<com.tivipro.model.Category>,
    hiddenCategoryIds: Set<String>,
    isPremium: Boolean,
    onToggleHidden: (String) -> Unit,
    onToggleAllHidden: (Boolean) -> Unit,
    onReorder: (List<com.tivipro.model.Category>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(20.dp)),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GESTIÓN DE CATEGORÍAS",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ordena las categorías de canales a tu gusto y oculta las que no te interesen.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF2B1A0E),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⭐", fontSize = 15.sp)
                            Text(
                                text = "La opción de ordenar las categorías solo está disponible para usuarios Premium.",
                                color = OrangeGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val allHidden = categories.isNotEmpty() && categories.all { hiddenCategoryIds.contains(it.id) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORÍAS (${categories.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    FocusableCard(
                        onClick = {
                            val hide = !allHidden
                            onToggleAllHidden(hide)
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allHidden) Color(0xFF0C2B1D) else Color(0xFF2B0C15)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (allHidden) Color(0xFF00E676) else Color(0xFFFF3366),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (allHidden) "👁 MOSTRAR TODAS" else "👁 OCULTAR TODAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (allHidden) Color(0xFF00E676) else Color(0xFFFF3366)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(categories) { index, cat ->
                        val isHidden = hiddenCategoryIds.contains(cat.id)

                        fun handleReorderAction(action: () -> Unit) {
                            if (!isPremium) {
                                android.widget.Toast.makeText(
                                    context,
                                    "La ordenación de categorías solo está disponible en la versión Premium",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                action()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDarkCard, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isHidden) TextMuted else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                var focusRequestedByReorder by remember { mutableStateOf(false) }
                                val cardFocusRequester = remember(cat.id) { androidx.compose.ui.focus.FocusRequester() }

                                LaunchedEffect(focusRequestedByReorder) {
                                    if (focusRequestedByReorder) {
                                        try {
                                            cardFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                        focusRequestedByReorder = false
                                    }
                                }

                                if (isPremium) {
                                    // 1. Move to Top (⏫)
                                    FocusableCard(
                                        onClick = {
                                            if (index > 0) {
                                                focusRequestedByReorder = true
                                                val mutable = categories.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(0, item)
                                                onReorder(mutable)
                                            }
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(28.dp).focusRequester(cardFocusRequester),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2C))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("⏫", fontSize = 11.sp)
                                        }
                                    }

                                    // 2. Move Up (↑)
                                    FocusableCard(
                                        onClick = {
                                            if (index > 0) {
                                                focusRequestedByReorder = true
                                                val mutable = categories.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                onReorder(mutable)
                                            }
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(28.dp).focusRequester(cardFocusRequester),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2C))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("↑", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // 3. Move Down (↓)
                                    FocusableCard(
                                        onClick = {
                                            if (index < categories.size - 1) {
                                                focusRequestedByReorder = true
                                                val mutable = categories.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                onReorder(mutable)
                                            }
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(28.dp).focusRequester(cardFocusRequester),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2C))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("↓", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // 4. Move to Bottom (⏬)
                                    FocusableCard(
                                        onClick = {
                                            if (index < categories.size - 1) {
                                                focusRequestedByReorder = true
                                                val mutable = categories.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(item)
                                                onReorder(mutable)
                                            }
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(28.dp).focusRequester(cardFocusRequester),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2C))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("⏬", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // 5. Visibility Toggle Button (👁️ Visible / 👁️ Oculta)
                                val visBgColor = if (!isHidden) Color(0xFF0C2B1D) else Color(0xFF2B0C15)
                                val visBorderColor = if (!isHidden) Color(0xFF00E676) else Color(0xFFFF3366)
                                val visText = if (!isHidden) "👁 Visible" else "👁 Oculta"

                                FocusableCard(
                                    onClick = { 
                                        focusRequestedByReorder = true
                                        onToggleHidden(cat.id) 
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp).focusRequester(cardFocusRequester),
                                    colors = CardDefaults.cardColors(containerColor = visBgColor)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, visBorderColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = visText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = visBorderColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPlaylistDialog(
    initialPlaylist: com.tivipro.model.SavedPlaylist? = null,
    onSave: (com.tivipro.model.SavedPlaylist) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialPlaylist?.name ?: "") }
    var selectedTab by remember { mutableStateOf(if (initialPlaylist?.mode == DataSourceMode.M3U) com.tivipro.ui.viewmodel.LoginTab.M3U else com.tivipro.ui.viewmodel.LoginTab.XTREAM) }
    var serverUrl by remember { mutableStateOf(initialPlaylist?.serverUrl ?: "") }
    var username by remember { mutableStateOf(initialPlaylist?.username ?: "") }
    var password by remember { mutableStateOf(initialPlaylist?.password ?: "") }
    var m3uUrl by remember { mutableStateOf(initialPlaylist?.m3uUrl ?: "") }

    val context = LocalContext.current
    val deviceId = remember { com.tivipro.data.network.DeviceManager.getDeviceId(context) }
    val qrUrl = remember(deviceId) { "https://tivipro.duckdns.org/tivipro/config.php?id=$deviceId" }
    val qrCodeApiUrl = remember(qrUrl) { com.tivipro.ui.components.QrCodeGenerator.getQrCodeUrl(qrUrl) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == com.tivipro.ui.viewmodel.LoginTab.QR) {
            while (isActive) {
                kotlinx.coroutines.delay(2000)
                try {
                    val creds = com.tivipro.data.network.DeviceManager.pollQrLoginCredentials(context)
                    if (creds != null) {
                        if (creds.usuario.isNotBlank() && creds.password.isNotBlank()) {
                            val sUrl = if (creds.url.isNotBlank()) creds.url else "https://tivipro.duckdns.org"
                            val plName = name.ifBlank { "Xtream Playlist" }
                            val targetId = initialPlaylist?.id ?: java.util.UUID.randomUUID().toString()
                            val isCurrentlyActive = initialPlaylist?.isActive ?: false
                            val newPl = com.tivipro.model.SavedPlaylist(
                                id = targetId,
                                name = plName,
                                mode = DataSourceMode.XTREAM,
                                serverUrl = sUrl,
                                username = creds.usuario,
                                password = creds.password,
                                m3uUrl = "",
                                isActive = isCurrentlyActive
                            )
                            onSave(newPl)
                            break
                        } else if (creds.url.isNotBlank()) {
                            val plName = name.ifBlank { "M3U Playlist" }
                            val targetId = initialPlaylist?.id ?: java.util.UUID.randomUUID().toString()
                            val isCurrentlyActive = initialPlaylist?.isActive ?: false
                            val newPl = com.tivipro.model.SavedPlaylist(
                                id = targetId,
                                name = plName,
                                mode = DataSourceMode.M3U,
                                serverUrl = "",
                                username = "",
                                password = "",
                                m3uUrl = creds.url,
                                isActive = isCurrentlyActive
                            )
                            onSave(newPl)
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val isCompactHeight = maxHeight < 480.dp
            val dialogWidth = if (isCompactHeight) 0.88f else 0.70f

            Surface(
                modifier = Modifier
                    .fillMaxWidth(dialogWidth)
                    .clip(RoundedCornerShape(18.dp)),
                color = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .padding(if (isCompactHeight) 12.dp else 18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (initialPlaylist != null) "EDITAR PLAYLIST" else "AÑADIR NUEVA PLAYLIST",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactHeight) 13.sp else 15.sp
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la lista (ej: Servidor 2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = TextMuted)
                    )

                    val xtreamTabFocus = remember { FocusRequester() }
                    val m3uTabFocus = remember { FocusRequester() }
                    val qrTabFocus = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        try {
                            xtreamTabFocus.requestFocus()
                        } catch (_: Exception) {}
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FocusableCard(
                            onClick = { selectedTab = com.tivipro.ui.viewmodel.LoginTab.XTREAM },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCompactHeight) 32.dp else 36.dp)
                                .focusRequester(xtreamTabFocus)
                                .focusProperties { right = m3uTabFocus }
                                .onFocusChanged { if (it.isFocused) selectedTab = com.tivipro.ui.viewmodel.LoginTab.XTREAM },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (selectedTab == com.tivipro.ui.viewmodel.LoginTab.XTREAM) OrangePrimary else Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Xtream Codes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        FocusableCard(
                            onClick = { selectedTab = com.tivipro.ui.viewmodel.LoginTab.M3U },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCompactHeight) 32.dp else 36.dp)
                                .focusRequester(m3uTabFocus)
                                .focusProperties {
                                    left = xtreamTabFocus
                                    right = qrTabFocus
                                }
                                .onFocusChanged { if (it.isFocused) selectedTab = com.tivipro.ui.viewmodel.LoginTab.M3U },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (selectedTab == com.tivipro.ui.viewmodel.LoginTab.M3U) OrangePrimary else Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Enlace M3U", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        FocusableCard(
                            onClick = { selectedTab = com.tivipro.ui.viewmodel.LoginTab.QR },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCompactHeight) 32.dp else 36.dp)
                                .focusRequester(qrTabFocus)
                                .focusProperties { left = m3uTabFocus }
                                .onFocusChanged { if (it.isFocused) selectedTab = com.tivipro.ui.viewmodel.LoginTab.QR },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (selectedTab == com.tivipro.ui.viewmodel.LoginTab.QR) OrangePrimary else Color(0xFF2A2A40))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Iniciar con QR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    when (selectedTab) {
                        com.tivipro.ui.viewmodel.LoginTab.XTREAM -> {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("URL del Servidor (http://...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = TextMuted)
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Usuario") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = TextMuted)
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = TextMuted)
                            )

                            FocusableCard(
                                onClick = {
                                    val plName = name.ifBlank { "Xtream Playlist" }
                                    val targetId = initialPlaylist?.id ?: java.util.UUID.randomUUID().toString()
                                    val isCurrentlyActive = initialPlaylist?.isActive ?: false
                                    val newPl = com.tivipro.model.SavedPlaylist(
                                        id = targetId,
                                        name = plName,
                                        mode = DataSourceMode.XTREAM,
                                        serverUrl = serverUrl,
                                        username = username,
                                        password = password,
                                        m3uUrl = m3uUrl,
                                        isActive = isCurrentlyActive
                                    )
                                    onSave(newPl)
                                },
                                modifier = Modifier.fillMaxWidth().height(if (isCompactHeight) 38.dp else 42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("GUARDAR Y CONECTAR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        com.tivipro.ui.viewmodel.LoginTab.M3U -> {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("URL de la lista M3U") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = TextMuted)
                            )

                            FocusableCard(
                                onClick = {
                                    val plName = name.ifBlank { "M3U Playlist" }
                                    val targetId = initialPlaylist?.id ?: java.util.UUID.randomUUID().toString()
                                    val isCurrentlyActive = initialPlaylist?.isActive ?: false
                                    val newPl = com.tivipro.model.SavedPlaylist(
                                        id = targetId,
                                        name = plName,
                                        mode = DataSourceMode.M3U,
                                        serverUrl = serverUrl,
                                        username = username,
                                        password = password,
                                        m3uUrl = m3uUrl,
                                        isActive = isCurrentlyActive
                                    )
                                    onSave(newPl)
                                },
                                modifier = Modifier.fillMaxWidth().height(if (isCompactHeight) 38.dp else 42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = OrangePrimary)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("GUARDAR Y CONECTAR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        com.tivipro.ui.viewmodel.LoginTab.QR -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Añadir Playlist escaneando código QR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Box(
                                    modifier = Modifier
                                        .size(if (isCompactHeight) 120.dp else 160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(6.dp),
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
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutInstructionsDialog(
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints {
            val isCompactHeight = maxHeight < 480.dp
            val surfaceWidth = if (isCompactHeight) 0.95f else 0.90f
            val surfaceHeight = if (isCompactHeight) 0.95f else 0.85f
            val outerPadding = if (isCompactHeight) 12.dp else 20.dp
            val itemSpacing = if (isCompactHeight) 4.dp else 8.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(surfaceWidth)
                    .fillMaxHeight(surfaceHeight)
                    .clip(RoundedCornerShape(18.dp)),
                color = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(outerPadding)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(if (isCompactHeight) 20.dp else 24.dp)
                            )
                            Text(
                                text = "GUÍA RÁPIDA DE USO",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isCompactHeight) 14.sp else 16.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(if (isCompactHeight) 32.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompactHeight) 18.dp else 24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 10.dp))

                    // Instruction Items List
                    val instructions = remember {
                        listOf(
                            "⚡ Salto rápido al primer y último canal" to "Al pulsar dos veces rápido hacia Arriba (▲) o hacia Abajo (▼) en la lista de canales (doble toque en la cruceta D-Pad o teclado), la lista salta instantáneamente al primer o al último canal.",
                            "⭐ Marcado rápido de Favoritos" to "Mueve la cruceta D-Pad hacia la Derecha (►) desde la tarjeta del canal para enfocar el botón de corazón y presiona OK para marcarlo o desmarcarlo de Favoritos.",
                            "🔔 Alertas de eventos deportivos por Telegram" to "Abre la web mediante el código QR y sigue estos pasos:\n1. Inicia conversación con @tivipro_bot y pulsa INICIAR.\n2. Consulta tu ID numérico en @userinfobot, pulsa INICIAR y copia el ID.\n3. Introduce tu ID en la web y pulsa guardar para vincularlo a TiviPro.",
                            "👁️ Orden y visibilidad de categorías" to "En Gestión de Categorías: usa las flechas dobles (⏫ / ⏬) para enviar la categoría al principio o al final de la lista, las flechas simples (↑ / ↓) para subir o bajar un solo elemento, y el botón Visible/Oculta para mostrar u ocultar la categoría.",
                            "🎥 Modo Multiview (2 Canales)" to "Activa el botón Multiview en la cabecera de Televisión para sintonizar y ver dos canales simultáneos en pantalla dividida con conmutación de audio.",
                            "🔄 Múltiples Playlists" to "Añade y gestiona hasta 5 servidores o listas M3U diferentes desde la sección Ajustes de Servidor."
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        itemsIndexed(instructions) { index, (title, desc) ->
                            InstructionItem(
                                title = title,
                                description = desc,
                                isCompact = isCompactHeight,
                                modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

                    // Bottom Close Button
                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 36.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "ENTENDIDO",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactHeight) 11.sp else 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionItem(
    title: String,
    description: String,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        onClick = {},
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkCard),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp)) {
            Text(
                text = title,
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
            Text(
                text = description,
                fontSize = if (isCompact) 10.5.sp else 11.sp,
                color = TextMuted,
                lineHeight = if (isCompact) 13.sp else 15.sp
            )
        }
    }
}

@Composable
fun SupportQrDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val telegramUrl = "https://t.me/TiviProapp"
    val qrCodeUrl = remember { com.tivipro.ui.components.QrCodeGenerator.getQrCodeUrl(telegramUrl) }

    fun openTelegramLink() {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(telegramUrl)
            )
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            val isCompactHeight = maxHeight < 480.dp
            val qrSize = if (isCompactHeight) 130.dp else 190.dp
            val outerPadding = if (isCompactHeight) 12.dp else 20.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isCompactHeight) 0.85f else 0.9f)
                    .clip(RoundedCornerShape(18.dp)),
                color = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .padding(outerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOPORTE TÉCNICO TELEGRAM",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactHeight) 13.sp else 15.sp
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(if (isCompactHeight) 32.dp else 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompactHeight) 18.dp else 24.dp)
                            )
                        }
                    }

                    Text(
                        text = "Escanea el código QR o pulsa el enlace para ponerte en contacto con nosotros:",
                        color = TextMuted,
                        fontSize = if (isCompactHeight) 11.sp else 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    FocusableCard(
                        onClick = { openTelegramLink() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.size(qrSize)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = qrCodeUrl,
                                contentDescription = "Código QR Soporte Telegram",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    FocusableCard(
                        onClick = { openTelegramLink() },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B3C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "https://t.me/TiviProapp",
                                color = Color(0xFF29B6F6),
                                fontSize = if (isCompactHeight) 12.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        }
                    }

                    FocusableCard(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 36.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A40))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "CERRAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactHeight) 11.sp else 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
