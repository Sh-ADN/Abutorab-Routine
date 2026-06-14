package com.abutorab.routine.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abutorab.routine.data.RoutineEntry
import com.abutorab.routine.data.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class RoutineUiState {
    object Loading : RoutineUiState()
    data class Success(val entries: List<RoutineEntry>) : RoutineUiState()
    data class Error(val message: String) : RoutineUiState()
}

enum class SearchMode {
    BY_CLASS, BY_TEACHER, BY_PERIOD
}

class RoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoutineRepository(application)

    private val _uiState = MutableStateFlow<RoutineUiState>(RoutineUiState.Loading)
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.BY_TEACHER)
    val searchMode = _searchMode.asStateFlow()

    private val _selectedQuery = MutableStateFlow<String?>(null)
    val selectedQuery = _selectedQuery.asStateFlow()

    val classes = repository.allClasses
    var teachers = emptyList<String>()
        private set

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = RoutineUiState.Loading
        viewModelScope.launch {
            repository.getRoutine()
                .catch { e ->
                    // Show a clear error message if data fails to load
                    _uiState.value = RoutineUiState.Error("Failed to load schedule. Please try again.")
                }
                .collect { data ->
                    teachers = repository.allTeachers
                    // Stop polling loop once data loads (we only collect once from a cold flow here)
                    _uiState.value = RoutineUiState.Success(data)
                }
        }
    }

    suspend fun pollDataUpdates() {
        val cm = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        while (true) {
            kotlinx.coroutines.delay(5000L)
            val actNw = cm.activeNetworkInfo
            if (actNw != null && actNw.isConnected) {
                val freshData = repository.fetchFreshData()
                if (freshData != null) {
                    teachers = repository.allTeachers
                    val currentState = _uiState.value
                    if (currentState is RoutineUiState.Success) {
                        if (currentState.entries != freshData) {
                            _uiState.value = RoutineUiState.Success(freshData)
                        }
                    } else {
                        // If it was Error or Loading, just update to Success
                        _uiState.value = RoutineUiState.Success(freshData)
                    }
                }
            }
        }
    }

    fun setMode(mode: SearchMode) {
        _searchMode.value = mode
        _selectedQuery.value = null
    }

    fun setQuery(query: String) {
        _selectedQuery.value = query
    }
}
