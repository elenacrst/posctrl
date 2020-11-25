package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilterAction
import `is`.posctrl.posctrl_android.util.Event
import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class LoginViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {
    @Inject
    lateinit var appContext: Application

    private var _sendFilterProcessEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val sendFilterProcessEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _sendFilterProcessEvent

    fun login(
            user: String = "",
            password: String = ""
    ) {
        viewModelScope.launch {
            val time = measureTimeMillis {
                try {
                    repository.sendLoginMessage(user, password)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }
            Timber.d("Send login duration $time")
        }
    }

    fun sendFilterProcessOpenMessage() {
        viewModelScope.launch {
            _sendFilterProcessEvent.value = Event(ResultWrapper.Loading)
            val time = measureTimeMillis {
                try {
                    repository.sendFilterProcessMessage(FilterAction.OPEN)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }

            Timber.d("Send filter process message duration $time")
            _sendFilterProcessEvent.value = Event(ResultWrapper.Success(""))
        }
    }
}
