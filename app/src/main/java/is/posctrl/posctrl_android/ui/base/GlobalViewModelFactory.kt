package `is`.posctrl.posctrl_android.ui.base

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class GlobalViewModelFactory(private val repository: PosCtrlRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GlobalViewModel(repository) as T
    }
}