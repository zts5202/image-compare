package com.zts.imagecompare.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zts.imagecompare.Graph
import com.zts.imagecompare.data.ComparisonRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: ComparisonRepository) : ViewModel() {
    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            onDone()
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(Graph.repository) as T
        }
    }
}
