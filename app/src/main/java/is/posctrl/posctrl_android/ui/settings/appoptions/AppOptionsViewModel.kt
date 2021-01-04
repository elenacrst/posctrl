package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilterAction
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class AppOptionsViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {
    fun suspendRegister(storeNumber: String, registerNumber: String) {
        viewModelScope.launch {
            val time = measureTimeMillis {
                try {
                    repository.sendSuspendRegisterMessage(storeNumber, registerNumber)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }

            Timber.d("Suspend register duration $time")
        }
    }

    fun closeFilterNotifications() {
        viewModelScope.launch {
            val time = measureTimeMillis {
                try {
                    repository.sendFilterProcessMessage(FilterAction.CLOSE)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }
            Timber.d("Close filter notifications duration $time")
        }
    }

}
