package com.example.dresscode.ui.tryon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.TryOnUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val repository: TryOnRepository,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<TryOnUiState> = _uiState

    val favorites = outfitRepository.favorites()
        .map { favs -> outfitRepository.featured().filter { favs.contains(it.id) } }
        .asLiveData()

    private var selectedPhotoLabel: String? = null
    private var selectedOutfit: OutfitPreview? = null

    fun refresh() {
        _uiState.value = repository.snapshot()
    }

    fun attachPhoto(label: String) {
        selectedPhotoLabel = label
        updateState()
    }

    fun useFavorite(outfit: OutfitPreview?) {
        selectedOutfit = outfit
        updateState()
    }

    fun submitTryOn() {
        val base = _uiState.value ?: repository.snapshot()
        if (selectedPhotoLabel.isNullOrBlank() || selectedOutfit == null) {
            _uiState.value = base.copy(
                error = "请先选择人像和收藏的穿搭",
                isSubmitting = false
            )
            return
        }
        _uiState.value = base.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            val result = repository.submitTryOn(selectedPhotoLabel, selectedOutfit)
            _uiState.postValue(
                result.copy(
                    isSubmitting = false,
                    selectedPhotoLabel = selectedPhotoLabel,
                    selectedOutfitTitle = selectedOutfit?.title
                )
            )
        }
    }

    private fun updateState() {
        val base = _uiState.value ?: repository.snapshot()
        val status = when {
            selectedPhotoLabel != null && selectedOutfit != null -> "准备提交换装"
            selectedPhotoLabel != null -> "已选择人像"
            selectedOutfit != null -> "已选择穿搭"
            else -> base.status
        }
        val hint = when {
            selectedPhotoLabel != null && selectedOutfit != null -> "点击提交，等待后端生成换装结果"
            selectedPhotoLabel != null -> "请选择收藏的穿搭用于换装"
            selectedOutfit != null -> "请上传或拍摄人像"
            else -> base.hint
        }
        _uiState.value = base.copy(
            status = status,
            hint = hint,
            selectedPhotoLabel = selectedPhotoLabel,
            selectedOutfitTitle = selectedOutfit?.title,
            error = null,
            isSubmitting = false
        )
    }
}
