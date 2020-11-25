package `is`.posctrl.posctrl_android

import `is`.posctrl.posctrl_android.di.AppModule
import `is`.posctrl.posctrl_android.di.DaggerPosCtrlComponent
import `is`.posctrl.posctrl_android.di.PosCtrlComponent
import `is`.posctrl.posctrl_android.service.AppClosingService
import android.app.Application
import android.content.Intent
import timber.log.Timber

class PosCtrlApplication : Application() {

    private lateinit var _appComponent: PosCtrlComponent
    val appComponent: PosCtrlComponent
        get() = _appComponent

    override fun onCreate() {
        super.onCreate()

        _appComponent = DaggerPosCtrlComponent.builder()
                .appModule(AppModule(this)).build()
        appComponent.inject(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        try {
            startService(Intent(baseContext, AppClosingService::class.java))
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}