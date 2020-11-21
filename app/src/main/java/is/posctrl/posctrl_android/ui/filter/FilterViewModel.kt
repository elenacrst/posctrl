package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.BitmapsResult
import `is`.posctrl.posctrl_android.data.model.FilterResults
import `is`.posctrl.posctrl_android.util.Event
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class FilterViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    private var _bitmaps = MutableLiveData<BitmapsResult>()
    val bitmaps: LiveData<BitmapsResult>
        get() = _bitmaps
    private var _bitmapsEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val bitmapsEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _bitmapsEvent

    private var _filterAnswerEvent: MutableLiveData<Event<ResultWrapper<*>>> =
            MutableLiveData(Event(ResultWrapper.None))
    val filterAnswerEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _filterAnswerEvent//todo message

    fun downloadBitmaps(path: String, fileNames: List<String>) {
        viewModelScope.launch {

            _bitmapsEvent.value = Event(ResultWrapper.Loading)
            val result: ResultWrapper<*> = repository.downloadBitmaps(path, fileNames)

            if (result is ResultWrapper.Success) {
                _bitmaps.value = result.data as BitmapsResult
            }

            _bitmapsEvent.value = Event(result)
        }
    }

    fun sendFilterMessage(itemLineId: Int, result: FilterResults) {
        viewModelScope.launch {
            _filterAnswerEvent.value = Event(ResultWrapper.Loading)
            repository.sendFilterResultMessage(itemLineId, result)
            _filterAnswerEvent.value = Event(ResultWrapper.Success(""))
        }
    }
}
