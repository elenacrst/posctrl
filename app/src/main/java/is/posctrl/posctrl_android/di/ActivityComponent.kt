package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.ui.filter.FilterActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.receipt.ReceiptFragment
import `is`.posctrl.posctrl_android.ui.registers.RegistersFragment
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsFragment
import `is`.posctrl.posctrl_android.ui.settings.RegisterSelectionFragment
import `is`.posctrl.posctrl_android.ui.settings.SettingsFragment
import dagger.Subcomponent
import javax.inject.Scope

@Scope
@Retention
annotation class ActivityScope

@ActivityScope
@Subcomponent(
    modules = [
        ActivityModule::class
    ],
)
interface ActivityComponent {
    fun inject(fragment: LoginFragment)
    fun inject(registersFragment: RegistersFragment)
    fun inject(appOptionsFragment: AppOptionsFragment)
    fun inject(receiptFragment: ReceiptFragment)
    fun inject(registerSelectionFragment: RegisterSelectionFragment)
    fun inject(filterActivity: FilterActivity)
    fun inject(baseActivity: BaseActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(settingsFragment: SettingsFragment)
}
