package `is`.posctrl.posctrl_android.worker

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.ui.MainActivity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters


class RestartWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        //do stuff in bg thread
        val mStartActivity = Intent(ctx, MainActivity::class.java)
        val mPendingIntentId = 123456
        val prefs = PreferencesSource(ctx)
        prefs.defaultPrefs()[ctx.getString(R.string.key_restarted)] = true
        val mPendingIntent = PendingIntent.getActivity(ctx, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT)
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)[AlarmManager.RTC, System.currentTimeMillis() + 1000] = mPendingIntent
        Process.killProcess(Process.myPid())
        val data = Data.Builder().build()
        return Result.success(data)
    }

    companion object {
        const val TAG_RESTART_WORKER = "PosCtrlRestartWorker"
    }
}