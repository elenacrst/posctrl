package `is`.posctrl.posctrl_android.ui.base

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.util.Event
import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class GlobalViewModel @Inject constructor(private val repository: PosCtrlRepository) :
    ViewModel() {
    @Inject
    lateinit var appContext: Application

    private var _downloadApkEvent: MutableLiveData<Event<ResultWrapper<*>>> =
        MutableLiveData(Event(ResultWrapper.None))
    val downloadApkEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _downloadApkEvent

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
            Timber.d("Download apk duration $time")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveSettingsFromFile() {
        viewModelScope.launch {
            val time = measureTimeMillis {
                repository.saveSettingsFromFile()
            }
            Timber.d("Save settings from file duration $time")
        }
    }
}
