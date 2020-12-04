package `is`.posctrl.posctrl_android.worker

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsViewModel
import `is`.posctrl.posctrl_android.util.scheduleLogout
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.fasterxml.jackson.dataformat.xml.XmlMapper

class LogoutWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = PreferencesSource(ctx)
        prefs.customPrefs().clear()
        stopFilterReceiverService(ctx)
        stopReceiptReceiverService(ctx)
        val appOptionsViewModel = AppOptionsViewModel(PosCtrlRepository(prefs, ctx, XmlMapper()))
        appOptionsViewModel.closeFilterNotifications()
        ctx.scheduleLogout(true)
        val i = Intent(BaseActivity.ACTION_LOGOUT)
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i)
        val data = Data.Builder().build()
        return Result.success(data)
    }

    private fun stopFilterReceiverService(context: Context) {
        val intent = Intent(context, FilterReceiverService::class.java)
        context.stopService(intent)
    }

    private fun stopReceiptReceiverService(context: Context) {
        val intent = Intent(context, ReceiptReceiverService::class.java)
        context.stopService(intent)
    }

    companion object {
        const val TAG_LOGOUT_WORKER = "PosCtrlLogoutWorker"
    }
}