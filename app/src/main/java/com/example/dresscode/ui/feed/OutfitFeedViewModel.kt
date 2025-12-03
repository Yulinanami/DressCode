package com.example.dresscode.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.SettingsRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.Gender
import com.example.dresscode.model.OutfitFilters
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.OutfitUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OutfitFeedViewModel @Inject constructor(
    private val repository: OutfitRepository,
    private val userRepository: UserRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val manualFilters = MutableStateFlow(OutfitFilters())
    private val queryFlow = MutableStateFlow("")
    private val defaultFilters = settingsRepository.defaultFilters()
        .stateIn(viewModelScope, SharingStarted.Lazily, OutfitFilters())

    private val activeFilters = combine(defaultFilters, manualFilters) { defaults, manual ->
        manual.withDefaults(defaults)
    }.stateIn(viewModelScope, SharingStarted.Lazily, OutfitFilters())

    private val recentSearches = repository.recentSearches().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private val _uiState = MutableLiveData(OutfitUiState())
    val uiState: LiveData<OutfitUiState> = _uiState

    val favorites: LiveData<List<OutfitPreview>> = repository.observeFavorites().asLiveData()

    val pagingData: Flow<PagingData<OutfitPreview>> = queryFlow
        .debounce(250)
        .combine(activeFilters) { query, filters -> query to filters }
        .flatMapLatest { (query, filters) ->
            repository.pagedOutfits(query, filters)
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            combine(queryFlow, activeFilters, recentSearches) { query, filters, history ->
                val highlightText = when {
                    query.isNotBlank() -> "搜索：$query"
                    history.isNotEmpty() -> "最近搜：${history.take(2).joinToString(" / ")}"
                    else -> "精选穿搭"
                }
                OutfitUiState(
                    query = query,
                    selectedGender = filters.gender,
                    selectedStyle = filters.style,
                    selectedSeason = filters.season,
                    selectedScene = filters.scene,
                    selectedWeather = filters.weather,
                    selectedTags = filters.tags,
                    highlight = highlightText,
                    filters = buildFilterLabel(filters),
                    recentQueries = history,
                    isLoading = false,
                    error = null
                )
            }.collect { state -> _uiState.postValue(state) }
        }
        viewModelScope.launch { repository.refreshFavoritesFromRemote() }
        viewModelScope.launch {
            userRepository.authState().collect { auth ->
                isLoggedIn = auth.isLoggedIn
            }
        }
    }

    fun onSearchQueryChanged(text: String) {
        queryFlow.value = text
    }

    fun onSearchSubmitted(text: String) {
        viewModelScope.launch { repository.recordSearch(text) }
    }

    fun setGenderFilter(label: String?) {
        val gender = when (label) {
            "男" -> Gender.MALE
            "女" -> Gender.FEMALE
            else -> null
        }
        manualFilters.value = manualFilters.value.copy(gender = gender)
    }

    fun setStyleFilter(value: String?) {
        manualFilters.value = manualFilters.value.copy(style = value)
    }

    fun setSeasonFilter(value: String?) {
        manualFilters.value = manualFilters.value.copy(season = value)
    }

    fun setSceneFilter(value: String?) {
        manualFilters.value = manualFilters.value.copy(scene = value)
    }

    fun setWeatherFilter(value: String?) {
        manualFilters.value = manualFilters.value.copy(weather = value)
    }

    fun clearFilters() {
        manualFilters.value = OutfitFilters()
        queryFlow.value = ""
    }

    private var isLoggedIn: Boolean = false

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            if (!isLoggedIn) {
                val base = _uiState.value ?: OutfitUiState()
                _uiState.postValue(base.copy(error = "请登录后再收藏"))
                return@launch
            }
            repository.toggleFavorite(id)
                .onFailure { error ->
                    val base = _uiState.value ?: OutfitUiState()
                    _uiState.postValue(base.copy(error = error.message ?: "收藏失败，请稍后重试"))
                }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch { repository.refreshFavoritesFromRemote() }
    }

    private fun buildFilterLabel(filters: OutfitFilters): String {
        val parts = mutableListOf<String>()
        filters.gender?.let { parts.add("性别:${genderLabel(it)}") }
        filters.style?.let { parts.add("风格:$it") }
        filters.season?.let { parts.add("季节:$it") }
        filters.scene?.let { parts.add("场景:$it") }
        filters.weather?.let { parts.add("天气:$it") }
        if (filters.tags.isNotEmpty()) {
            parts.add("标签:${filters.tags.joinToString("/")}")
        }
        return if (parts.isEmpty()) "默认筛选：全部" else parts.joinToString(" · ")
    }

    private fun genderLabel(gender: Gender): String = when (gender) {
        Gender.FEMALE -> "女"
        Gender.MALE -> "男"
        Gender.UNISEX -> "通用"
    }
}
