package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.service.*
import android.app.Application
import dagger.Component
import javax.inject.Scope

@ApplicationScope
@Component(
        modules = [
            AppModule::class,
        ]
)
interface PosCtrlComponent {
    fun activityComponent(activityModule: ActivityModule): ActivityComponent
    fun appContext(): Application
    fun preferences(): PreferencesSource

    fun inject(app: PosCtrlApplication)
    fun inject(receiptReceiverService: ReceiptReceiverService)
    fun inject(filterReceiverService: FilterReceiverService)
    fun inject(appClosingService: AppClosingService)
    fun inject(loginResultReceiverService: LoginResultReceiverService)
    fun inject(chargingService: ChargingService)
}

@Scope
@Retention
annotation class ApplicationScope
