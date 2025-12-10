package com.example.dresscode.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.SettingsRepository
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.AuthState
import com.example.dresscode.model.Gender
import com.example.dresscode.model.TaggingModel
import com.example.dresscode.model.ProfileUiState
import com.example.dresscode.model.TryOnModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val authState: LiveData<AuthState> = repository.authState().asLiveData()

    val uiState: LiveData<ProfileUiState> = repository.authState()
        .map { repository.profileFor(it) }
        .asLiveData()

    val defaultGender: LiveData<Gender?> =
        settingsRepository.defaultFilters().map { it.gender }.asLiveData()
    val tryOnModel: LiveData<TryOnModel> =
        settingsRepository.modelPreferences().map { it.tryOnModel }.asLiveData()
    val taggingModel: LiveData<TaggingModel> =
        settingsRepository.modelPreferences().map { it.taggingModel }.asLiveData()

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    fun updateDefaultGender(gender: Gender?) {
        viewModelScope.launch { settingsRepository.setDefaultGender(gender) }
    }

    fun updateTryOnModel(model: TryOnModel) {
        viewModelScope.launch { settingsRepository.setTryOnModel(model) }
    }

    fun updateTaggingModel(model: TaggingModel) {
        viewModelScope.launch { settingsRepository.setTaggingModel(model) }
    }
}
