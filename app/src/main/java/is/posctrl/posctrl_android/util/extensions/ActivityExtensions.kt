package `is`.posctrl.posctrl_android.util.extensions

import `is`.posctrl.posctrl_android.ScreenOffAdminReceiver
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import timber.log.Timber

fun Activity.lockScreen() {
    val policyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminReceiver = ComponentName(
        this,
        ScreenOffAdminReceiver::class.java
    )
    val admin = policyManager.isAdminActive(adminReceiver)
    if (admin) {
        Timber.d("Going to sleep now.")
        policyManager.lockNow()
    } else {
        Timber.d("Not an admin")
        Toast.makeText(
            this, "not a device admin",
            Toast.LENGTH_LONG
        ).show()
    }
}