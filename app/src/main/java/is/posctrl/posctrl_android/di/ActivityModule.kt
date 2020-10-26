package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.ui.login.LoginViewModel
import android.app.Activity
import android.content.Context
import dagger.Module
import dagger.Provides


@Module
class ActivityModule(private val activity: Activity) {

    @Provides
    @ActivityScope
    fun provideContext(): Context = activity

    @Provides
    @ActivityScope
    fun provideActivity(): Activity = activity

    @ActivityScope
    @Provides
    fun provideLoginViewModel(repository: PosCtrlRepository): LoginViewModel =
            LoginViewModel(repository)
}