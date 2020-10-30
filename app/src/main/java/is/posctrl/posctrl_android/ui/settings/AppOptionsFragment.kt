package `is`.posctrl.posctrl_android.ui.settings

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.databinding.FragmentAppOptionsBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

class AppOptionsFragment : BaseFragment() {

    private lateinit var appOptionsBinding: FragmentAppOptionsBinding
    private var register: RegisterResponse? = null

    @Inject
    lateinit var preferencesSource: PreferencesSource

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        appOptionsBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_app_options, container, false)

        val args = AppOptionsFragmentArgs.fromBundle(
                requireArguments()
        )
        register = args.register

        return appOptionsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*if (register == null){//todo use on suspend listener
        }else{
        }*/
        appOptionsBinding.tvLogout.setOnClickListener {
            preferencesSource.customPrefs().clear()
            findNavController().navigate(NavigationMainContainerDirections.toLoginFragment())
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

