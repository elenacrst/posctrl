package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentAppOptionsBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.ui.login.LoginViewModel
import `is`.posctrl.posctrl_android.util.extensions.showInputDialog
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
    ): View {
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
            appOptionsViewModel.closeFilterNotifications()
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
        appOptionsBinding.swKiosk.isChecked =
            preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode), true]
                ?: true
        appOptionsBinding.tvKiosk.setOnClickListener {
            requireContext().showInputDialog(R.string.insert_security_code) {
                if (it == preferencesSource.defaultPrefs()[requireActivity().getString(R.string.key_master_password), SECURITY_CODE] ?: SECURITY_CODE) {
                    enableKioskMode(!appOptionsBinding.swKiosk.isChecked)
                } else {
                    requireContext().toast(getString(R.string.error_wrong_code))
                }
            }

        }
        /*appOptionsBinding.clBase.setOnSwipeListener(onDoubleTap = {
            baseFragmentHandler?.onDoubleTap()
        })*/
    }

    private fun enableKioskMode(enable: Boolean) {
        if (enable) {
            preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode)] = true
            appOptionsBinding.swKiosk.isChecked = true

            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val pm: PackageManager = requireActivity().applicationContext.packageManager
            val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.activityInfo?.packageName != requireActivity().packageName) {
                val compName = ComponentName(
                    requireContext(),

                    MainActivity::class.java
                )
                pm.setComponentEnabledSetting(
                    compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                pm.setComponentEnabledSetting(
                    compName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

            }

        } else {
            preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode)] = false
            appOptionsBinding.swKiosk.isChecked = false
            val pm: PackageManager = requireActivity().applicationContext.packageManager
            val compName = ComponentName(
                requireContext(),
                MainActivity::class.java
            )
            pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
                PackageManager.DONT_KILL_APP
            )
        }
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

