package com.tivipro.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.tivipro.model.AuthResponse
import com.tivipro.model.Category
import com.tivipro.model.CategoryType
import com.tivipro.model.Episode
import com.tivipro.model.EpgProgram
import com.tivipro.model.LiveChannel
import com.tivipro.model.Series
import com.tivipro.model.SportsEvent
import com.tivipro.model.VodMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class DataSourceMode {
    MOCK,
    XTREAM,
    M3U
}

enum class EpgSourceMode {
    TIVIPRO,
    PROVIDER
}

data class XtreamServerAccount(
    val serverUrl: String,
    val username: String,
    val password: String,
    val authResponse: AuthResponse? = null
)

data class SearchResult(
    val channels: List<LiveChannel> = emptyList(),
    val movies: List<VodMovie> = emptyList(),
    val series: List<Series> = emptyList()
)

interface DataRepository {
    val isLoggedIn: StateFlow<Boolean>
    val activeMode: StateFlow<DataSourceMode>
    val activeXtreamAccount: StateFlow<XtreamServerAccount?>
    val activePlaylist: StateFlow<com.tivipro.model.SavedPlaylist?>
    val isDohEnabled: StateFlow<Boolean>
    val epgSourceMode: StateFlow<EpgSourceMode>
    val isPremium: StateFlow<Boolean>

    fun setDohEnabled(enabled: Boolean)
    fun setEpgSourceMode(mode: EpgSourceMode)
    fun setPremiumStatus(isPremium: Boolean)
    fun getOkHttpClient(): okhttp3.OkHttpClient
    fun getPlayerOkHttpClient(): okhttp3.OkHttpClient

    fun saveSession(serverUrl: String, username: String, pass: String, authResponse: AuthResponse? = null)
    fun clearSession()

    suspend fun setMode(mode: DataSourceMode)
    suspend fun connectXtreamServer(serverUrl: String, username: String, password: String): Result<AuthResponse>
    suspend fun loadM3uFromUrl(url: String): Result<Unit>
    suspend fun loadM3uFromText(content: String): Result<Unit>

    suspend fun getLiveCategories(): List<Category>
    suspend fun getLiveChannels(categoryId: String? = null): List<LiveChannel>
    suspend fun getVodCategories(): List<Category>
    suspend fun getVodMovies(categoryId: String? = null): List<VodMovie>
    suspend fun getSeriesCategories(): List<Category>
    suspend fun getSeriesList(categoryId: String? = null): List<Series>
    suspend fun getSeriesEpisodes(seriesId: String): List<Episode>
    suspend fun getEpgForChannel(channelId: String, streamId: String? = null, forceProvider: Boolean = false): List<EpgProgram>
    suspend fun getSportsEvents(): List<SportsEvent>

    fun searchContent(query: String): SearchResult
    fun toggleFavoriteChannel(channelId: String)
    fun toggleFavoriteMovie(movieId: String)
    fun toggleFavoriteSeries(seriesId: String)
    fun getFavoriteChannels(): List<LiveChannel>
    fun getFavoriteMovies(): List<VodMovie>
    fun getFavoriteSeries(): List<Series>

    suspend fun refreshData()
    fun getHiddenCategoryIds(): Set<String>
    fun toggleCategoryHidden(categoryId: String)
    suspend fun getAllLiveCategories(): List<Category>
    fun getCategoryOrder(): List<String>
    fun saveCategoryOrder(order: List<String>)
    fun exportLocalBackup(): Boolean
    fun importLocalBackup(): Boolean
    fun exportBackupToJson(): String
    fun importBackupFromJson(jsonStr: String): Boolean

    fun getSavedPlaylists(): List<com.tivipro.model.SavedPlaylist>
    fun savePlaylist(playlist: com.tivipro.model.SavedPlaylist)
    fun deletePlaylist(playlistId: String)
    suspend fun switchPlaylist(playlistId: String): Boolean
    suspend fun getCatchupChannels(): List<LiveChannel>
    fun buildCatchupStreamUrl(streamId: String, startTimestampSec: Long, durationMinutes: Long): String
}

class DataRepositoryImpl(
    context: Context? = null,
    private val xtreamRepository: XtreamRepository = XtreamRepositoryImpl(),
    private val m3uRepository: M3URepository = M3URepositoryImpl(),
    private val sportsRepository: SportsRepository = SportsRepositoryImpl()
) : DataRepository {

    private val appContext: Context? = context ?: run {
        try { com.tivipro.TiviProApplication.instance } catch (_: Exception) { null }
    }

    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("tivipro_session", Context.MODE_PRIVATE)
    }

    private val _isLoggedIn = MutableStateFlow(
        (!prefs?.getString("username", "").isNullOrBlank() && !prefs?.getString("password", "").isNullOrBlank()) ||
        !prefs?.getString("m3u_url", "").isNullOrBlank() ||
        prefs?.getBoolean("is_logged_in", false) == true
    )
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _activeMode = MutableStateFlow(
        if (_isLoggedIn.value) {
            val savedMode = prefs?.getString("data_source_mode", "XTREAM")
            if (savedMode == "M3U" || (!prefs?.getString("m3u_url", "").isNullOrBlank() && prefs?.getString("username", "").isNullOrBlank())) {
                DataSourceMode.M3U
            } else {
                DataSourceMode.XTREAM
            }
        } else {
            DataSourceMode.MOCK
        }
    )
    override val activeMode: StateFlow<DataSourceMode> = _activeMode.asStateFlow()

    private val _activeXtreamAccount = MutableStateFlow<XtreamServerAccount?>(
        if (_isLoggedIn.value) {
            val savedServer = prefs?.getString("server_url", "") ?: ""
            val savedUser = prefs?.getString("username", "") ?: ""
            val savedPass = prefs?.getString("password", "") ?: ""
            XtreamServerAccount(savedServer, savedUser, savedPass)
        } else null
    )
    override val activeXtreamAccount: StateFlow<XtreamServerAccount?> = _activeXtreamAccount.asStateFlow()

    private val _activePlaylist = MutableStateFlow<com.tivipro.model.SavedPlaylist?>(null)
    override val activePlaylist: StateFlow<com.tivipro.model.SavedPlaylist?> = _activePlaylist.asStateFlow()

    override val isDohEnabled: StateFlow<Boolean> = com.tivipro.data.network.NetworkModule.isDohEnabled

    override fun setDohEnabled(enabled: Boolean) {
        com.tivipro.data.network.NetworkModule.setDohEnabled(enabled)
    }

    private val _epgSourceMode = MutableStateFlow<EpgSourceMode>(
        if (prefs?.getString("epg_source_mode", "PROVIDER") == "TIVIPRO") EpgSourceMode.TIVIPRO else EpgSourceMode.PROVIDER
    )
    override val epgSourceMode: StateFlow<EpgSourceMode> = _epgSourceMode.asStateFlow()

    override fun setEpgSourceMode(mode: EpgSourceMode) {
        _epgSourceMode.value = mode
        prefs?.edit()?.putString("epg_source_mode", mode.name)?.apply()
    }

    private fun isTrialActive(context: Context?): Boolean {
        if (context == null) return false
        val prefsDev = context.getSharedPreferences("tivipro_device_prefs", Context.MODE_PRIVATE)
        var installMs = prefsDev.getLong("first_install_timestamp_ms", 0L)
        if (installMs == 0L) {
            installMs = System.currentTimeMillis()
            prefsDev.edit().putLong("first_install_timestamp_ms", installMs).apply()
        }
        return (System.currentTimeMillis() - installMs) < (72 * 3600 * 1000L)
    }

    private val _isPremium = MutableStateFlow<Boolean>(
        prefs?.getBoolean("is_premium", false) == true ||
        appContext?.getSharedPreferences("tivipro_device_prefs", Context.MODE_PRIVATE)?.getBoolean("is_premium", false) == true ||
        isTrialActive(appContext)
    )
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override fun setPremiumStatus(isPremium: Boolean) {
        _isPremium.value = isPremium
        prefs?.edit()?.putBoolean("is_premium", isPremium)?.apply()
        try {
            appContext?.getSharedPreferences("tivipro_device_prefs", Context.MODE_PRIVATE)?.edit()?.putBoolean("is_premium", isPremium)?.apply()
        } catch (_: Exception) {}

        if (!isPremium) {
            saveCategoryOrder(emptyList())
            val currentList = getSavedPlaylists()
            if (currentList.size > 1) {
                val active = currentList.firstOrNull { it.isActive } ?: currentList.firstOrNull()
                if (active != null) {
                    persistPlaylists(listOf(active.copy(isActive = true)))
                }
            }
        }
    }

    override fun getOkHttpClient(): okhttp3.OkHttpClient {
        return com.tivipro.data.network.NetworkModule.getOkHttpClient()
    }

    override fun getPlayerOkHttpClient(): okhttp3.OkHttpClient {
        return com.tivipro.data.network.NetworkModule.getPlayerOkHttpClient()
    }

    private var cachedM3uResult: com.tivipro.data.parser.M3UParseResult? = null

    private val favoriteChannelIds = mutableSetOf<String>().apply {
        addAll(prefs?.getStringSet("favorite_channel_ids", emptySet()) ?: emptySet())
    }
    private val favoriteMovieIds = mutableSetOf<String>().apply {
        addAll(prefs?.getStringSet("favorite_movie_ids", emptySet()) ?: emptySet())
    }
    private val favoriteSeriesIds = mutableSetOf<String>().apply {
        addAll(prefs?.getStringSet("favorite_series_ids", emptySet()) ?: emptySet())
    }

    // In-memory cache for loaded real data
    @Volatile private var xtreamLiveCategories = listOf<Category>()
    @Volatile private var xtreamLiveChannels = listOf<LiveChannel>()
    @Volatile private var xtreamVodCategories = listOf<Category>()
    @Volatile private var xtreamVodMovies = listOf<VodMovie>()
    @Volatile private var xtreamSeriesCategories = listOf<Category>()
    @Volatile private var xtreamSeriesList = listOf<Series>()

    override fun saveSession(serverUrl: String, username: String, pass: String, authResponse: AuthResponse?) {
        _isLoggedIn.value = true
        val acc = XtreamServerAccount(serverUrl, username, pass, authResponse)
        _activeXtreamAccount.value = acc
        _activeMode.value = DataSourceMode.XTREAM
        prefs?.edit()?.apply {
            putBoolean("is_logged_in", true)
            putString("server_url", serverUrl)
            putString("username", username)
            putString("password", pass)
            putString("data_source_mode", "XTREAM")
            apply()
        }
    }

    override fun clearSession() {
        _isLoggedIn.value = false
        _activeXtreamAccount.value = null
        _activeMode.value = DataSourceMode.MOCK
        cachedM3uResult = null
        xtreamLiveCategories = emptyList()
        xtreamLiveChannels = emptyList()
        xtreamVodCategories = emptyList()
        xtreamVodMovies = emptyList()
        xtreamSeriesCategories = emptyList()
        xtreamSeriesList = emptyList()
        favoriteChannelIds.clear()
        favoriteMovieIds.clear()
        favoriteSeriesIds.clear()
        prefs?.edit()?.apply {
            putBoolean("is_logged_in", false)
            putString("username", "")
            putString("password", "")
            putString("m3u_url", "")
            putString("data_source_mode", "MOCK")
            apply()
        }
    }

    override suspend fun setMode(mode: DataSourceMode) {
        _activeMode.value = mode
        if (mode == DataSourceMode.MOCK) {
            clearSession()
        }
    }

    override suspend fun connectXtreamServer(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> {
        val result = xtreamRepository.authenticate(serverUrl, username, password)
        result.onSuccess { auth ->
            saveSession(serverUrl, username, password, auth)

            // Pre-fetch categories and lists in background if authenticated
            runCatching {
                xtreamLiveCategories = xtreamRepository.getLiveCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamLiveChannels = xtreamRepository.getLiveChannels(serverUrl, username, password).getOrDefault(emptyList())
                xtreamVodCategories = xtreamRepository.getVodCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamVodMovies = xtreamRepository.getVodMovies(serverUrl, username, password).getOrDefault(emptyList())
                xtreamSeriesCategories = xtreamRepository.getSeriesCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamSeriesList = xtreamRepository.getSeriesList(serverUrl, username, password).getOrDefault(emptyList())
            }
        }
        return result
    }

    override suspend fun loadM3uFromUrl(url: String): Result<Unit> {
        val result = m3uRepository.fetchAndParseFromUrl(url)
        return result.map { parseResult ->
            cachedM3uResult = parseResult
            _activeMode.value = DataSourceMode.M3U
            _isLoggedIn.value = true
            prefs?.edit()?.apply {
                putBoolean("is_logged_in", true)
                putString("m3u_url", url)
                putString("data_source_mode", "M3U")
                apply()
            }
        }
    }

    override suspend fun loadM3uFromText(content: String): Result<Unit> {
        val result = m3uRepository.parseFromText(content)
        return result.map { parseResult ->
            cachedM3uResult = parseResult
            _activeMode.value = DataSourceMode.M3U
        }
    }

    override fun getHiddenCategoryIds(): Set<String> {
        return prefs?.getStringSet("hidden_category_ids", emptySet()) ?: emptySet()
    }

    override fun exportBackupToJson(): String {
        val json = org.json.JSONObject().apply {
            put("category_order", prefs?.getString("category_order", ""))

            val hiddenSet = prefs?.getStringSet("hidden_category_ids", emptySet()) ?: emptySet()
            put("hidden_category_ids", org.json.JSONArray(hiddenSet))

            val favChannels = favoriteChannelIds
            put("favorite_channels", org.json.JSONArray(favChannels))

            val favMovies = favoriteMovieIds
            put("favorite_movies", org.json.JSONArray(favMovies))

            val favSeries = favoriteSeriesIds
            put("favorite_series", org.json.JSONArray(favSeries))
        }
        return json.toString()
    }

    override fun importBackupFromJson(jsonStr: String): Boolean {
        return try {
            if (jsonStr.isBlank()) return false
            val json = org.json.JSONObject(jsonStr)

            val categoryOrder = json.optString("category_order", "")

            val hiddenArray = json.optJSONArray("hidden_category_ids")
            val hiddenSet = mutableSetOf<String>()
            if (hiddenArray != null) {
                for (i in 0 until hiddenArray.length()) {
                    hiddenSet.add(hiddenArray.getString(i))
                }
            }

            val favChannelsArray = json.optJSONArray("favorite_channels")
            if (favChannelsArray != null) {
                favoriteChannelIds.clear()
                for (i in 0 until favChannelsArray.length()) {
                    favoriteChannelIds.add(favChannelsArray.getString(i))
                }
                prefs?.edit()?.putStringSet("favorite_channel_ids", favoriteChannelIds.toSet())?.apply()
            }

            val favMoviesArray = json.optJSONArray("favorite_movies")
            if (favMoviesArray != null) {
                favoriteMovieIds.clear()
                for (i in 0 until favMoviesArray.length()) {
                    favoriteMovieIds.add(favMoviesArray.getString(i))
                }
                prefs?.edit()?.putStringSet("favorite_movie_ids", favoriteMovieIds.toSet())?.apply()
            }

            val favSeriesArray = json.optJSONArray("favorite_series")
            if (favSeriesArray != null) {
                favoriteSeriesIds.clear()
                for (i in 0 until favSeriesArray.length()) {
                    favoriteSeriesIds.add(favSeriesArray.getString(i))
                }
                prefs?.edit()?.putStringSet("favorite_series_ids", favoriteSeriesIds.toSet())?.apply()
            }

            val p = prefs
            if (p != null) {
                val editor = p.edit()
                if (categoryOrder.isNotBlank()) editor.putString("category_order", categoryOrder)
                if (hiddenSet.isNotEmpty()) editor.putStringSet("hidden_category_ids", hiddenSet)
                editor.commit()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun exportLocalBackup(): Boolean {
        return try {
            val jsonStr = exportBackupToJson()
            var savedAtLeastOne = false

            val p = prefs
            if (p != null) {
                p.edit().putString("tivipro_local_backup", jsonStr).commit()
                savedAtLeastOne = true
            }

            // Save as goaltv_backup.json and tivipro_backup.json
            val filenames = listOf("goaltv_backup.json", "tivipro_backup.json")

            // 1. Try Internal Files Dir
            try {
                appContext?.let { ctx ->
                    for (fn in filenames) {
                        val file = java.io.File(ctx.filesDir, fn)
                        if (file.exists()) file.delete()
                        file.writeText(jsonStr)
                    }
                    savedAtLeastOne = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try App External Files Dir
            try {
                appContext?.let { ctx ->
                    val extDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: ctx.getExternalFilesDir(null)
                    if (extDir != null) {
                        if (!extDir.exists()) extDir.mkdirs()
                        for (fn in filenames) {
                            val file = java.io.File(extDir, fn)
                            if (file.exists()) file.delete()
                            file.writeText(jsonStr)
                        }
                        savedAtLeastOne = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Try Public Downloads
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    for (fn in filenames) {
                        val file = java.io.File(downloadsDir, fn)
                        if (file.exists()) file.delete()
                        file.writeText(jsonStr)
                    }
                    savedAtLeastOne = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            savedAtLeastOne
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun importLocalBackup(): Boolean {
        return try {
            var rawJson = ""
            val filenames = listOf("tivipro_backup.json", "goaltv_backup.json")

            // 1. Check Public Downloads first (for tivipro_backup.json then goaltv_backup.json)
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                for (fn in filenames) {
                    val pubFile = java.io.File(downloadsDir, fn)
                    if (pubFile.exists() && pubFile.length() > 0) {
                        rawJson = pubFile.readText()
                        if (rawJson.isNotBlank()) break
                    }
                }
            }

            // 2. Check App External Files Dir
            if (rawJson.isBlank()) {
                appContext?.let { ctx ->
                    val extDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: ctx.getExternalFilesDir(null)
                    if (extDir != null && extDir.exists()) {
                        for (fn in filenames) {
                            val extFile = java.io.File(extDir, fn)
                            if (extFile.exists() && extFile.length() > 0) {
                                rawJson = extFile.readText()
                                if (rawJson.isNotBlank()) break
                            }
                        }
                    }
                }
            }

            // 3. Check Internal Files Dir
            if (rawJson.isBlank()) {
                appContext?.let { ctx ->
                    for (fn in filenames) {
                        val intFile = java.io.File(ctx.filesDir, fn)
                        if (intFile.exists() && intFile.length() > 0) {
                            rawJson = intFile.readText()
                            if (rawJson.isNotBlank()) break
                        }
                    }
                }
            }

            // 4. Check SharedPreferences Backup
            if (rawJson.isBlank()) {
                rawJson = prefs?.getString("tivipro_local_backup", "") ?: ""
            }

            if (rawJson.isBlank()) return false
            importBackupFromJson(rawJson)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun toggleCategoryHidden(categoryId: String) {
        val current = getHiddenCategoryIds().toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        prefs?.edit()?.putStringSet("hidden_category_ids", current)?.apply()
    }

    override suspend fun refreshData() {
        if (_activeMode.value == DataSourceMode.M3U) {
            val savedM3uUrl = prefs?.getString("m3u_url", "") ?: ""
            if (savedM3uUrl.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val result = m3uRepository.fetchAndParseFromUrl(savedM3uUrl)
                        result.getOrNull()?.let { parseResult ->
                            cachedM3uResult = parseResult
                        }
                    }
                }
            }
            return
        }

        val acc = _activeXtreamAccount.value ?: run {
            val savedServer = prefs?.getString("server_url", "") ?: ""
            val savedUser = prefs?.getString("username", "") ?: ""
            val savedPass = prefs?.getString("password", "") ?: ""
            if (savedServer.isNotBlank() && savedUser.isNotBlank() && savedPass.isNotBlank()) {
                val newAcc = XtreamServerAccount(savedServer, savedUser, savedPass)
                _activeXtreamAccount.value = newAcc
                newAcc
            } else null
        } ?: return

        withContext(Dispatchers.IO) {
            runCatching {
                // Obligatory real HTTP network authentication handshake on launch
                val authResult = xtreamRepository.authenticate(acc.serverUrl, acc.username, acc.password)
                val authRes = authResult.getOrNull()

                if (authRes != null && authRes.userInfo?.auth == 1) {
                    _activeXtreamAccount.value = acc.copy(authResponse = authRes)
                    _isLoggedIn.value = true
                    prefs?.edit()?.putBoolean("is_logged_in", true)?.apply()
                } else if (authRes != null && authRes.userInfo?.auth == 0) {
                    _isLoggedIn.value = false
                    prefs?.edit()?.putBoolean("is_logged_in", false)?.apply()
                    return@runCatching
                } else if (authRes != null) {
                    _activeXtreamAccount.value = acc.copy(authResponse = authRes)
                }

                coroutineScope {
                    val liveCatsDeferred = async { xtreamRepository.getLiveCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }
                    val liveChsDeferred = async { xtreamRepository.getLiveChannels(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }
                    val vodCatsDeferred = async { xtreamRepository.getVodCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }
                    val vodMovsDeferred = async { xtreamRepository.getVodMovies(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }
                    val serCatsDeferred = async { xtreamRepository.getSeriesCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }
                    val serListDeferred = async { xtreamRepository.getSeriesList(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList()) }

                    val liveCats = liveCatsDeferred.await()
                    val liveChs = liveChsDeferred.await()
                    val vodCats = vodCatsDeferred.await()
                    val vodMovs = vodMovsDeferred.await()
                    val serCats = serCatsDeferred.await()
                    val serList = serListDeferred.await()

                    if (liveCats.isNotEmpty()) xtreamLiveCategories = liveCats
                    if (liveChs.isNotEmpty()) xtreamLiveChannels = liveChs
                    if (vodCats.isNotEmpty()) xtreamVodCategories = vodCats
                    if (vodMovs.isNotEmpty()) xtreamVodMovies = vodMovs
                    if (serCats.isNotEmpty()) xtreamSeriesCategories = serCats
                    if (serList.isNotEmpty()) xtreamSeriesList = serList
                }
            }
        }
    }

    override fun getCategoryOrder(): List<String> {
        val raw = prefs?.getString("category_order", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
    }

    override fun saveCategoryOrder(order: List<String>) {
        prefs?.edit()?.putString("category_order", order.joinToString(","))?.apply()
    }

    override suspend fun getAllLiveCategories(): List<Category> {
        val rawCats = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamLiveCategories.isNotEmpty()) xtreamLiveCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getLiveCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamLiveCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.LIVE } ?: emptyList()
            }
        }
        val order = getCategoryOrder()
        return if (order.isNotEmpty()) {
            rawCats.sortedBy { cat ->
                val idx = order.indexOf(cat.id)
                if (idx >= 0) idx else 999
            }
        } else {
            rawCats
        }
    }

    override suspend fun getLiveCategories(): List<Category> {
        val hidden = getHiddenCategoryIds()
        val allCats = getAllLiveCategories()
        return allCats.filter { !hidden.contains(it.id) }
    }

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> {
        val hidden = getHiddenCategoryIds()
        val rawChannels = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamLiveChannels.isEmpty()) {
                    xtreamLiveChannels = xtreamRepository.getLiveChannels(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamLiveChannels
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.liveChannels ?: emptyList()
            }
        }

        val visibleChannels = rawChannels.filter { !hidden.contains(it.categoryId) }

        val filtered = if (categoryId.isNullOrBlank()) {
            visibleChannels
        } else {
            visibleChannels.filter { it.categoryId == categoryId }
        }

        return filtered.map { ch ->
            ch.copy(
                isFavorite = favoriteChannelIds.contains(ch.id)
            )
        }
    }

    override suspend fun getCatchupChannels(): List<LiveChannel> {
        val hidden = getHiddenCategoryIds()
        val allChannels = getLiveChannels()
        return allChannels.filter { it.tvArchive == 1 && !hidden.contains(it.categoryId) }
    }

    override fun buildCatchupStreamUrl(streamId: String, startTimestampSec: Long, durationMinutes: Long): String {
        val acc = _activeXtreamAccount.value ?: return ""
        return xtreamRepository.buildCatchupStreamUrl(
            acc.serverUrl, acc.username, acc.password, streamId, startTimestampSec, durationMinutes
        )
    }

    override suspend fun getVodCategories(): List<Category> {
        return when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamVodCategories.isNotEmpty()) xtreamVodCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getVodCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamVodCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.VOD } ?: emptyList()
            }
        }
    }

    override suspend fun getVodMovies(categoryId: String?): List<VodMovie> {
        val rawMovies = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamVodMovies.isEmpty()) {
                    xtreamVodMovies = xtreamRepository.getVodMovies(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamVodMovies
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.vodMovies ?: emptyList()
            }
        }

        val filtered = if (categoryId.isNullOrBlank()) {
            rawMovies
        } else {
            rawMovies.filter { it.categoryId == categoryId }
        }

        return filtered.map { m ->
            m.copy(isFavorite = favoriteMovieIds.contains(m.id))
        }
    }

    override suspend fun getSeriesCategories(): List<Category> {
        return when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamSeriesCategories.isNotEmpty()) xtreamSeriesCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getSeriesCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamSeriesCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.SERIES } ?: emptyList()
            }
        }
    }

    override suspend fun getSeriesList(categoryId: String?): List<Series> {
        val rawSeries = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamSeriesList.isEmpty()) {
                    xtreamSeriesList = xtreamRepository.getSeriesList(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamSeriesList
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.seriesList ?: emptyList()
            }
        }

        val filtered = if (categoryId.isNullOrBlank()) {
            rawSeries
        } else {
            rawSeries.filter { it.categoryId == categoryId }
        }

        return filtered.map { s ->
            s.copy(isFavorite = favoriteSeriesIds.contains(s.id))
        }
    }

    override suspend fun getSeriesEpisodes(seriesId: String): List<Episode> {
        if (_activeMode.value == DataSourceMode.XTREAM) {
            val acc = _activeXtreamAccount.value
            if (acc != null) {
                val remote = xtreamRepository.getSeriesEpisodes(acc.serverUrl, acc.username, acc.password, seriesId)
                if (remote.isSuccess) {
                    val list = remote.getOrDefault(emptyList())
                    if (list.isNotEmpty()) return list
                }
            }
        }
        return emptyList()
    }

    private fun cleanChannelNameForEpg(rawName: String): String {
        var clean = rawName
            .replace(Regex("""(?i)^\s*(ES\b|ES\s*4k-?|ES\s*fhd-?|ES\s*hd-?|ES\||ES:|-|\[ES]|\bHD\b|\bFHD\b|\b4K\b|\bSD\b)\s*"""), "")
            .replace(Regex("""(?i)\s*(HD|FHD|4K|SD|\d+k|\.tv)$"""), "")
            .replace(Regex("""(?i)\bmovistar\b"""), "m+")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase()

        if (clean == "tve 1" || clean == "tve1" || clean == "la1" || clean == "la 1") clean = "la 1"
        if (clean == "tve 2" || clean == "tve2" || clean == "la2" || clean == "la 2") clean = "la 2"
        return clean
    }

    private fun isChannelMatch(cleanTarget: String, cleanXmlName: String): Boolean {
        if (cleanTarget.isBlank() || cleanXmlName.isBlank()) return false

        val t = cleanTarget.replace(" ", "")
        val x = cleanXmlName.replace(" ", "")
        if (t == x) return true

        if (cleanTarget == "ten" || cleanXmlName == "ten") {
            return cleanTarget == "ten" && cleanXmlName == "ten"
        }

        if (cleanTarget == "la 1" || cleanXmlName == "la 1") {
            return cleanTarget.startsWith("la 1") && cleanXmlName.startsWith("la 1")
        }
        if (cleanTarget == "la 2" || cleanXmlName == "la 2") {
            return cleanTarget.startsWith("la 2") && cleanXmlName.startsWith("la 2")
        }

        // Strict channel number matching (e.g. "m+ liga de campeones 1" vs "2")
        val targetNum = Regex("""\b(\d+)\b""").find(cleanTarget)?.value
        val xmlNum = Regex("""\b(\d+)\b""").find(cleanXmlName)?.value
        if (targetNum != null || xmlNum != null) {
            if (targetNum != xmlNum) return false
        }

        // Avoid matching generic short names (e.g. "m+", "es", "la", "tv")
        if (cleanTarget.length < 4 || cleanXmlName.length < 4) return false

        // For substring matches, both cleanTarget and cleanXmlName must share the primary name token
        // and not differ by key suffixes (like "2", "3", "360", "bar", "latam")
        val targetTokens = cleanTarget.split(" ").filter { it.isNotBlank() }
        val xmlTokens = cleanXmlName.split(" ").filter { it.isNotBlank() }

        if (targetTokens.size > 1 && xmlTokens.size > 1) {
            val targetLast = targetTokens.last()
            val xmlLast = xmlTokens.last()
            if (targetLast != xmlLast && (targetLast.length <= 3 || xmlLast.length <= 3)) {
                return false
            }
        }

        return t.contains(x) || x.contains(t)
    }

    override suspend fun getEpgForChannel(channelId: String, streamId: String?, forceProvider: Boolean): List<EpgProgram> = withContext(Dispatchers.IO) {
        if (forceProvider || _epgSourceMode.value == EpgSourceMode.PROVIDER) {
            if (_activeMode.value == DataSourceMode.XTREAM) {
                if (xtreamLiveChannels.isEmpty()) {
                    getLiveChannels()
                }
                val acc = _activeXtreamAccount.value
                if (acc != null) {
                    val targetChannel = xtreamLiveChannels.firstOrNull {
                        it.id == channelId || it.epgId == channelId || (streamId != null && (it.id == streamId || it.epgId == streamId))
                    }
                    val sId = targetChannel?.id ?: streamId ?: channelId
                    val result = xtreamRepository.getShortEpg(acc.serverUrl, acc.username, acc.password, sId, limit = 10)
                    if (result.isSuccess) {
                        val providerProgs = result.getOrDefault(emptyList())
                        val nowSec = System.currentTimeMillis() / 1000L
                        return@withContext providerProgs.map { p ->
                            val live = nowSec in p.startTimestamp..p.endTimestamp
                            val catchup = if (targetChannel != null && targetChannel.tvArchive == 1 && p.endTimestamp < nowSec) {
                                val durationMins = ((p.endTimestamp - p.startTimestamp) / 60L).coerceAtLeast(1L)
                                xtreamRepository.buildCatchupStreamUrl(acc.serverUrl, acc.username, acc.password, targetChannel.id, p.startTimestamp, durationMins)
                            } else null

                            p.copy(isLive = live, catchupUrl = catchup)
                        }.sortedBy { it.startTimestamp }
                    }
                }
            }
            return@withContext emptyList()
        }

        // Default or Fallback: TiviPro EPG (External DobleM EPG)
        try {
            val xmlTvResult = sportsRepository.getXmlTvData()
            if (xmlTvResult.programs.isEmpty()) return@withContext emptyList()

            if (xtreamLiveChannels.isEmpty()) {
                getLiveChannels()
            }

            val targetChannel = xtreamLiveChannels.firstOrNull { it.id == channelId || it.epgId == channelId || (streamId != null && it.epgId == streamId) }
            val acc = _activeXtreamAccount.value
            val rawName = targetChannel?.name ?: channelId
            val cleanedName = cleanChannelNameForEpg(rawName)
            val tvgId = targetChannel?.epgId

            val matchedXmlChannelIds = xmlTvResult.channels.entries.filter { (id, ch) ->
                id.equals(tvgId, ignoreCase = true) ||
                id.equals(channelId, ignoreCase = true) ||
                ch.displayNames.any { displayName ->
                    val cleanDisplay = cleanChannelNameForEpg(displayName)
                    isChannelMatch(cleanedName, cleanDisplay)
                }
            }.map { it.key }.toSet()

            val matchingProgs = xmlTvResult.programs.filter { p ->
                p.channelId == channelId || (tvgId != null && p.channelId == tvgId) || matchedXmlChannelIds.contains(p.channelId)
            }

            if (matchingProgs.isNotEmpty()) {
                val nowSec = System.currentTimeMillis() / 1000L
                return@withContext matchingProgs.map { p ->
                    val live = nowSec in p.startTimeSeconds..p.stopTimeSeconds
                    val catchup = if (targetChannel != null && targetChannel.tvArchive == 1 && acc != null && p.stopTimeSeconds < nowSec) {
                        val durationMins = ((p.stopTimeSeconds - p.startTimeSeconds) / 60L).coerceAtLeast(1L)
                        xtreamRepository.buildCatchupStreamUrl(acc.serverUrl, acc.username, acc.password, targetChannel.id, p.startTimeSeconds, durationMins)
                    } else null

                    EpgProgram(
                        id = "${p.channelId}_${p.startTimeSeconds}",
                        channelId = channelId,
                        title = p.title,
                        description = p.description ?: "",
                        startTimestamp = p.startTimeSeconds,
                        endTimestamp = p.stopTimeSeconds,
                        isLive = live,
                        catchupUrl = catchup
                    )
                }.sortedBy { it.startTimestamp }
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getSavedPlaylists(): List<com.tivipro.model.SavedPlaylist> {
        val jsonStr = prefs?.getString("saved_playlists_json", "") ?: ""
        if (jsonStr.isBlank()) {
            val acc = _activeXtreamAccount.value
            if (acc != null && acc.serverUrl.isNotBlank()) {
                val defaultPl = com.tivipro.model.SavedPlaylist(
                    id = "default_1",
                    name = "Lista Principal",
                    mode = DataSourceMode.XTREAM,
                    serverUrl = acc.serverUrl,
                    username = acc.username,
                    password = acc.password,
                    isActive = true
                )
                val list = listOf(defaultPl)
                persistPlaylists(list)
                return list
            }
            return emptyList()
        }
        val res = try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<com.tivipro.model.SavedPlaylist>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val modeStr = obj.optString("mode", "XTREAM")
                val mode = if (modeStr == "M3U") DataSourceMode.M3U else DataSourceMode.XTREAM
                val serverUrl = obj.optString("serverUrl")
                val username = obj.optString("username")
                val password = obj.optString("password")
                val m3uUrl = obj.optString("m3uUrl")
                val isActive = obj.optBoolean("isActive", false)
                list.add(
                    com.tivipro.model.SavedPlaylist(
                        id = id,
                        name = name,
                        mode = mode,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        m3uUrl = m3uUrl,
                        isActive = isActive
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
        _activePlaylist.value = res.firstOrNull { it.isActive } ?: res.firstOrNull()
        return res
    }

    override fun savePlaylist(playlist: com.tivipro.model.SavedPlaylist) {
        val currentList = getSavedPlaylists().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == playlist.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = playlist
        } else {
            currentList.add(playlist)
        }
        persistPlaylists(currentList)
    }

    override fun deletePlaylist(playlistId: String) {
        val currentList = getSavedPlaylists().filter { it.id != playlistId }
        persistPlaylists(currentList)
    }

    override suspend fun switchPlaylist(playlistId: String): Boolean {
        val currentList = getSavedPlaylists()
        val target = currentList.firstOrNull { it.id == playlistId } ?: return false

        val updatedList = currentList.map { pl ->
            pl.copy(isActive = pl.id == playlistId)
        }
        persistPlaylists(updatedList)

        _activePlaylist.value = target.copy(isActive = true)

        if (target.mode == DataSourceMode.XTREAM) {
            saveSession(target.serverUrl, target.username, target.password)
            val authRes = xtreamRepository.authenticate(target.serverUrl, target.username, target.password)
            if (authRes.isSuccess) {
                refreshData()
                return true
            }
        } else if (target.mode == DataSourceMode.M3U) {
            loadM3uFromUrl(target.m3uUrl)
            return true
        }
        return false
    }

    private fun persistPlaylists(list: List<com.tivipro.model.SavedPlaylist>) {
        try {
            val array = org.json.JSONArray()
            for (pl in list) {
                val obj = org.json.JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("mode", pl.mode.name)
                    put("serverUrl", pl.serverUrl)
                    put("username", pl.username)
                    put("password", pl.password)
                    put("m3uUrl", pl.m3uUrl)
                    put("isActive", pl.isActive)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("saved_playlists_json", array.toString())?.apply()
        } catch (_: Exception) {}
    }

    override suspend fun getSportsEvents(): List<SportsEvent> = withContext(Dispatchers.IO) {
        val channels = getLiveChannels()
        sportsRepository.getSportsEvents(channels)
    }

    override fun searchContent(query: String): SearchResult {
        if (query.isBlank()) return SearchResult()
        val q = query.trim().lowercase()

        val matchingChannels = xtreamLiveChannels.filter { it.name.lowercase().contains(q) }
        val matchingMovies = xtreamVodMovies.filter { it.name.lowercase().contains(q) || it.description?.lowercase()?.contains(q) == true }
        val matchingSeries = xtreamSeriesList.filter { it.name.lowercase().contains(q) || it.overview?.lowercase()?.contains(q) == true }

        return SearchResult(
            channels = matchingChannels,
            movies = matchingMovies,
            series = matchingSeries
        )
    }

    override fun toggleFavoriteChannel(channelId: String) {
        if (favoriteChannelIds.contains(channelId)) {
            favoriteChannelIds.remove(channelId)
        } else {
            favoriteChannelIds.add(channelId)
        }
        prefs?.edit()?.putStringSet("favorite_channel_ids", favoriteChannelIds.toSet())?.apply()
    }

    override fun toggleFavoriteMovie(movieId: String) {
        if (favoriteMovieIds.contains(movieId)) {
            favoriteMovieIds.remove(movieId)
        } else {
            favoriteMovieIds.add(movieId)
        }
        prefs?.edit()?.putStringSet("favorite_movie_ids", favoriteMovieIds.toSet())?.apply()
    }

    override fun toggleFavoriteSeries(seriesId: String) {
        if (favoriteSeriesIds.contains(seriesId)) {
            favoriteSeriesIds.remove(seriesId)
        } else {
            favoriteSeriesIds.add(seriesId)
        }
        prefs?.edit()?.putStringSet("favorite_series_ids", favoriteSeriesIds.toSet())?.apply()
    }

    override fun getFavoriteChannels(): List<LiveChannel> {
        return xtreamLiveChannels.filter { favoriteChannelIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }

    override fun getFavoriteMovies(): List<VodMovie> {
        return xtreamVodMovies.filter { favoriteMovieIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }

    override fun getFavoriteSeries(): List<Series> {
        return xtreamSeriesList.filter { favoriteSeriesIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }
}
