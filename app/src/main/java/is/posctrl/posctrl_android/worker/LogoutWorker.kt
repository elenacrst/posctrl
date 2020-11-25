package `is`.posctrl.posctrl_android.worker

import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.util.scheduleLogout
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class LogoutWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = PreferencesSource(ctx)
        prefs.customPrefs().clear()

        ctx.scheduleLogout(true)

        val data = Data.Builder().build()
        return Result.success(data)
    }

    companion object {
        const val TAG_LOGOUT_WORKER = "PosCtrlLogoutWorker"
    }
}