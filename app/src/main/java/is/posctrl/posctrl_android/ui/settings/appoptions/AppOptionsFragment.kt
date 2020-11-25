package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentAppOptionsBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.ui.login.LoginViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

class AppOptionsFragment : BaseFragment() {

    private lateinit var appOptionsBinding: FragmentAppOptionsBinding
    private var store: StoreResult? = null

    @Inject
    lateinit var preferencesSource: PreferencesSource

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

    @Inject
    lateinit var loginViewModel: LoginViewModel

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
        store = args.store

        return appOptionsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appOptionsBinding.tvLogout.setOnClickListener {
            preferencesSource.customPrefs().clear()
            findNavController().navigate(NavigationMainContainerDirections.toLoginFragment())
            stopFilterReceiverService()
            stopReceiptReceiverService()
        }
        appOptionsBinding.tvSuspend.setOnClickListener {
            findNavController().navigate(
                    AppOptionsFragmentDirections.toRegisterSelectionFragment(
                            store!!
                    )
            )
        }
        appOptionsBinding.store = store
        appOptionsBinding.loggedInUser =
                preferencesSource.customPrefs()[getString(R.string.key_logged_username)]
    }

    private fun stopReceiptReceiverService() {
        val intent = Intent(requireContext(), ReceiptReceiverService::class.java)
        requireActivity().stopService(intent)
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
                ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }

    private fun stopFilterReceiverService() {
        val intent = Intent(requireContext(), FilterReceiverService::class.java)
        requireActivity().stopService(intent)
    }
}

