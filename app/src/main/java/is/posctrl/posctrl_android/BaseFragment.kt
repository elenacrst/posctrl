package `is`.posctrl.posctrl_android

import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.ui.BaseFragmentHandler
import `is`.posctrl.posctrl_android.util.Event
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer


open class BaseFragment : Fragment() {

    protected var baseFragmentHandler: BaseFragmentHandler? = null

    protected fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit = { },
            errorListener: () -> Unit = { }
    ): Observer<Event<ResultWrapper<*>>> {
        return baseFragmentHandler?.createLoadingObserver(successListener, errorListener)
                ?: Observer { }
    }

    fun hideLoading() {
        baseFragmentHandler?.hideLoading()
    }

    override fun onDetach() {
        super.onDetach()
        hideLoading()
        baseFragmentHandler = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseFragmentHandler) {
            baseFragmentHandler = context
        }
    }

    companion object {
        const val SECURITY_CODE = "2020"
    }
}
