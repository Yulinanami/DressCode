package com.example.dresscode.ui.weather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.WeatherRepository
import com.example.dresscode.model.WeatherRecommendation
import com.example.dresscode.model.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<WeatherUiState> = _uiState
    private val requestId = AtomicInteger(0)
    private var hasAutoRefreshed = false

    fun shouldAutoRefresh(): Boolean {
        if (hasAutoRefreshed) return false
        hasAutoRefreshed = true
        return true
    }

    fun refresh(city: String? = null, lat: Double? = null, lon: Double? = null) {
        val current = _uiState.value ?: repository.snapshot()
        val id = requestId.incrementAndGet()
        _uiState.value = current.copy(isLoading = true, error = null, recommendation = null, isRecommending = false)
        viewModelScope.launch {
            runCatching { repository.fetch(city ?: current.city, lat, lon) }
                .onSuccess { fetched ->
                    if (id == requestId.get()) {
                        val updated = fetched.copy(isLoading = false, error = null, isRecommending = false, recommendation = null)
                        _uiState.postValue(updated)
                    }
                }
                .onFailure { error ->
                    if (id == requestId.get()) {
                        _uiState.postValue(current.copy(isLoading = false, error = error.message ?: "天气数据获取失败"))
                    }
                }
        }
    }

    fun requestRecommendation() {
        val weather = _uiState.value ?: return
        if (weather.isRecommending) return
        _uiState.postValue(weather.copy(isRecommending = true))
        val tempValue = parseTemperature(weather.temperature)
        val summary = weather.summary.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            outfitRepository.recommendOutfit(
                city = weather.city,
                temperature = tempValue,
                weatherText = summary
            ).onSuccess { rec ->
                val latest = _uiState.value ?: weather
                _uiState.postValue(latest.copy(isRecommending = false, recommendation = rec))
            }.onFailure {
                val latest = _uiState.value ?: weather
                _uiState.postValue(latest.copy(isRecommending = false))
            }
        }
    }

    private fun parseTemperature(temp: String?): Double? {
        if (temp.isNullOrBlank()) return null
        val cleaned = temp.replace("°", "").trim()
        return cleaned.toDoubleOrNull()
    }
}
