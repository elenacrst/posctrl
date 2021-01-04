package `is`.posctrl.posctrl_android.receiver

import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class UpdateReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("received install package broadcast")
        val i =
                context.applicationContext.packageManager.getLaunchIntentForPackage(context.applicationContext.packageName)
        i?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(i)
        val preferencesSource = PreferencesSource(context.applicationContext)
        preferencesSource.customPrefs().clear()
    }
}