package com.tivipro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tivipro.data.network.UpdateInfo
import com.tivipro.data.network.UpdateManager
import com.tivipro.ui.theme.OrangePrimary
import com.tivipro.ui.theme.SurfaceDark
import com.tivipro.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    isReleaseNotesOnly: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isDownloading) {
        if (!isDownloading) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isReleaseNotesOnly) "Novedades Versión ${updateInfo.versionName}" else "¡Nueva Versión ${updateInfo.versionName}!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Text(
                    text = "Novedades y cambios:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Scrollable Changelog
                val scrollState = rememberScrollState()
                var isChangelogFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .onFocusChanged { isChangelogFocused = it.isFocused }
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                val keyCode = event.nativeKeyEvent.keyCode
                                if (keyCode == 20) { // D-Pad Down
                                    if (scrollState.value < scrollState.maxValue) {
                                        scope.launch { scrollState.animateScrollBy(120f) }
                                        true
                                    } else false
                                } else if (keyCode == 19) { // D-Pad Up
                                    if (scrollState.value > 0) {
                                        scope.launch { scrollState.animateScrollBy(-120f) }
                                        true
                                    } else false
                                } else false
                            } else false
                        }
                        .border(
                            width = if (isChangelogFocused) 2.dp else 1.dp,
                            color = if (isChangelogFocused) OrangePrimary else Color(0xFF2A2A40),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(Color(0xFF1E1E2C), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = updateInfo.changelog,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                if (isDownloading && !isReleaseNotesOnly) {
                    Column {
                        Text(
                            text = "Descargando actualización... (${(downloadProgress * 100).toInt()}%)",
                            color = OrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = OrangePrimary,
                            trackColor = Color(0xFF2A2A3E)
                        )
                    }
                }

                // Bottom Buttons (ACTUALIZAR / CERRAR)
                if (isReleaseNotesOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(focusRequester),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CERRAR", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else if (!isDownloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isDownloading = true
                                scope.launch {
                                    UpdateManager.downloadAndInstallApk(
                                        context = context,
                                        apkUrl = updateInfo.apkUrl,
                                        onProgress = { downloadProgress = it }
                                    )
                                    isDownloading = false
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.focusRequester(focusRequester),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ACTUALIZAR", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CANCELAR", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
