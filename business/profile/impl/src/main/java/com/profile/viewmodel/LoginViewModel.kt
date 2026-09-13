package com.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.common.base.BaseViewModel
import com.profile.data.ProfileAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {
    private val profileAuthRepository = ProfileAuthRepository()

    sealed interface AuthUiState {
        data object Idle : AuthUiState
        data object Loading : AuthUiState
        data class Success(
            val authenticated: Boolean
        ) : AuthUiState

        data class Error(
            val message: String
        ) : AuthUiState
    }

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        if (_loginState.value == AuthUiState.Loading) return

        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            try {
                val authenticated = profileAuthRepository.login(email, password)
                _loginState.value = if (authenticated) {
                    AuthUiState.Success(authenticated = true)
                } else {
                    AuthUiState.Error("登录失败，未获取到用户会话")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _loginState.value = AuthUiState.Error(
                    exception.message ?: "登录失败，请稍后重试"
                )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        nickname: String
    ) {
        if (_registerState.value == AuthUiState.Loading) return

        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            try {
                val authenticated = profileAuthRepository.register(
                    email = email,
                    password = password,
                    nickname = nickname
                )
                _registerState.value = AuthUiState.Success(authenticated)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _registerState.value = AuthUiState.Error(
                    exception.message ?: "注册失败，请稍后重试"
                )
            }
        }
    }

    fun consumeLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    fun consumeRegisterState() {
        _registerState.value = AuthUiState.Idle
    }
}
