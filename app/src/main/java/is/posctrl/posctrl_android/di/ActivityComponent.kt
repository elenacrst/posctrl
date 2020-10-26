package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.registers.RegistersFragment
import `is`.posctrl.posctrl_android.ui.stores.StoresFragment
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
    fun inject(activity: MainActivity)
    fun inject(fragment: LoginFragment)
    fun inject(registersFragment: RegistersFragment)
    fun inject(storesFragment: StoresFragment)
}
