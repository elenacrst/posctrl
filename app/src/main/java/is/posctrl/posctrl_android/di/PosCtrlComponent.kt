package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.data.local.LocalInformation
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import android.app.Application
import android.content.Context
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Scope
import javax.inject.Singleton

@ApplicationScope
@Component(
    modules = [
        AppModule::class,
        //MainActivityModule::class
    ]
)
interface PosCtrlComponent {
    fun activityComponent(activityModule: ActivityModule): ActivityComponent
    fun appContext(): Application

    fun localInfo(): LocalInformation

    fun preferences(): PreferencesSource

//    fun inject(activity: MainActivity)
    fun inject(app: PosCtrlApplication)
   // fun inject(fragment: LoginFragment)
}

@Scope
@Retention
annotation class ApplicationScope
