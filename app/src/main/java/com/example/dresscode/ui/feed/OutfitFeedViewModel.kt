package com.example.dresscode.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.model.Gender
import com.example.dresscode.model.OutfitFilters
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.OutfitUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class OutfitFeedViewModel @Inject constructor(
    private val repository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<OutfitUiState> = _uiState

    private val _featured = MutableLiveData<List<OutfitPreview>>(emptyList())
    val featured: LiveData<List<OutfitPreview>> = _featured

    private var filters = OutfitFilters()
    private var favorites: Set<String> = emptySet()
    private var query: String = ""

    init {
        viewModelScope.launch {
            repository.favorites().collectLatest { favs ->
                favorites = favs
                rebuild()
            }
        }
        refresh()
    }

    fun refresh() {
        val current = _uiState.value ?: repository.snapshot()
        _uiState.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch { rebuild() }
    }

    fun onSearchQueryChanged(text: String) {
        query = text
        refresh()
    }

    fun toggleGenderFilter(enabled: Boolean) {
        filters = filters.copy(gender = if (enabled) Gender.FEMALE else null)
        refresh()
    }

    fun toggleTag(tag: String, enabled: Boolean) {
        filters = filters.copy(
            tags = if (enabled) filters.tags + tag else filters.tags - tag
        )
        refresh()
    }

    fun toggleFavorite(id: String) {
        repository.toggleFavorite(id)
    }

    private suspend fun rebuild() {
        val items = repository.search(query, filters)
            .map { preview -> preview.copy(isFavorite = favorites.contains(preview.id)) }
        _featured.postValue(items)
        val filterLabel = buildFilterLabel()
        val updatedState = (_uiState.value ?: repository.snapshot()).copy(
            query = query,
            selectedGender = filters.gender,
            selectedTags = filters.tags,
            highlight = "精选穿搭 ${items.size} 套",
            filters = filterLabel,
            isLoading = false,
            error = null
        )
        _uiState.postValue(updatedState)
    }

    private fun buildFilterLabel(): String {
        val parts = mutableListOf<String>()
        filters.gender?.let { parts.add("性别:${genderLabel(it)}") }
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
