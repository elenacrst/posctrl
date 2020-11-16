package `is`.posctrl.posctrl_android

import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import timber.log.Timber


open class BaseFragment : Fragment() {

    private fun handleError(resultError: ResultWrapper.Error): Boolean {
        return when (resultError.code) {
            ErrorCode.NO_DATA_CONNECTION.code -> {
                requireActivity().toast(getString(R.string.no_data_connection))
                true
            }
            else -> false
        }
    }

    protected fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit = { },
            errorListener: () -> Unit = { }
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
                        requireActivity().toast(
                                message = (result.peekContent() as
                                        ResultWrapper.Error).message.toString()
                        )
                        errorListener()
                    }
                }
                else -> Timber.d("Nothing to do here")
            }
        }
    }

    private fun showLoading() {
        (activity as? MainActivity)?.showLoading()
    }

    fun hideLoading() {
        (activity as? MainActivity)?.hideLoading()
    }

    override fun onDetach() {
        super.onDetach()
        hideLoading()
    }

    companion object {
        const val SECURITY_CODE = "2020"
    }
}
