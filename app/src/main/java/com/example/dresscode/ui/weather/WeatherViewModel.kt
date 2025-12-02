package com.example.dresscode.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.WeatherRepository
import com.example.dresscode.model.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<WeatherUiState> = _uiState
    private val requestId = AtomicInteger(0)

    fun refresh(city: String? = null, lat: Double? = null, lon: Double? = null) {
        val current = _uiState.value ?: repository.snapshot()
        val id = requestId.incrementAndGet()
        _uiState.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.fetch(city ?: current.city, lat, lon) }
                .onSuccess { fetched ->
                    if (id == requestId.get()) {
                        _uiState.postValue(fetched.copy(isLoading = false, error = null))
                    }
                }
                .onFailure { error ->
                    if (id == requestId.get()) {
                        _uiState.postValue(current.copy(isLoading = false, error = error.message ?: "天气数据获取失败"))
                    }
                }
        }
    }
}
