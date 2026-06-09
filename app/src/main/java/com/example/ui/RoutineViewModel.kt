package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RoutineEntry
import com.example.data.RoutineRepository
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
    BY_CLASS, BY_TEACHER
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

    fun setMode(mode: SearchMode) {
        _searchMode.value = mode
        _selectedQuery.value = null
    }

    fun setQuery(query: String) {
        _selectedQuery.value = query
    }
}
