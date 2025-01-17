package `is`.posctrl.posctrl_android.service

import android.app.Service
import android.content.Context
import android.content.Intent


class ForegroundServiceLauncher(private val serviceClass: Class<out Service>) {

    private var isStarting = false
    private var shouldStop = false

    @Synchronized
    fun onStartService() {
        isStarting = true
        shouldStop = false
    }

    @Synchronized
    fun stopService(context: Context) {
        if (isStarting) {
            shouldStop = true
        } else {
            context.stopService(Intent(context, serviceClass))
        }
    }

    @Synchronized
    fun onServiceCreated(service: Service) {
        isStarting = false
        if (shouldStop) {
            shouldStop = false
            service.stopSelf()
        }
    }
}