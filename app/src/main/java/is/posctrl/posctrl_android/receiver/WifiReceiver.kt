package `is`.posctrl.posctrl_android.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber


class WifiReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("wifi - received broadcast")
        val i = Intent(ACTION_WIFI_CHANGE)
        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    companion object {
        const val ACTION_WIFI_CHANGE = "is.posctrl.posctrl_android.ACTION_WIFI_CHANGE"
    }
}

