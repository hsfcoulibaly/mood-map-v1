package hacklanta.moodmap.data.repository

import hacklanta.moodmap.data.network.LoginRequest
import hacklanta.moodmap.data.network.MoodMapApi

class AuthRepository(private val api: MoodMapApi) {

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val response = api.loginUser(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                // Return the success token
                Result.success(response.body()!!.token)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}