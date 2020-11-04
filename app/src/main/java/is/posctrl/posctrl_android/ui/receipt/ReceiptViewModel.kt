package `is`.posctrl.posctrl_android.ui.receipt

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

enum class ReceiptAction(val actionValue: String){
    OPEN("Open"),
    CLOSE("Close"),
    ALIFE("ALife")
}

class ReceiptViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    private var _receiptInfoRequestEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val receiptInfoRequestEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _receiptInfoRequestEvent

    fun sendReceiptInfoMessage(action: ReceiptAction, storeNumber: Int, registerNumber: Int) {
        viewModelScope.launch {
            _receiptInfoRequestEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.sendReceiptInfoMessage(action, storeNumber, registerNumber)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }

            Timber.d("Receipt info send duration $time")
            _receiptInfoRequestEvent.value = Event(result)
        }
    }
}
