package com.example.dresscode.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.profile())
    val uiState: LiveData<ProfileUiState> = _uiState

    fun refresh() {
        _uiState.value = repository.profile()
    }
}
