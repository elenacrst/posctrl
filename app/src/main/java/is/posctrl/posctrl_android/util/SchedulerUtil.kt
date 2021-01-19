package `is`.posctrl.posctrl_android.util

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.worker.RestartWorker
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit


fun Context.scheduleRestart(nextDay: Boolean = false) {
    val c = Calendar.getInstance()
    val prefs = PreferencesSource(this)

    var hour = prefs.defaultPrefs()[getString(R.string.key_restart_hour), 0] ?: 0
    if (hour == 0) {
        return
    } else if (hour > 23 || hour < 0) {
        hour = 0
    }
    c[Calendar.HOUR_OF_DAY] = hour
    c[Calendar.MINUTE] = 0
    c[Calendar.SECOND] = 0
    c[Calendar.MILLISECOND] = 0
    var timeDiff = c.timeInMillis - System.currentTimeMillis()
    if (timeDiff < 0 || nextDay) {
        c.add(Calendar.DAY_OF_MONTH, 1)
        timeDiff = c.timeInMillis - System.currentTimeMillis()
    }
    val repeatingRequest = OneTimeWorkRequestBuilder<RestartWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag(RestartWorker.TAG_RESTART_WORKER).build()

    Timber.d("scheduled restart at ${c.time}")
    WorkManager.getInstance(this)
            .cancelAllWorkByTag(RestartWorker.TAG_RESTART_WORKER)//to avoid having multiple workers with the same tag, helpful in the work info observer
    WorkManager.getInstance(this).enqueue(
            repeatingRequest
    )
}