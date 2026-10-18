package com.zts.imagecompare.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zts.imagecompare.Graph
import com.zts.imagecompare.data.ComparisonRepository
import com.zts.imagecompare.data.ComparisonWithImages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ComparisonRepository) : ViewModel() {
    val items: StateFlow<List<ComparisonWithImages>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(Graph.repository) as T
        }
    }
}
