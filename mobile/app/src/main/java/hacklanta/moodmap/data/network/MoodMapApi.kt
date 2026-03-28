package hacklanta.moodmap.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// These data classes represent the JSON sent to and received from your server
data class LoginRequest(val email: String, val pass: String)
data class LoginResponse(val token: String, val message: String)

interface MoodMapApi {
    // This tells Android to make a POST request to your backend's login route
    @POST("api/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>
}