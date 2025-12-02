package com.example.dresscode.ui.tryon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.OutfitRepository
import com.example.dresscode.data.repository.TaggingRepository
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.OutfitPreview
import com.example.dresscode.model.TaggingUiState
import com.example.dresscode.model.TryOnUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val repository: TryOnRepository,
    private val outfitRepository: OutfitRepository,
    private val taggingRepository: TaggingRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.snapshot())
    val uiState: LiveData<TryOnUiState> = _uiState

    private val _taggingState = MutableLiveData(TaggingUiState())
    val taggingState: LiveData<TaggingUiState> = _taggingState

    val favorites = outfitRepository.favorites()
        .map { favs -> outfitRepository.featured().filter { favs.contains(it.id) } }
        .asLiveData()

    private var selectedPhotoLabel: String? = null
    private var selectedOutfit: OutfitPreview? = null
    private var isLoggedIn = false

    init {
        viewModelScope.launch {
            userRepository.authState().collectLatest { auth ->
                isLoggedIn = auth.isLoggedIn
            }
        }
    }

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
        if (!isLoggedIn) {
            _uiState.value = base.copy(
                error = "请登录后再提交换装",
                isSubmitting = false
            )
            return
        }
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

    fun uploadOutfitForTags(fileName: String, bytes: ByteArray) {
        val base = _taggingState.value ?: TaggingUiState()
        _taggingState.value = base.copy(
            status = "上传中，正在生成标签",
            isUploading = true,
            error = null,
            selectedFileName = fileName
        )
        viewModelScope.launch {
            runCatching { taggingRepository.uploadForTags(fileName, bytes) }
                .onSuccess { result ->
                    _taggingState.postValue(
                        base.copy(
                            status = "标签生成完成",
                            isUploading = false,
                            selectedFileName = result.originalName,
                            suggestedName = result.suggestedName,
                            tagsPreview = formatTags(result.tagsJson),
                            error = null
                        )
                    )
                }
                .onFailure { error ->
                    _taggingState.postValue(
                        base.copy(
                            status = "打标签失败",
                            isUploading = false,
                            error = error.message ?: "上传失败，请稍后重试"
                        )
                    )
                }
        }
    }

    private fun formatTags(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            buildString { appendObject(json, 0) }.trim()
        }.getOrElse { raw }
    }

    private fun StringBuilder.appendObject(obj: JSONObject, indent: Int) {
        val keys = obj.keys().asSequence().toList()
        keys.forEachIndexed { index, key ->
            val value = obj.get(key)
            append(" ".repeat(indent)).append(key).append(": ")
            when (value) {
                is JSONObject -> {
                    append("\n")
                    appendObject(value, indent + 2)
                }
                is JSONArray -> {
                    appendArray(value, indent + 2)
                }
                else -> append(value.toString())
            }
            if (index != keys.lastIndex) append("\n")
        }
    }

    private fun StringBuilder.appendArray(array: JSONArray, indent: Int) {
        if (array.length() == 0) {
            append("[]")
            return
        }
        val simpleItems = (0 until array.length()).all { idx ->
            val item = array.get(idx)
            item !is JSONObject && item !is JSONArray
        }
        if (simpleItems) {
            val joined = (0 until array.length()).joinToString(", ") { array.get(it).toString() }
            append(joined)
        } else {
            append("\n")
            for (i in 0 until array.length()) {
                val item = array.get(i)
                append(" ".repeat(indent)).append("- ")
                when (item) {
                    is JSONObject -> {
                        append("\n")
                        appendObject(item, indent + 2)
                    }
                    is JSONArray -> appendArray(item, indent + 2)
                    else -> append(item.toString())
                }
                if (i != array.length() - 1) append("\n")
            }
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
