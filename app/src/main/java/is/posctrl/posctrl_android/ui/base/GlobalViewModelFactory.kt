package `is`.posctrl.posctrl_android.ui.base

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class GlobalViewModelFactory(private val repository: PosCtrlRepository, private val appContext: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GlobalViewModel(repository, appContext) as T
    }
}