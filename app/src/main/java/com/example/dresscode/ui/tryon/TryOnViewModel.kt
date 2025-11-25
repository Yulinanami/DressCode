package com.example.dresscode.ui.tryon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.model.TryOnUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val repository: TryOnRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<TryOnUiState> = _uiState

    fun refresh() {
        _uiState.value = repository.snapshot()
    }
}
