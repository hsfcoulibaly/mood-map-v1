package hacklanta.moodmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hacklanta.moodmap.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository? = null) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun signInWithEmail(email: String, pass: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            // Null check added here so the UI doesn't crash before we connect the backend!
            if (repository != null) {
                val result = repository.login(email, pass)
                if (result.isSuccess) {
                    _loginState.value = LoginState.Success(result.getOrNull() ?: "")
                } else {
                    _loginState.value = LoginState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            }
        }
    }

    // This is the function the UI was looking for!
    fun signInWithGoogle() {
        // TODO: We will trigger the Google Sign-In intent here later
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}