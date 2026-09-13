package com.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.common.base.BaseViewModel
import com.profile.data.ProfileAuthRepository
import com.profile.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : BaseViewModel() {
    private val repository = ProfileAuthRepository()

    private val _profile = MutableStateFlow(
        repository.currentProfile()
    )
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _profileSaved = MutableSharedFlow<Unit>()
    val profileSaved: SharedFlow<Unit> = _profileSaved

    fun refreshProfile() {
        _profile.value = repository.currentProfile()
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            _profile.value = repository.updateNickname(nickname)
        }
    }

    fun updateProfile(nickname: String, email: String, avatarUri: String) {
        viewModelScope.launch {
            _profile.value = repository.updateProfile(nickname, email, avatarUri)
            _profileSaved.emit(Unit)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _loggedOut.value = true
        }
    }
}
