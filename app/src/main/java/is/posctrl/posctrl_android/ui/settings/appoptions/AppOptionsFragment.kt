package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.FragmentAppOptionsBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.util.extensions.showConfirmDialog
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
    private var register: RegisterResponse? = null
    private var store: StoreResponse? = null

    @Inject
    lateinit var preferencesSource: PreferencesSource

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

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
        store = args.store

        return appOptionsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appOptionsBinding.tvLogout.setOnClickListener {
            preferencesSource.customPrefs().clear()
            findNavController().navigate(NavigationMainContainerDirections.toLoginFragment())
            //todo stop services here
        }
        appOptionsBinding.tvSuspend.setOnClickListener {
            if (register == null) {
                //show registers list first
                findNavController().navigate(AppOptionsFragmentDirections.toRegisterSelectionFragment(store!!))
            } else {
                //show confirm dialog
                requireContext().showConfirmDialog(getString(R.string.confirm_suspend_register, register!!.registerNumber
                        ?: -1)) {
                    appOptionsViewModel.suspendRegister(store!!.storeNumber?.toInt()
                            ?: -1, register!!.registerNumber ?: -1)
                }
            }
        }
        appOptionsBinding.store = store

        appOptionsBinding.swSound.isChecked = preferencesSource.customPrefs()[getString(R.string.key_notification_sound), true]
                ?: true
        appOptionsBinding.swSound.setOnCheckedChangeListener { _, isChecked ->
            preferencesSource.customPrefs()[getString(R.string.key_notification_sound)] = isChecked
        }

        appOptionsBinding.swReceiveNotifications.isChecked = preferencesSource.customPrefs()[getString(R.string.key_receive_notifications), true]
                ?: true
        appOptionsBinding.swReceiveNotifications.setOnCheckedChangeListener { _, isChecked ->
            preferencesSource.customPrefs()[getString(R.string.key_receive_notifications)] = isChecked
            if (isChecked) {
                //start service
                startFilterReceiverService()
            } else {
                //stop service
                stopFilterReceiverService()
            }
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }

    private fun startFilterReceiverService() {
        val intent = Intent(requireContext(), FilterReceiverService::class.java)
        FilterReceiverService.enqueueWork(requireContext(), intent)
    }

    private fun stopFilterReceiverService() {
        val intent = Intent(requireContext(), FilterReceiverService::class.java)
        requireActivity().stopService(intent)
    }
}

