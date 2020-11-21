package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity(), BaseFragmentHandler {

    private fun handleError(resultError: ResultWrapper.Error): Boolean {
        return when (resultError.code) {
            ErrorCode.NO_DATA_CONNECTION.code -> {
                toast(getString(R.string.no_data_connection))
                true
            }
            else -> false
        }
    }

    override fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit,
            errorListener: () -> Unit
    ): Observer<Event<ResultWrapper<*>>> {
        return Observer { result ->
            when (val value = result.getContentIfNotHandled()) {
                is ResultWrapper.Success -> {
                    successListener(value)
                }
                is ResultWrapper.Loading -> showLoading()
                is ResultWrapper.Error -> {
                    hideLoading()
                    val resultError =
                            result.peekContent() as ResultWrapper.Error
                    val resultHandled = handleError(resultError)
                    if (!resultHandled) {
                        toast(
                                message = (result.peekContent() as
                                        ResultWrapper.Error).message.toString()
                        )
                    }
                    errorListener()
                }
                else -> Timber.d("Nothing to do here")
            }
        }
    }

    abstract override fun showLoading()

    abstract override fun hideLoading()

    override fun onDestroy() {
        super.onDestroy()
        hideLoading()
    }

    override fun handleFilter(result: FilteredInfoResponse?) {
    }

}