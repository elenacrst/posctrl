package `is`.posctrl.posctrl_android

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class ScreenOffAdminReceiver : DeviceAdminReceiver() {
    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        showToast(
            context,
            "admin enabled"
        )
    }

    override fun onDisabled(context: Context, intent: Intent) {
        showToast(
            context,
            "admin disabled"
        )
    }
}