package hacklanta.moodmap.data.network

import android.content.Context
import hacklanta.moodmap.BuildConfig
import hacklanta.moodmap.data.local.SessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var _api: MoodMapApi? = null

    fun ensureInitialized(context: Context) {
        if (_api != null) return
        synchronized(this) {
            if (_api != null) return
            SessionStore.init(context.applicationContext)
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val token = SessionStore.getAccessToken()
                    val request = chain.request()
                    val next = if (!token.isNullOrBlank()) {
                        request.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    } else {
                        request
                    }
                    chain.proceed(next)
                }
            if (BuildConfig.IS_DEBUG_BUILD) {
                val logging = HttpLoggingInterceptor().apply {
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                builder.addInterceptor(logging)
            }
            val baseUrl = BuildConfig.MOOD_MAP_API_BASE_URL.trim().trimEnd('/') + "/"
            _api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MoodMapApi::class.java)
        }
    }

    val api: MoodMapApi
        get() = requireNotNull(_api) { "RetrofitClient not initialized; MoodMapApplication must run first." }
}
