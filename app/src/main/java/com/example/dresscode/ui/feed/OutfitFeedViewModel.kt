package com.example.dresscode.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.OutfitUiState

class OutfitFeedViewModel(
    private val repository: OutfitRepository = OutfitRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<OutfitUiState> = _uiState

    private val _featured = MutableLiveData(repository.featured())
    val featured: LiveData<List<OutfitPreview>> = _featured

    fun refresh() {
        _uiState.value = repository.snapshot()
        _featured.value = repository.featured()
    }
}
