package hacklanta.moodmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hacklanta.moodmap.data.local.SessionStore
import hacklanta.moodmap.data.network.RetrofitClient
import hacklanta.moodmap.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository(RetrofitClient.api)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    fun onGoogleSignInPresentationFailure(message: String) {
        _loginState.value = LoginState.Error(message)
    }

    fun signInWithGoogle(idToken: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.loginWithGoogleIdToken(idToken)
            if (result.isSuccess) {
                val token = result.getOrNull() ?: ""
                SessionStore.saveAccessToken(token)
                _loginState.value = LoginState.Success(token)
            } else {
                _loginState.value =
                    LoginState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.login(email, pass)

            if (result.isSuccess) {
                val token = result.getOrNull() ?: ""
                SessionStore.saveAccessToken(token)
                _loginState.value = LoginState.Success(token)
            } else {
                _loginState.value =
                    LoginState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun register(email: String, password: String) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            val result = repository.register(email, password)
            if (result.isSuccess) {
                val token = result.getOrNull() ?: ""
                SessionStore.saveAccessToken(token)
                _registerState.value = RegisterState.Success(token)
            } else {
                _registerState.value =
                    RegisterState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val token: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
