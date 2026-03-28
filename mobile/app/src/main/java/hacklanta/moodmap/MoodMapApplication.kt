package hacklanta.moodmap

import android.app.Application
import hacklanta.moodmap.data.network.RetrofitClient

class MoodMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.ensureInitialized(this)
    }
}
