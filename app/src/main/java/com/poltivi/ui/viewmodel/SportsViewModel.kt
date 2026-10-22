package com.poltivi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poltivi.data.repository.DataRepository
import com.poltivi.model.SportsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SportsUiState(
    val selectedDateTab: String = "Hoy",
    val selectedSportFilter: String = "Todos",
    val allEvents: List<SportsEvent> = emptyList(),
    val filteredEvents: List<SportsEvent> = emptyList(),
    val isLoading: Boolean = false
)

class SportsViewModel(
    val repository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    init {
        loadSportsEvents()
    }

    fun loadSportsEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val events = repository.getSportsEvents()
                val currentTab = _uiState.value.selectedDateTab
                val availableTab = if (events.isNotEmpty() && events.none { it.dateTab.equals(currentTab, ignoreCase = true) }) {
                    events.first().dateTab
                } else {
                    currentTab
                }
                val state = _uiState.value.copy(
                    allEvents = events,
                    selectedDateTab = availableTab,
                    isLoading = false
                )
                _uiState.value = state.copy(filteredEvents = filterEvents(state))
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectDateTab(dateTab: String) {
        val state = _uiState.value.copy(selectedDateTab = dateTab)
        _uiState.value = state.copy(filteredEvents = filterEvents(state))
    }

    fun selectSportFilter(sportCategory: String) {
        val state = _uiState.value.copy(selectedSportFilter = sportCategory)
        _uiState.value = state.copy(filteredEvents = filterEvents(state))
    }

    private fun filterEvents(state: SportsUiState): List<SportsEvent> {
        return state.allEvents.filter { event ->
            val matchDate = event.dateTab.equals(state.selectedDateTab, ignoreCase = true)
            val matchSport = if (state.selectedSportFilter == "Todos") {
                true
            } else {
                event.sportCategory.equals(state.selectedSportFilter, ignoreCase = true)
            }
            matchDate && matchSport
        }
    }
}
