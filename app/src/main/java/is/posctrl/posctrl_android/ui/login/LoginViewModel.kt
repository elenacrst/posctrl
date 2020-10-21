package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class LoginViewModel @Inject constructor(private val repository: PosCtrlRepository) :
    ViewModel() {

    /* todo define models private var _loginResponse: MutableLiveData<...> = MutableLiveData()
     val loginResponse: LiveData<...>
         get() = _loginResponse*/
    private var _loginEvent: MutableLiveData<Event<ResultWrapper<*>>> =
        MutableLiveData(Event(ResultWrapper.None))
    val loginEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _loginEvent

    fun login() {
        viewModelScope.launch {
            _loginEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.login()
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
                /*if (result is ResultWrapper.Success) {
                    _loginResponse.value = values
                }*/

            }

            Timber.d("Login duration $time")
            _loginEvent.value = Event(result)
        }
    }
}
