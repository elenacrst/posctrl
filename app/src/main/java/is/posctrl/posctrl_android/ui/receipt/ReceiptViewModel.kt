package `is`.posctrl.posctrl_android.ui.receipt

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.NoNetworkConnectionException
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

enum class ReceiptAction(val actionValue: String) {
    OPEN("Open"),
    CLOSE("Close"),
    ALIFE("ALife")
}

class ReceiptViewModel @Inject constructor(private val repository: PosCtrlRepository) :
        ViewModel() {

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var appContext: Application

    fun sendReceiptInfoMessage(action: ReceiptAction, storeNumber: Int, registerNumber: Int) {
        viewModelScope.launch {
            val time = measureTimeMillis {
                try {
                    repository.sendReceiptInfoMessage(action, storeNumber, registerNumber)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                }
            }

            Timber.d("Receipt info send duration $time")
        }
    }

    fun sendReceiptInfoALife(storeNumber: Int, registerNumber: Int) {
        viewModelScope.launch {
            val isAlreadySending = prefs.customPrefs()[appContext.getString(R.string.key_send_alife, storeNumber, registerNumber)]
                    ?: false
            if (isAlreadySending) {
                return@launch
            }
            val time = measureTimeMillis {
                try {
                    repository.sendReceiptInfoALife(storeNumber, registerNumber)
                } catch (e: NoNetworkConnectionException) {
                    e.printStackTrace()
                }
            }

            Timber.d("Receipt info send alife duration $time")
        }
    }
}
