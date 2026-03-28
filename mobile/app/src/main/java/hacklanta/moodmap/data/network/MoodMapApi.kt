package hacklanta.moodmap.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val pass: String)

data class RegisterBody(val email: String, val password: String)

data class LoginResponse(val token: String, val message: String)

data class GoogleIdTokenBody(
    @SerializedName("id_token") val idToken: String
)

interface MoodMapApi {
    @POST("api/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun registerUser(@Body body: RegisterBody): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleIdTokenBody): Response<LoginResponse>
}
