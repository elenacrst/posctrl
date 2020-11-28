package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.FilterAction
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class AppClosingService : Service() {

    @Inject
    lateinit var repository: PosCtrlRepository

    @Inject
    lateinit var preferencesSource: PreferencesSource

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Timber.d("closed app")

        GlobalScope.launch {
            repository.sendFilterProcessMessage(FilterAction.CLOSE)
            stopFilterReceiverService()

            stopSelf()
            preferencesSource.defaultPrefs()[getString(R.string.key_app_visible)] = false
        }

    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext as PosCtrlApplication).appComponent.inject(this)
        Timber.d("App closing service has been created")
    }

    private fun stopFilterReceiverService() {
        val intent = Intent(this, FilterReceiverService::class.java)
        stopService(intent)
    }
}