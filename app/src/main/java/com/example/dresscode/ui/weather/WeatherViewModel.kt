package com.example.dresscode.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dresscode.data.repository.WeatherRepository
import com.example.dresscode.model.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<WeatherUiState> = _uiState

    fun refresh() {
        _uiState.value = repository.snapshot()
    }
}
