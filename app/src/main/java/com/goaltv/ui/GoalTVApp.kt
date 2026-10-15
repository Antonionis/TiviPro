package com.goaltv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goaltv.data.network.UpdateInfo
import com.goaltv.data.network.UpdateManager
import com.goaltv.ui.components.UpdateDialog
import com.goaltv.data.repository.DataRepositoryImpl
import com.goaltv.ui.screens.DashboardScreen
import com.goaltv.ui.screens.LiveTvScreen
import com.goaltv.ui.screens.LoginScreen
import com.goaltv.ui.screens.MoviesScreen
import com.goaltv.ui.screens.SeriesScreen
import com.goaltv.ui.screens.SettingsScreen
import com.goaltv.ui.screens.SportsScreen
import com.goaltv.ui.theme.DarkBackground
import com.goaltv.ui.theme.GoaltvTheme
import com.goaltv.ui.viewmodel.AppScreen
import com.goaltv.ui.viewmodel.AuthViewModel
import com.goaltv.ui.viewmodel.LiveTvViewModel
import com.goaltv.ui.viewmodel.MainViewModel
import com.goaltv.ui.viewmodel.SportsViewModel
import com.goaltv.ui.viewmodel.VodViewModel

@Composable
fun GoalTVApp(
    modifier: Modifier = Modifier
) {
    GoaltvTheme {
        val context = LocalContext.current.applicationContext
        // Shared repository instance with persistent context
        val repository = remember { DataRepositoryImpl(context) }

        val mainViewModel: MainViewModel = viewModel { MainViewModel(repository) }
        val authViewModel: AuthViewModel = viewModel { AuthViewModel(repository) }
        val liveTvViewModel: LiveTvViewModel = viewModel { LiveTvViewModel(repository) }
        val vodViewModel: VodViewModel = viewModel { VodViewModel(repository) }
        val sportsViewModel: SportsViewModel = viewModel { SportsViewModel(repository) }

        val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
        val currentScreen by mainViewModel.currentScreen.collectAsState()
        val activePlaybackTarget by mainViewModel.activePlaybackTarget.collectAsState()

        // Check for App Updates on Startup
        val activityContext = LocalContext.current
        var pendingUpdateInfo by remember { androidx.compose.runtime.mutableStateOf<UpdateInfo?>(null) }

        LaunchedEffect(Unit) {
            try {
                val updateInfo = UpdateManager.checkForUpdate(activityContext)
                if (updateInfo.isUpdateAvailable) {
                    pendingUpdateInfo = updateInfo
                }
            } catch (_: Exception) {}
        }

        // Obligatorily refresh channel list & server data on app launch when logged in
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) {
                mainViewModel.navigateTo(AppScreen.DASHBOARD)
                mainViewModel.reloadPlaylist()
                liveTvViewModel.loadData()
                vodViewModel.loadData()
                sportsViewModel.loadSportsEvents()
            } else {
                mainViewModel.navigateTo(AppScreen.LOGIN)
            }
        }

        // Handle system back button navigation safely
        val backHandlerEnabled = activePlaybackTarget != null || currentScreen != AppScreen.DASHBOARD

        BackHandler(enabled = backHandlerEnabled) {
            try {
                if (activePlaybackTarget != null) {
                    mainViewModel.clearPlayback()
                } else if (currentScreen != AppScreen.DASHBOARD) {
                    mainViewModel.navigateTo(AppScreen.DASHBOARD)
                }
            } catch (_: Exception) {}
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.LOGIN -> {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                liveTvViewModel.loadData()
                                vodViewModel.loadData()
                                sportsViewModel.loadSportsEvents()
                                mainViewModel.navigateTo(AppScreen.DASHBOARD)
                            }
                        )
                    }
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            liveTvViewModel = liveTvViewModel,
                            vodViewModel = vodViewModel
                        )
                    }
                    AppScreen.LIVE_TV, AppScreen.EPG -> {
                        LiveTvScreen(
                            viewModel = liveTvViewModel,
                            onChannelPlay = { channel ->
                                mainViewModel.playChannel(channel)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.MOVIES -> {
                        MoviesScreen(
                            viewModel = vodViewModel,
                            onMoviePlay = { movie ->
                                mainViewModel.playMovie(movie)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.SERIES -> {
                        SeriesScreen(
                            viewModel = vodViewModel,
                            onEpisodePlay = { ep ->
                                mainViewModel.playStream(
                                    title = ep.title,
                                    streamUrl = ep.streamUrl,
                                    subTitle = "S${ep.seasonNum} E${ep.episodeNum} • ${ep.duration ?: ""}",
                                    isLive = false,
                                    logoUrl = ep.iconUrl
                                )
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.SPORTS -> {
                        SportsScreen(
                            viewModel = sportsViewModel,
                            onEventPlay = { event ->
                                mainViewModel.playStream(
                                    title = event.title,
                                    streamUrl = event.streamUrl,
                                    subTitle = "${event.competition} • ${event.channelName}",
                                    isLive = true,
                                    logoUrl = event.iconUrl
                                )
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                    AppScreen.FAVORITES -> {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            liveTvViewModel = liveTvViewModel,
                            vodViewModel = vodViewModel
                        )
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = mainViewModel,
                            onLogoutClick = {
                                authViewModel.logout()
                                mainViewModel.navigateTo(AppScreen.LOGIN)
                            },
                            onBackClick = { mainViewModel.navigateTo(AppScreen.DASHBOARD) }
                        )
                    }
                }
            }

            // Full Screen Player Overlay when activePlaybackTarget is present
            val activeTarget = activePlaybackTarget
            if (activeTarget != null) {
                com.goaltv.ui.player.PlayerScreen(
                    playbackTarget = activeTarget,
                    repository = repository,
                    onChannelChange = { ch -> liveTvViewModel.selectChannel(ch) },
                    onBackClick = { mainViewModel.clearPlayback() }
                )
            }

            // Update Dialog Popup
            val updateInfo = pendingUpdateInfo
            if (updateInfo != null) {
                UpdateDialog(
                    updateInfo = updateInfo,
                    onDismiss = { pendingUpdateInfo = null }
                )
            }
        }
    }
}
