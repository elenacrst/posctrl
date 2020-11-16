package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilterAction
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class AppOptionsViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    private var _suspendEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val suspendEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _suspendEvent

    private var _closeFilterNotificationsEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val closeFilterNotificationsEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _closeFilterNotificationsEvent

    //todo on success, display a toast message
    fun suspendRegister(storeNumber: Int, registerNumber: Int) {
        viewModelScope.launch {
            _suspendEvent.value = Event(ResultWrapper.Loading)
            val time = measureTimeMillis {
                try {
                    repository.sendSuspendRegisterMessage(storeNumber, registerNumber)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }

            Timber.d("Suspend register duration $time")
            _suspendEvent.value = Event(ResultWrapper.Success(""))
        }
    }

    fun closeFilterNotifications() {
        viewModelScope.launch {
            _closeFilterNotificationsEvent.value = Event(ResultWrapper.Loading)
            val time = measureTimeMillis {
                try {
                    repository.sendFilterProcessMessage(FilterAction.CLOSE)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }

            Timber.d("Close filter notifications duration $time")
            _closeFilterNotificationsEvent.value = Event(ResultWrapper.Success(""))
        }
    }
}
