package `is`.posctrl.posctrl_android.receiver

import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Restart your app here
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Timber.d("received install package broadcast")
            val i =
                context.applicationContext.packageManager.getLaunchIntentForPackage(context.applicationContext.packageName)
            i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(i)
            val preferencesSource = PreferencesSource(context.applicationContext)
            preferencesSource.customPrefs().clear()
        }

    }
}