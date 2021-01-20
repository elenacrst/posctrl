package `is`.posctrl.posctrl_android.worker

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
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
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.*


class RestartWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val mStartActivity = Intent(ctx, MainActivity::class.java)
        val mPendingIntentId = 123456
        val prefs = PreferencesSource(ctx)
        val restartAlreadySet = prefs.defaultPrefs()[ctx.getString(R.string.key_restarted), false]
                ?: false
        if (!restartAlreadySet) {
            val req = GlobalScope.launch {
                withContext(Dispatchers.Default) {
                    delay(60 * 1000)
                    prefs.defaultPrefs()[ctx.getString(R.string.key_restarted)] = true
                }
            }
            GlobalScope.launch {
                req.join()
                withContext(Dispatchers.Default) {
                    val repository = PosCtrlRepository(prefs, ctx, XmlMapper(
                            JacksonXmlModule().apply { setDefaultUseWrapper(false) }
                    ))
                    repository.sendProgramProcess(`is`.posctrl.posctrl_android.data.model.Process.PROGRAM_END)
                    val mPendingIntent = PendingIntent.getActivity(ctx, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT)
                    (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)[AlarmManager.RTC, System.currentTimeMillis() + 200] = mPendingIntent
                    Process.killProcess(Process.myPid())
                }
            }
        }
        val data = Data.Builder().build()
        return Result.success(data)
    }

    companion object {
        const val TAG_RESTART_WORKER = "PosCtrlRestartWorker"
    }
}