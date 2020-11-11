package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.util.Event
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class FilterViewModel @Inject constructor(private val repository: PosCtrlRepository) :
    ViewModel() {

    private var _bitmaps = MutableLiveData<List<Bitmap>>()
    val bitmaps: LiveData<List<Bitmap>>
        get() = _bitmaps
    private var _bitmapsEvent: MutableLiveData<Event<ResultWrapper<*>>> =
        MutableLiveData(Event(ResultWrapper.None))
    val bitmapsEvent: LiveData<Event<ResultWrapper<*>>>
        get() = _bitmapsEvent

    fun downloadBitmaps(
        fileNames: List<String>
    ) {
        viewModelScope.launch {
            _bitmapsEvent.value = Event(ResultWrapper.Loading)
            val result: ResultWrapper<*> = repository.downloadBitmaps(fileNames)

            if (result is ResultWrapper.Success) {
                _bitmaps.value = (result.data as List<*>).filterIsInstance(Bitmap::class.java)
            }

            _bitmapsEvent.value = Event(result)
        }
    }
}
