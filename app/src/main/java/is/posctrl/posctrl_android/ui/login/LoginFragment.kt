package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.LoginResult
import `is`.posctrl.posctrl_android.databinding.FragmentLoginBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.LoginResultReceiverService
import `is`.posctrl.posctrl_android.ui.base.BaseFragment
import `is`.posctrl.posctrl_android.ui.base.GlobalViewModel
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsFragment
import `is`.posctrl.posctrl_android.util.extensions.*
import `is`.posctrl.posctrl_android.util.glide.load
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class LoginFragment : BaseFragment(), PopupMenu.OnMenuItemClickListener {

    private lateinit var loginBinding: FragmentLoginBinding

    @Inject
    lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private var loginBroadcastReceiver = createLoginReceiver()

    private var loginCountdownTimer: CountDownTimer? = null
    private var updateDialog: androidx.appcompat.app.AlertDialog? = null

    private val globalViewModel: GlobalViewModel by activityViewModels()

    private var batteryCheckTimer: CountDownTimer = createBatteryCheckTimer()
    private var popup: PopupMenu? = null

    private fun createBatteryCheckTimer() =
            object : CountDownTimer(TimeUnit.MINUTES.toMillis(BATTERY_CHECK_INTERVAL_MINUTES), 1000) {
                override fun onFinish() {
                    startBatteryTimer()
                }

                override fun onTick(millisUntilFinished: Long) {
                }
            }

    private fun createLoginReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bundle = intent.extras
                Timber.d("received login")
                if (bundle != null) {
                    loginCountdownTimer?.cancel()
                    if (activity != null && isVisible) {
                        val result =
                                bundle.getParcelable<LoginResult>(LoginResultReceiverService.EXTRA_LOGIN)
                        handleLogin(result)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        globalViewModel.setShouldReceiveLoginResult(true)
    }

    private fun startBatteryTimer() {
        batteryCheckTimer.start()
        val bm = requireContext().getSystemService(BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        Timber.d("battery level is $batLevel")
        when (batLevel) {
            in 20..39 -> {
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_1,
                                null
                        ), null, null, null
                )
            }
            in 40..59 -> {
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_2,
                                null
                        ), null, null, null
                )
            }
            in 60..79 -> {
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_3,
                                null
                        ), null, null, null
                )
            }
            in 80..99 -> {
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_4,
                                null
                        ), null, null, null
                )
            }
            100 -> {
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_5,
                                null
                        ), null, null, null
                )
            }
            else -> {
                //0-19
                loginBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_battery_0,
                                null
                        ), null, null, null
                )
            }
        }
        loginBinding.tvBattery.text = batLevel.toString()
        loginBinding.tvBattery.append("%")
    }

    private fun handleLogin(result: LoginResult?) {
        hideLoading()
        result?.let {
            if (it.errorMessage.isNotEmpty()) {
                requireContext().toast(it.errorMessage)
            } else {
                if (it.serverPath != prefs.defaultPrefs()[getString(R.string.key_login_server), ""]) {
                    prefs.defaultPrefs()[getString(R.string.key_login_server)] = it.serverPath
                    login()
                    return@let
                }
                stopLoginService()
                globalViewModel.setShouldReceiveLoginResult(false)
                if (it.isReceivingNotifications()) {
                    startFilterReceiverService()
                }
                val versionNumber = it.appVersion.split(".")[0].toInt()
                val subVersionNumber = it.appVersion.split(".")[1].toInt()
                val currentVersionNumber = requireContext().getAppVersion().split(".")[0].toInt()
                val currentSubVersionNumber = requireContext().getAppVersion().split(".")[1].toInt()
                if (versionNumber > currentVersionNumber || versionNumber == currentVersionNumber && subVersionNumber > currentSubVersionNumber) {
                    storeLoginResultData(it, false)
                    updateDialog = requireContext().showUpdateDialog()
                    baseFragmentHandler!!.downloadApk {
                        updateDialog?.dismiss()
                    }
                } else {
                    storeLoginResultData(it, true)
                    loginViewModel.sendFilterProcessOpenMessage()
                    findNavController().navigate(LoginFragmentDirections.toRegistersFragment(it.store))
                }
                addToRememberedUsers()
            }
            for (item in it.texts) {
                prefs.defaultPrefs()[item.id] = item.string
            }
        } ?: kotlin.run {
            requireContext().toast(
                    prefs.defaultPrefs()["error_unknown", requireActivity().getString(R.string.error_unknown)]
                            ?: requireActivity().getString(R.string.error_unknown)
            )
        }
    }

    private fun addToRememberedUsers() {
        var rememberedUsersString = (prefs.defaultPrefs()[getString(R.string.key_users), ""] ?: "")
        val rememberedUsers = rememberedUsersString.split(";")
        //store as user,password,date;user2,pass2,date2;etc
        val existingUser = rememberedUsers.find { userLine ->
            userLine.split(",")
                    .isNotEmpty() && userLine.split(",")[0] == loginBinding.etUser.text.toString()
        }
        if (existingUser == null) {
            if (rememberedUsers.isNotEmpty()) {
                rememberedUsersString += ";"
            }
            rememberedUsersString += loginBinding.etUser.text.toString() + "," + loginBinding.etPassword.text.toString() + "," + System.currentTimeMillis()
            prefs.defaultPrefs()[getString(R.string.key_users)] = rememberedUsersString
        }
    }

    private fun storeLoginResultData(it: LoginResult, fullStore: Boolean = true) {
        if (fullStore) {
            prefs.customPrefs()[requireActivity().getString(R.string.key_logged_user)] =
                    loginBinding.etUser.text.toString()
            prefs.customPrefs()[requireActivity().getString(R.string.key_logged_username)] =
                    it.username
        }

        prefs.customPrefs()[requireActivity().getString(R.string.key_server_path)] =
                it.serverPath
        prefs.customPrefs()[requireActivity().getString(R.string.key_server_port)] =
                it.serverPort
        prefs.customPrefs()[requireActivity().getString(R.string.key_filter_respond_time)] =
                it.filterRespondTime
        prefs.customPrefs()[requireActivity().getString(R.string.key_app_version)] =
                it.appVersion
        prefs.customPrefs()[requireActivity().getString(R.string.key_server_user)] =
                it.serverUser
        prefs.customPrefs()[requireActivity().getString(R.string.key_server_domain)] =
                it.serverUserDomain
        prefs.customPrefs()[requireActivity().getString(R.string.key_server_password)] =
                it.serverPassword
        prefs.customPrefs()[requireActivity().getString(R.string.key_server_snapshot_path)] =
                it.serverSnapshotPath

        prefs.defaultPrefs()[requireActivity().getString(R.string.key_master_password)] =
                it.masterPassword

        prefs.customPrefs()[requireActivity().getString(R.string.key_time_zone)] =
                it.timeZone
        prefs.customPrefs()[requireActivity().getString(R.string.key_receive_notifications)] =
                it.isReceivingNotifications()
        prefs.customPrefs()[requireActivity().getString(R.string.key_notification_sound)] =
                it.isNotificationSoundEnabled()
        prefs.customPrefs()[requireActivity().getString(R.string.key_store_name)] =
                it.store.storeName
        prefs.customPrefs()[requireActivity().getString(R.string.key_store_number)] =
                it.store.storeNumber
        prefs.customPrefs()[requireActivity().getString(R.string.key_registers)] =
                it.store.registers.joinToString(",") { reg -> reg.registerNumber }
        prefs.customPrefs()[requireActivity().getString(R.string.key_registers_columns)] =
                it.store.registersColumns
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        loginBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_login, container, false)

        return loginBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startLoginResultReceiverService()

        globalViewModel.setShouldReceiveLoginResult(true)
        loginBinding.clBase.setOnSwipeListener(onSwipeBottom = {
            requireContext().showInputDialog(
                    prefs.defaultPrefs()["insert_security_code", getString(
                            R.string.insert_security_code
                    )] ?: getString(R.string.insert_security_code)
            ) {
                if (it == prefs.defaultPrefs()[requireActivity().getString(R.string.key_master_password), SECURITY_CODE] ?: SECURITY_CODE) {
                    findNavController().navigate(LoginFragmentDirections.toSettingsFragment())
                } else {
                    requireContext().toast(
                            prefs.defaultPrefs()["error_wrong_code", getString(R.string.error_wrong_code)]
                                    ?: getString(R.string.error_wrong_code)
                    )
                }
            }
        }, onSwipeLeft = {
            findNavController().navigate(
                    NavigationMainContainerDirections.toAppOptionsFragment(
                            arrayOf(
                                    AppOptionsFragment.AppOptions.APP_VERSION.name,
                                    AppOptionsFragment.AppOptions.KIOSK_MODE.name
                            ),
                            null,
                            null
                    )
            )
        })
        loginBinding.btLogin.setOnClickListener {
            validateLogin()
        }
        loginBinding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateLogin()
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener false
        }
        setupTextChangeListeners()
        checkAlreadyLoggedIn()
        loginCountdownTimer = object : CountDownTimer(LOGIN_MAX_WAIT_MILLIS, 1000) {
            override fun onFinish() {
                hideLoading()
                requireContext().toast(
                        prefs.defaultPrefs()["message_timed_out", getString(R.string.message_timed_out)]
                                ?: getString(R.string.message_timed_out)
                )
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
        setupTexts()
        loginBinding.etUser.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && loginBinding.etUser.text.isNullOrEmpty() && requireContext().getRememberedUsers(
                            prefs
                    ).isNotEmpty()
            ) {
                showPopup(v)
            }
        }
        loginBinding.etUser.setOnClickListener {
            if (loginBinding.etUser.text.isNullOrEmpty() && requireContext().getRememberedUsers(
                            prefs
                    ).isNotEmpty()
            ) {
                showPopup(it)
            }
        }
        loginBinding.ivLogo.load(requireContext(), R.drawable.logo)
        globalViewModel.wifiSignal.observe(viewLifecycleOwner, createWifiObserver())
        globalViewModel.setWifiSignal(requireContext().getWifiLevel())
        startBatteryTimer()

        globalViewModel.shouldReceiveLoginResult.observe(viewLifecycleOwner, createLoginObserver())
    }

    private fun createLoginObserver(): Observer<Boolean> {
        return Observer {
            if (it) {
                Timber.d("login result receiver started")
                startLoginResultReceiverService()
                LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                        loginBroadcastReceiver,
                        IntentFilter(LoginResultReceiverService.ACTION_RECEIVE_LOGIN)
                )
            } else {
                Timber.d("login result receiver stopped")
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
                        loginBroadcastReceiver
                )
            }
        }
    }

    private fun createWifiObserver(): Observer<Int> {
        return Observer {
            when (it) {
                0 -> {
                    loginBinding.ivWifi.setImageResource(R.drawable.ic_wifi_1)
                }
                1 -> {
                    loginBinding.ivWifi.setImageResource(R.drawable.ic_wifi_2)
                }
                2 -> {
                    loginBinding.ivWifi.setImageResource(R.drawable.ic_wifi_3)
                }
                3 -> {
                    loginBinding.ivWifi.setImageResource(R.drawable.ic_wifi_4)
                }
                4 -> {
                    loginBinding.ivWifi.setImageResource(R.drawable.ic_wifi_5)
                }
            }
        }
    }

    private fun setupTexts() {
        loginBinding.etUser.hint =
                prefs.defaultPrefs()["hint_user_id", getString(R.string.hint_user_id)]
                        ?: getString(R.string.hint_user_id)
        loginBinding.btLogin.text =
                prefs.defaultPrefs()["action_login", getString(R.string.action_login)]
                        ?: getString(R.string.action_login)
        loginBinding.etPassword.hint =
                prefs.defaultPrefs()["hint_password", getString(R.string.hint_password)]
                        ?: getString(R.string.hint_password)
    }

    private fun checkAlreadyLoggedIn() {
        if (!prefs.customPrefs()[getString(R.string.key_logged_user), ""].isNullOrEmpty() && !prefs.customPrefs()[getString(
                        R.string.key_registers
                ), ""].isNullOrEmpty()) {
            prefs.customPrefs().clear()
        }
        loginBinding.clBase.visibility = View.VISIBLE
    }

    private fun startLoginResultReceiverService() {
        LoginResultReceiverService.enqueueWork(requireContext())
    }

    private fun setupTextChangeListeners() {
        loginBinding.etUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                loginBinding.tilUser.error = ""
                if (s.isNullOrEmpty()) {
                    showPopup(loginBinding.etUser)
                }
            }

        })
        loginBinding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                loginBinding.tilPassword.error = ""
            }

        })
    }

    private fun validateLogin() {
        var valid = true
        if (loginBinding.etUser.text!!.isEmpty()) {
            valid = false
            loginBinding.tilUser.error =
                    prefs.defaultPrefs()["error_empty_user", getString(R.string.error_empty_user)]
                            ?: getString(R.string.error_empty_user)
        }
        if (loginBinding.etPassword.text!!.isEmpty()) {
            valid = false
            loginBinding.tilPassword.error =
                    prefs.defaultPrefs()["error_empty_password", getString(R.string.error_empty_password)]
                            ?: getString(R.string.error_empty_password)
        }
        if (valid) {
            login()
        }
    }

    private fun login() {
        showLoading()
        loginViewModel.login(
                loginBinding.etUser.text!!.toString(),
                loginBinding.etPassword.text!!.toString()
        )

        loginCountdownTimer?.start()
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
                ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
        startLoginResultReceiverService()
    }

    private fun startFilterReceiverService() {
        FilterReceiverService.enqueueWork(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        stopLoginService()
        loginCountdownTimer?.cancel()
        batteryCheckTimer.cancel()
        popup?.dismiss()
    }

    private fun stopLoginService() {
        val intent = Intent(requireContext(), LoginResultReceiverService::class.java)
        requireContext().stopService(intent)
    }

    private fun showPopup(v: View) {
        popup = PopupMenu(requireContext(), v, Gravity.BOTTOM or Gravity.START)
        val inflater: MenuInflater = popup!!.menuInflater
        inflater.inflate(R.menu.suggested_users, popup!!.menu)
        popup!!.setOnMenuItemClickListener(this)

        val users = requireContext().getRememberedUsers(prefs)
        for (i in users.indices) {
            popup!!.menu.add(0, MENU_FIRST_ITEM + i, Menu.NONE, users[i].userId)
        }
        popup!!.show()
    }

    override fun onPause() {
        super.onPause()
        popup?.dismiss()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        context?.let { context ->
            item?.title?.let {
                if (it.isNotEmpty()) {
                    loginBinding.etUser.setText(it)
                    loginBinding.etPassword.requestFocus()
                    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm!!.toggleSoftInput(
                            InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY
                    )
                    return true
                }
            }
        }
        return false
    }

    companion object {
        const val LOGIN_MAX_WAIT_MILLIS = 6000L
        const val MENU_FIRST_ITEM = Menu.FIRST
        const val BATTERY_CHECK_INTERVAL_MINUTES = 10L
    }
}

