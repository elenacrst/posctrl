package `is`.posctrl.posctrl_android.ui.settings.appoptions

import `is`.posctrl.posctrl_android.ui.base.BaseFragment
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
import `is`.posctrl.posctrl_android.ui.base.GlobalViewModel
import `is`.posctrl.posctrl_android.ui.login.LoginViewModel
import `is`.posctrl.posctrl_android.util.extensions.getAppVersion
import `is`.posctrl.posctrl_android.util.extensions.showInputDialog
import `is`.posctrl.posctrl_android.util.extensions.toast
import `is`.posctrl.posctrl_android.util.glide.load
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import javax.inject.Inject


class AppOptionsFragment : BaseFragment() {

    private lateinit var appOptionsBinding: FragmentAppOptionsBinding
    private var store: StoreResult? = null
    private var options: Array<String> = arrayOf()
    val globalViewModel: GlobalViewModel by activityViewModels()

    @Inject
    lateinit var preferencesSource: PreferencesSource

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

    @Inject
    lateinit var loginViewModel: LoginViewModel

    enum class AppOptions {
        APP_VERSION,
        KIOSK_MODE,
        USER_INFO,
        LOGOUT,
        SUSPEND_REGISTER,
        STORE_INFO
    }

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
        options = args.options

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
            globalViewModel.setShouldReceiveLoginResult(true)
        }
        appOptionsBinding.btSuspend.setOnClickListener {
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
            preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true]
                ?: true
        appOptionsBinding.tvKiosk.setOnClickListener {
            requireContext().showInputDialog(
                preferencesSource.defaultPrefs()["insert_security_code", getString(
                    R.string.insert_security_code
                )] ?: getString(R.string.insert_security_code)
            ) {
                if (it == preferencesSource.defaultPrefs()[requireActivity().getString(R.string.key_master_password), SECURITY_CODE] ?: SECURITY_CODE) {
                    enableKioskMode(!appOptionsBinding.swKiosk.isChecked)
                } else {
                    requireContext().toast(
                        preferencesSource.defaultPrefs()["error_wrong_code", getString(
                            R.string.error_wrong_code
                        )] ?: getString(R.string.error_wrong_code)
                    )
                }
            }

        }
        setupTexts()
        setupOptionsVisibility()
        appOptionsBinding.ivLogo.load(requireContext(), R.drawable.logo)
    }

    private fun setupOptionsVisibility() {
        if (options.contains(AppOptions.USER_INFO.name)) {
            appOptionsBinding.tvLoggedIn.visibility = View.VISIBLE
        } else {
            appOptionsBinding.tvLoggedIn.visibility = View.GONE
        }
        if (options.contains(AppOptions.STORE_INFO.name)) {
            appOptionsBinding.tvStoreInfo.visibility = View.VISIBLE
        } else {
            appOptionsBinding.tvStoreInfo.visibility = View.GONE
        }
        if (options.contains(AppOptions.APP_VERSION.name)) {
            appOptionsBinding.tvAppVersion.visibility = View.VISIBLE
        } else {
            appOptionsBinding.tvAppVersion.visibility = View.GONE
        }
        if (options.contains(AppOptions.LOGOUT.name)) {
            appOptionsBinding.tvLogout.visibility = View.VISIBLE
        } else {
            appOptionsBinding.tvLogout.visibility = View.GONE
        }
        if (options.contains(AppOptions.KIOSK_MODE.name)) {
            appOptionsBinding.tvKiosk.visibility = View.VISIBLE
            appOptionsBinding.swKiosk.visibility = View.VISIBLE
        } else {
            appOptionsBinding.tvKiosk.visibility = View.GONE
            appOptionsBinding.swKiosk.visibility = View.GONE
        }
        if (options.contains(AppOptions.SUSPEND_REGISTER.name)) {
            appOptionsBinding.btSuspend.visibility = View.VISIBLE
        } else {
            appOptionsBinding.btSuspend.visibility = View.GONE
        }
    }

    private fun setupTexts() {
        appOptionsBinding.tvLogout.text =
            preferencesSource.defaultPrefs()["action_logout", getString(R.string.action_logout)]
                ?: getString(R.string.action_logout)
        appOptionsBinding.btSuspend.text =
            preferencesSource.defaultPrefs()["action_suspend_register", getString(R.string.action_suspend_register)]
                ?: getString(R.string.action_suspend_register)
        appOptionsBinding.tvKiosk.text =
            preferencesSource.defaultPrefs()["action_kiosk_mode", getString(R.string.action_kiosk_mode)]
                ?: getString(R.string.action_kiosk_mode)

        val user = preferencesSource.customPrefs()[getString(R.string.key_logged_username), "null"]
            ?: "null"
        val userId = preferencesSource.customPrefs()[getString(R.string.key_logged_user), "null"]
            ?: "null"
        var loggedInText =
            preferencesSource.defaultPrefs()["login_value", getString(
                R.string.login_value,
                userId,
                user
            )]
                ?: getString(R.string.login_value, userId, user)
        loggedInText = loggedInText.replace("%1\$s", userId)
        loggedInText = loggedInText.replace("%2\$s", user)
        appOptionsBinding.tvLoggedIn.text = loggedInText

        store?.let {
            var storeText =
                preferencesSource.defaultPrefs()["current_store_value", getString(
                    R.string.current_store_value,
                    it.storeNumber,
                    it.storeName
                )]
                    ?: getString(R.string.current_store_value, it.storeNumber, it.storeName)
            storeText = storeText.replace("%1\$s", it.storeNumber)
            storeText = storeText.replace("%2\$s", it.storeName)
            appOptionsBinding.tvStoreInfo.text = storeText
        }

        var versionText =
            preferencesSource.defaultPrefs()["app_version_value", getString(
                R.string.app_version_value,
                requireContext().getAppVersion()
            )]
                ?: getString(R.string.app_version_value, requireContext().getAppVersion())
        versionText = versionText.replace("%s", requireContext().getAppVersion())
        appOptionsBinding.tvAppVersion.text = versionText


    }

    private fun enableKioskMode(enable: Boolean) {
        var kiosk = preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true]
        Timber.d("kiosk enable start: $kiosk")
        if (enable) {
            preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode)] = true
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
            preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode)] = false
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
        kiosk = preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true]
        Timber.d("kiosk enable end: $kiosk")
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

