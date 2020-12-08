package `is`.posctrl.posctrl_android.receiver

import `is`.posctrl.posctrl_android.ui.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Restart your app here
        val i = Intent(context, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}