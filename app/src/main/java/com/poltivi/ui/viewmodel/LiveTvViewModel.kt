package com.poltivi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poltivi.data.repository.DataRepository
import com.poltivi.model.Category
import com.poltivi.model.EpgProgram
import com.poltivi.model.LiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isFavoritesSelected: Boolean = false,
    val isCatchupSelected: Boolean = false,
    val channels: List<LiveChannel> = emptyList(),
    val selectedChannel: LiveChannel? = null,
    val epgPrograms: List<EpgProgram> = emptyList(),
    val isLoading: Boolean = false
)

class LiveTvViewModel(
    val repository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveTvUiState())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeEpgSourceMode()
    }

    private fun observeEpgSourceMode() {
        viewModelScope.launch {
            repository.epgSourceMode.collect {
                refreshEpgData()
            }
        }
    }

    fun refreshEpgData() {
        viewModelScope.launch {
            val activeChannel = _uiState.value.selectedChannel
            if (activeChannel != null) {
                try {
                    val epg = repository.getEpgForChannel(activeChannel.id, activeChannel.epgId)
                    _uiState.value = _uiState.value.copy(epgPrograms = epg)
                } catch (_: Exception) {}
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val cats = repository.getLiveCategories()
                val channels = if (_uiState.value.isCatchupSelected) {
                    repository.getCatchupChannels()
                } else if (_uiState.value.isFavoritesSelected) {
                    repository.getFavoriteChannels()
                } else {
                    repository.getLiveChannels(_uiState.value.selectedCategory?.id)
                }
                val currentSelected = _uiState.value.selectedChannel
                val activeChannel = if (currentSelected != null && channels.any { it.id == currentSelected.id }) {
                    currentSelected
                } else {
                    channels.firstOrNull()
                }
                val epg = if (activeChannel != null) {
                    repository.getEpgForChannel(activeChannel.id, activeChannel.epgId)
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    categories = cats,
                    channels = channels,
                    selectedChannel = activeChannel,
                    epgPrograms = epg,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectCategory(category: Category?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                isFavoritesSelected = false,
                isCatchupSelected = false,
                isLoading = true
            )
            try {
                val channels = repository.getLiveChannels(category?.id)
                val firstChannel = channels.firstOrNull()
                val epg = if (firstChannel != null) {
                    repository.getEpgForChannel(firstChannel.id, firstChannel.epgId)
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    selectedChannel = firstChannel,
                    epgPrograms = epg,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCategory = null,
                isFavoritesSelected = true,
                isCatchupSelected = false,
                isLoading = true
            )
            try {
                val favs = repository.getFavoriteChannels()
                val firstChannel = favs.firstOrNull()
                val epg = if (firstChannel != null) {
                    repository.getEpgForChannel(firstChannel.id, firstChannel.epgId)
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    channels = favs,
                    selectedChannel = firstChannel,
                    epgPrograms = epg,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectCatchup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCategory = null,
                isFavoritesSelected = false,
                isCatchupSelected = true,
                isLoading = true
            )
            try {
                val catchupChs = repository.getCatchupChannels()
                val firstChannel = catchupChs.firstOrNull()
                val epg = if (firstChannel != null) {
                    repository.getEpgForChannel(firstChannel.id, firstChannel.epgId)
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    channels = catchupChs,
                    selectedChannel = firstChannel,
                    epgPrograms = epg,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectChannel(channel: LiveChannel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedChannel = channel)
            try {
                val epg = repository.getEpgForChannel(channel.id, channel.epgId)
                _uiState.value = _uiState.value.copy(epgPrograms = epg)
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite(channelId: String) {
        repository.toggleFavoriteChannel(channelId)
        viewModelScope.launch {
            try {
                val updatedChannels = if (_uiState.value.isCatchupSelected) {
                    repository.getCatchupChannels()
                } else if (_uiState.value.isFavoritesSelected) {
                    repository.getFavoriteChannels()
                } else {
                    repository.getLiveChannels(_uiState.value.selectedCategory?.id)
                }
                _uiState.value = _uiState.value.copy(channels = updatedChannels)
            } catch (_: Exception) {}
        }
    }
}
