package `is`.posctrl.posctrl_android.util

import `is`.posctrl.posctrl_android.worker.LogoutWorker
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

const val logoutHour = 23//23
const val logoutMinute = 59//59

fun Context.scheduleLogout(nextDay: Boolean = false) {
    val c = Calendar.getInstance()

    c[Calendar.HOUR_OF_DAY] = logoutHour
    c[Calendar.MINUTE] = logoutMinute
    c[Calendar.SECOND] = 0
    c[Calendar.MILLISECOND] = 0
    var timeDiff = c.timeInMillis - System.currentTimeMillis()
    if (timeDiff < 0 || nextDay) {
        c.add(Calendar.DAY_OF_MONTH, 1)
        timeDiff = c.timeInMillis - System.currentTimeMillis()
    }
    val repeatingRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag(LogoutWorker.TAG_LOGOUT_WORKER).build()

    Timber.d("scheduled logout at ${c.time}")
    WorkManager.getInstance(this)
            .cancelAllWorkByTag(LogoutWorker.TAG_LOGOUT_WORKER)//to avoid having multiple workers with the same tag, helpful in the work info observer
    WorkManager.getInstance(this).enqueue(
            repeatingRequest
    )
}