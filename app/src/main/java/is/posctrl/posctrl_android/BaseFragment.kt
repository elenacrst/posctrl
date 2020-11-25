package `is`.posctrl.posctrl_android

import `is`.posctrl.posctrl_android.ui.BaseFragmentHandler
import android.content.Context
import androidx.fragment.app.Fragment


open class BaseFragment : Fragment() {

    protected var baseFragmentHandler: BaseFragmentHandler? = null

    fun showLoading() {
        baseFragmentHandler?.showLoading()
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
