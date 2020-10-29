package `is`.posctrl.posctrl_android.ui.registers

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class RegistersViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    private var _registers = MutableLiveData<List<RegisterResponse>>()
    val registers: LiveData<List<RegisterResponse>>
        get() = _registers
    private var _registersEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val registersEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _registersEvent

    fun getRegisters(server: String = "", port: String = "", databaseUser: String = "", databasePassword: String = "", storeNumber: Int = -1, loggedInUser: String = "") {
        viewModelScope.launch {
            _registersEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.getRegisters(server, port, databaseUser, databasePassword, storeNumber, loggedInUser)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
                if (result is ResultWrapper.Success) {
                    _registers.value = ((result as ResultWrapper.Success).data as List<*>).filterIsInstance(RegisterResponse::class.java)
                }
            }

            Timber.d("Get registers duration $time")
            _registersEvent.value = Event(result)
        }
    }
}
