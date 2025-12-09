package com.example.dresscode.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.OutfitDetail
import com.example.dresscode.model.OutfitPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: OutfitRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val trigger = MutableStateFlow(0)

    val favorites: Flow<PagingData<OutfitPreview>> = trigger.flatMapLatest {
        repository.observeFavorites().onEach { list ->
            _uiState.postValue(
                FavoritesUiState(
                    isLoading = false,
                    isEmpty = list.isEmpty()
                )
            )
        }.map { list ->
            PagingData.from(list)
        }
    }.cachedIn(viewModelScope)

    private val _uiState = MutableLiveData(FavoritesUiState())
    val uiState: LiveData<FavoritesUiState> = _uiState

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?> = _toast

    private val _detail = MutableLiveData<OutfitDetail?>()
    val detail: LiveData<OutfitDetail?> = _detail

    fun refreshFavorites() {
        viewModelScope.launch {
            val auth = userRepository.authState().first()
            if (!auth.isLoggedIn) {
                _toast.postValue("请登录后查看收藏")
                _uiState.postValue(FavoritesUiState(isLoading = false, isEmpty = true))
                return@launch
            }
            _uiState.postValue(FavoritesUiState(isLoading = true))
            val result = repository.refreshFavoritesFromRemote()
            result.onFailure { error ->
                _toast.postValue(error.message ?: "刷新收藏失败")
            }
            _uiState.postValue(
                FavoritesUiState(
                    isLoading = false,
                    isEmpty = false
                )
            )
            trigger.value += 1
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
                .onFailure { error -> _toast.postValue(error.message ?: "收藏操作失败") }
        }
    }

    fun loadOutfitDetail(id: String) {
        viewModelScope.launch {
            repository.fetchOutfitDetail(id)
                .onSuccess { _detail.postValue(it) }
                .onFailure { _toast.postValue(it.message ?: "加载详情失败") }
        }
    }

    fun clearDetail() {
        _detail.value = null
    }
}
