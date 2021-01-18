package `is`.posctrl.posctrl_android.ui.base

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.data.model.ReceiptResponse
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class GlobalViewModel @Inject constructor(
        private val repository: PosCtrlRepository
) :
        ViewModel() {

    private var _downloadApkEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val downloadApkEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _downloadApkEvent

    private var _receiptItems: MutableLiveData<List<ReceiptResponse>> = MutableLiveData()
    val receiptItems: LiveData<List<ReceiptResponse>>
        get() = _receiptItems

    private var _isReceivingReceipt: MutableLiveData<Boolean> = MutableLiveData(false)
    val isReceivingReceipt: LiveData<Boolean>
        get() = _isReceivingReceipt

    private var _wifiSignal: MutableLiveData<Int> = MutableLiveData(0)
    val wifiSignal: LiveData<Int>
        get() = _wifiSignal

    private var _isReceivingFilter: MutableLiveData<Boolean> = MutableLiveData(false)
    val isReceivingFilter: LiveData<Boolean>
        get() = _isReceivingFilter

    private var _filterItemMessages: MutableLiveData<List<FilteredInfoResponse>> =
            MutableLiveData(mutableListOf())
    val filterItemMessages: LiveData<List<FilteredInfoResponse>>
        get() = _filterItemMessages

    private var _shouldReceiveLoginResult: MutableLiveData<Boolean> = MutableLiveData(true)
    val shouldReceiveLoginResult: LiveData<Boolean>
        get() = _shouldReceiveLoginResult

    private var _logout: MutableLiveData<Boolean> = MutableLiveData(false)
    val logout: LiveData<Boolean>
        get() = _logout

    fun downloadApk() {
        viewModelScope.launch {
            _downloadApkEvent.value = Event(ResultWrapper.Loading)
            var result: ResultWrapper<*>
            val time = measureTimeMillis {
                result = try {
                    repository.downloadApk()
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                    ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }
            }
            _downloadApkEvent.value = Event(result)
//            Timber.d("Download apk duration $time")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveSettingsFromFile() {
        viewModelScope.launch {
            val time = measureTimeMillis {
                repository.saveSettingsFromFile()
            }
            //  Timber.d("Save settings from file duration $time")
        }
    }

    fun addReceiptResult(result: ReceiptResponse) {
        if (receiptItems.value.isNullOrEmpty() || receiptItems.value!!.last().clearTextFlag != result.clearTextFlag) {
            _receiptItems.value = listOf(result)
        } else {
            _receiptItems.value = receiptItems.value!! + result
        }
    }

    fun clearReceipt() {
        _receiptItems.value = listOf()
    }

    fun setReceivingReceipt(b: Boolean) {
        _isReceivingReceipt.value = b
    }

    fun setWifiSignal(level: Int) {
        if (level != wifiSignal.value) {
            _wifiSignal.value = level
        }

    }

    fun setReceivingFilter(b: Boolean) {
        _isReceivingFilter.value = b
    }

    fun addFilter(item: FilteredInfoResponse) {
        filterItemMessages.value?.firstOrNull {
            it.storeNumber == item.storeNumber && it.registerNumber == item.registerNumber && it.txn == item.txn && it.itemSeqNumber == item.itemSeqNumber
        }?.apply { return }
        _filterItemMessages.value = (filterItemMessages.value
                ?: mutableListOf<FilteredInfoResponse>()) + item


    }

    fun removeFirstFilter() {
        if (filterItemMessages.value.isNullOrEmpty()) {
            return
        }
        _filterItemMessages.value = filterItemMessages.value!! - filterItemMessages.value!![0]
    }

    fun setShouldReceiveLoginResult(b: Boolean) {
        _shouldReceiveLoginResult.value = b
    }

    fun clearFilterMessages() {
        _filterItemMessages.value = mutableListOf()
    }
}
