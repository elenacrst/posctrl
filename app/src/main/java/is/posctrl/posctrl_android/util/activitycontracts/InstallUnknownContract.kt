package `is`.posctrl.posctrl_android.util.activitycontracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi

class InstallUnknownContract : ActivityResultContract<Any?, Boolean>() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createIntent(context: Context, input: Any?): Intent {
        return Intent(ACTION)
            .apply {
                data = Uri.parse("package:${context.applicationContext.packageName}")
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return when (resultCode) {
            Activity.RESULT_OK -> true
            else -> false
        }
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        const val ACTION = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
    }
}