package com.example.dresscode.ui.tryon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.SettingsRepository
import com.example.dresscode.data.repository.TryOnImage
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.TryOnUiState
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.model.TryOnModel
import com.example.dresscode.ui.tryon.TryOnFavoriteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val repository: TryOnRepository,
    userRepository: UserRepository,
    outfitRepository: OutfitRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<TryOnUiState> = _uiState
    private val _favorites = MutableLiveData<List<TryOnFavoriteItem>>(emptyList())
    val favorites: LiveData<List<TryOnFavoriteItem>> = _favorites

    private var selectedPhotoLabel: String? = null
    private var selectedPortraitImage: TryOnImage? = null
    private var selectedOutfitImage: TryOnImage? = null
    private var isLoggedIn = false
    private var authToken: String? = null
    private var selectedTryOnModel: TryOnModel = TryOnModel.AITRYON_PLUS

    init {
        viewModelScope.launch {
            userRepository.authState().collectLatest { auth ->
                isLoggedIn = auth.isLoggedIn
                authToken = auth.token
            }
        }
        viewModelScope.launch {
            settingsRepository.modelPreferences().collectLatest { prefs ->
                selectedTryOnModel = prefs.tryOnModel
            }
        }
        viewModelScope.launch {
            outfitRepository.observeFavorites().collectLatest { list ->
                val enriched = list.flatMap { preview ->
                    val images = runCatching {
                        outfitRepository.fetchOutfitDetail(preview.id).getOrNull()?.images
                    }.getOrNull()
                    val finalImages = images
                        ?.filter { it.isNotBlank() }
                        ?.ifEmpty { null }
                        ?: preview.imageUrl?.let { listOf(it) }
                        ?: emptyList()
                    finalImages.map { url ->
                        TryOnFavoriteItem(
                            outfitId = preview.id,
                            title = preview.title,
                            tags = preview.tags,
                            imageUrl = url
                        )
                    }
                }
                _favorites.postValue(enriched)
            }
        }
    }

    fun consumePendingRecommendedOutfit() {
        repository.consumePendingRecommendedOutfit()?.let { (title, imageUrl) ->
            useRecommendedOutfit(title, imageUrl)
        }
    }

    fun refresh() {
        _uiState.value = repository.snapshot()
    }

    fun attachPhoto(label: String, bytes: ByteArray, mimeType: String? = null) {
        selectedPhotoLabel = label
        selectedPortraitImage = TryOnImage(
            fileName = label,
            bytes = bytes,
            mimeType = mimeType ?: "image/*"
        )
        _uiState.value = (_uiState.value ?: repository.snapshot()).copy(
            resultImageBase64 = null,
            resultPreview = null
        )
        updateState()
    }

    fun selectOutfitImage(label: String, bytes: ByteArray, mimeType: String? = null) {
        selectedOutfitImage = TryOnImage(
            fileName = label,
            bytes = bytes,
            mimeType = mimeType ?: "image/*"
        )
        _uiState.value = (_uiState.value ?: repository.snapshot()).copy(
            resultImageBase64 = null,
            resultPreview = null
        )
        updateState()
    }

    fun submitTryOn() {
        val base = _uiState.value ?: repository.snapshot()
        if (!isLoggedIn) {
            _uiState.value = base.copy(
                error = "请登录后再提交换装",
                isSubmitting = false
            )
            return
        }
        val portrait = selectedPortraitImage
        val outfitImage = selectedOutfitImage
        if (portrait == null || outfitImage == null) {
            _uiState.value = base.copy(
                error = "请先选择人像和穿搭",
                isSubmitting = false
            )
            return
        }
        _uiState.value = base.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            runCatching { repository.submitTryOn(portrait, outfitImage, authToken, selectedTryOnModel.value) }
                .onSuccess { result ->
                    _uiState.postValue(
                        result.copy(
                            isSubmitting = false,
                            selectedPhotoLabel = selectedPhotoLabel,
                            selectedOutfitTitle = outfitImage.fileName,
                            selectedPhotoBytes = portrait.bytes,
                            selectedOutfitBytes = outfitImage.bytes
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.postValue(
                        base.copy(
                            isSubmitting = false,
                            error = error.message ?: "换装失败，请稍后重试"
                        )
                    )
                }
        }
    }

    private fun updateState() {
        val base = _uiState.value ?: repository.snapshot()
        val status = when {
            selectedPortraitImage != null && selectedOutfitImage != null -> "准备提交换装"
            selectedPortraitImage != null -> "已选择人像"
            selectedOutfitImage != null -> "已选择穿搭"
            else -> base.status
        }
        val hint = when {
            selectedPortraitImage != null && selectedOutfitImage != null -> "点击提交，等待后端生成换装结果"
            selectedPortraitImage != null -> "请上传穿搭用于换装"
            selectedOutfitImage != null -> "请上传或拍摄人像"
            else -> base.hint
        }
        _uiState.value = base.copy(
            status = status,
            hint = hint,
            selectedPhotoLabel = selectedPhotoLabel,
            selectedOutfitTitle = selectedOutfitImage?.fileName,
            error = null,
            isSubmitting = false,
            selectedPhotoBytes = selectedPortraitImage?.bytes ?: base.selectedPhotoBytes,
            selectedOutfitBytes = selectedOutfitImage?.bytes ?: base.selectedOutfitBytes
        )
    }

    fun useFavoriteOutfit(outfit: TryOnFavoriteItem) {
        viewModelScope.launch {
            val base = _uiState.value ?: repository.snapshot()
            val url = outfit.imageUrl
            if (url.isNullOrBlank()) {
                _uiState.postValue(base.copy(error = "该收藏缺少图片，无法用于换装"))
                return@launch
            }
            applyOutfitFromUrl(
                title = outfit.title,
                imageUrl = url,
                hint = "已选择收藏穿搭，上传人像后提交"
            )
        }
    }

    fun useRecommendedOutfit(title: String, imageUrl: String) {
        viewModelScope.launch {
            applyOutfitFromUrl(
                title = title,
                imageUrl = imageUrl,
                hint = "已选择推荐穿搭，上传人像后提交"
            )
        }
    }

    private suspend fun applyOutfitFromUrl(title: String, imageUrl: String, hint: String) {
        val base = _uiState.value ?: repository.snapshot()
        val bytes = repository.downloadImage(imageUrl)
        if (bytes == null) {
            _uiState.postValue(base.copy(error = "下载穿搭图片失败，请稍后重试"))
            return
        }
        selectedOutfitImage = TryOnImage(
            fileName = title,
            bytes = bytes,
            mimeType = "image/jpeg"
        )
        _uiState.postValue(
            base.copy(
                selectedOutfitTitle = title,
                selectedOutfitBytes = bytes,
                resultImageBase64 = null,
                resultPreview = null,
                error = null,
                hint = hint
            )
        )
    }
}
