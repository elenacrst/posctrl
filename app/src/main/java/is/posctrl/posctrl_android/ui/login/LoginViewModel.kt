package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.LoginResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class LoginViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    private var _loginResponse = MutableLiveData<LoginResponse>()
    val loginResponse: LiveData<LoginResponse>
        get() = _loginResponse
    private var _loginEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val loginEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _loginEvent

    private var _stores = MutableLiveData<List<StoreResponse>>()
    val stores: LiveData<List<StoreResponse>>
        get() = _stores
    private var _storesEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val storesEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _storesEvent

    fun login(server: String = "", port: String = "", databaseUser: String = "", databasePassword: String = "", user: String = "", password: String = "") {
        viewModelScope.launch {
            _loginEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.login(server, port, databaseUser, databasePassword, user, password)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
                if (result is ResultWrapper.Success) {
                    _loginResponse.value = (result as ResultWrapper.Success).data as LoginResponse
                }

            }

            Timber.d("Login duration $time")
            _loginEvent.value = Event(result)
        }
    }

    fun getStores(server: String = "", port: String = "", databaseUser: String = "", databasePassword: String = "", loggedInUser: String = "") {
        viewModelScope.launch {
            _storesEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.getStores(server, port, databaseUser, databasePassword, loggedInUser)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
                if (result is ResultWrapper.Success) {
                    _stores.value = ((result as ResultWrapper.Success).data as List<*>).filterIsInstance(StoreResponse::class.java)
                }

            }

            Timber.d("Get stores duration $time")
            _storesEvent.value = Event(result)
        }
    }
}
