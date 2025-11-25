package com.example.dresscode.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dresscode.data.repository.UserRepository
import com.example.dresscode.model.AuthState
import com.example.dresscode.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val authState: LiveData<AuthState> = repository.authState().asLiveData()

    val uiState: LiveData<ProfileUiState> = repository.authState()
        .map { repository.profileFor(it) }
        .asLiveData()

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
