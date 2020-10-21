package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.databinding.FragmentLoginBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.di.PosCtrlComponent
import `is`.posctrl.posctrl_android.util.Event
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import javax.inject.Inject

class LoginFragment : BaseFragment() {

    private lateinit var loginBinding: FragmentLoginBinding
    private lateinit var activityComponent: ActivityComponent

    @Inject
    lateinit var loginViewModel: LoginViewModel

    private fun getLoginObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(
                progressListener = {
                },
                successListener = {

                },
                errorListener = {
                }
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        loginBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_login, container, false)

        return loginBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginViewModel.loginEvent.observe(viewLifecycleOwner, getLoginObserver())
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

