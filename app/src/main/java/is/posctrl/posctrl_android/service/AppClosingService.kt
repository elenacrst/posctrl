package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.model.FilterAction
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
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
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesSource = PreferencesSource(applicationContext)
        repository = PosCtrlRepository(preferencesSource, applicationContext, XmlMapper(
                JacksonXmlModule().apply { setDefaultUseWrapper(false) }
        ))
    }

    private fun stopFilterReceiverService() {
        FilterReceiverService.enqueueWork(this, FilterReceiverService.Actions.STOP.name)
    }
}