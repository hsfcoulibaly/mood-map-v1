package hacklanta.moodmap.data.repository

import hacklanta.moodmap.data.network.GoogleIdTokenBody
import hacklanta.moodmap.data.network.LoginRequest
import hacklanta.moodmap.data.network.MoodMapApi
import hacklanta.moodmap.data.network.RegisterBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class AuthRepository(private val api: MoodMapApi) {

    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            val response = api.loginUser(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.token)
            } else {
                Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(Exception(networkMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Result<String> {
        return try {
            val response = api.loginWithGoogle(GoogleIdTokenBody(idToken))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.token)
            } else {
                Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(Exception(networkMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<String> {
        return try {
            val response = api.registerUser(RegisterBody(email.trim().lowercase(), password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.token)
            } else {
                Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(Exception(networkMessage(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun networkMessage(e: IOException): String = when (e) {
        is UnknownHostException ->
            "Cannot find server. Check MOOD_MAP_API_BASE_URL in local.properties and that your phone and PC are on the same Wi‑Fi."
        is SocketTimeoutException ->
            "Connection timed out. Start the backend, open firewall port 8000, and use your PC's LAN IP on a real device (not 10.0.2.2)."
        is ConnectException ->
            "Cannot connect. On a physical device set MOOD_MAP_API_BASE_URL=http://YOUR_PC_IP:8000/ and run the API with host 0.0.0.0 (see backend main.py)."
        is SSLException ->
            "SSL error. Release builds must use https:// for the API URL."
        else ->
            e.message?.takeIf { it.isNotBlank() }
                ?: "Network error (${e.javaClass.simpleName})"
    }

    private fun parseErrorBody(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return "Request failed"
        return try {
            val json = JSONObject(text)
            when (val detail = json.opt("detail")) {
                is String -> detail
                is JSONArray -> parseDetailArray(detail)
                else -> text
            }
        } catch (_: Exception) {
            text
        }
    }

    /** FastAPI 422 uses `[{ "msg": "...", "loc": [...] }, ...]`; 409/401 use a string `detail`. */
    private fun parseDetailArray(detail: JSONArray): String {
        if (detail.length() == 0) return "Request failed"
        val parts = ArrayList<String>()
        for (i in 0 until detail.length()) {
            val obj = detail.optJSONObject(i)
            if (obj != null) {
                val msg = obj.optString("msg", "")
                if (msg.isNotEmpty()) parts.add(msg)
            } else {
                val s = detail.optString(i, "")
                if (s.isNotEmpty()) parts.add(s)
            }
        }
        return parts.joinToString(" ").ifEmpty { detail.toString() }
    }
}
