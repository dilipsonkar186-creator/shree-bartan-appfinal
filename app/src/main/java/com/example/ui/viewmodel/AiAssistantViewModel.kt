package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiAssistantResult
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModelProvider

sealed class AiAssistantUiState {
    object Idle : AiAssistantUiState()
    object Processing : AiAssistantUiState()
    data class Success(val result: AiAssistantResult) : AiAssistantUiState()
    data class Error(val message: String) : AiAssistantUiState()
}

class AiAssistantViewModel(
    folderRepository: FolderRepository
) : ViewModel() {

    private val aiRepository = AiAssistantRepository(folderRepository)

    private val _uiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _history = MutableStateFlow<List<AiAssistantResult>>(emptyList())
    val history: StateFlow<List<AiAssistantResult>> = _history.asStateFlow()

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun submitQuery(promptText: String? = null) {
        val queryToSubmit = promptText ?: _inputText.value
        if (queryToSubmit.isBlank()) return

        _inputText.value = ""
        _uiState.value = AiAssistantUiState.Processing

        viewModelScope.launch {
            try {
                val result = aiRepository.processQuery(queryToSubmit)
                _uiState.value = AiAssistantUiState.Success(result)
                _history.value = listOf(result) + _history.value
            } catch (e: Exception) {
                _uiState.value = AiAssistantUiState.Error(e.message ?: "त्रुटि उत्पन्न हुई")
            }
        }
    }

    fun clearState() {
        _uiState.value = AiAssistantUiState.Idle
    }
}

class AiAssistantViewModelFactory(
    private val repository: FolderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiAssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiAssistantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
