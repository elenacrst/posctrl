package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.LoginResult
import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentLoginBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.LoginResultReceiverService
import `is`.posctrl.posctrl_android.util.extensions.*
import `is`.posctrl.posctrl_android.util.scheduleLogout
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import javax.inject.Inject

class LoginFragment : BaseFragment() {

    private lateinit var loginBinding: FragmentLoginBinding

    @Inject
    lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private var loginBroadcastReceiver = createLoginReceiver()

    private var loginCountdownTimer: CountDownTimer? = null
    private var updateDialog: androidx.appcompat.app.AlertDialog? = null

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
    }

    private fun handleLogin(result: LoginResult?) {
        hideLoading()
        result?.let {
            if (it.errorMessage.isNotEmpty()) {
                requireContext().toast(it.errorMessage)
            } else {
                stopLoginService()
                LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(loginBroadcastReceiver)

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
            }
        } ?: kotlin.run {
            requireContext().toast(requireActivity().getString(R.string.error_unknown))
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loginBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_login, container, false)
        requireContext().scheduleLogout()

        return loginBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs.customPrefs()[getString(R.string.key_kiosk_mode)] = true
        loginBinding.clBase.setOnSwipeListener(onSwipeBottom = {
            requireContext().showInputDialog(R.string.insert_security_code) {
                if (it == prefs.defaultPrefs()[requireActivity().getString(R.string.key_master_password), SECURITY_CODE] ?: SECURITY_CODE) {
                    findNavController().navigate(LoginFragmentDirections.toSettingsFragment())
                } else {
                    requireContext().toast(getString(R.string.error_wrong_code))
                }
            }
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
                requireContext().toast(requireActivity().getString(R.string.message_timed_out))
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
    }

    private fun checkAlreadyLoggedIn() {
        if (prefs.customPrefs()[getString(R.string.key_logged_user), ""].isNullOrEmpty() || prefs.customPrefs()[getString(
                R.string.key_registers
            ), ""].isNullOrEmpty()
        ) {
            loginBinding.clBase.visibility = View.VISIBLE
            return
        }
        loginViewModel.sendFilterProcessOpenMessage()
        val receivesNotifications =
            prefs.customPrefs()[getString(R.string.key_receive_notifications), true]
                ?: true
        if (receivesNotifications) {
            startFilterReceiverService()
        }

        val store =
            StoreResult(_storeName = prefs.customPrefs()[getString(R.string.key_store_name), ""]
                ?: "",
                _storeNumber = prefs.customPrefs()[getString(R.string.key_store_number), -1] ?: -1,
                _registers = prefs.customPrefs()[getString(R.string.key_registers), ""]?.split(",")
                    ?.map { RegisterResult(_registerNumber = it) } ?: listOf())
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(loginBroadcastReceiver)
        findNavController().navigate(LoginFragmentDirections.toRegistersFragment(store = store))
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
        startLoginResultReceiverService()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            loginBroadcastReceiver,
            IntentFilter(LoginResultReceiverService.ACTION_RECEIVE_LOGIN)
        )
        var valid = true
        if (loginBinding.etUser.text!!.isEmpty()) {
            valid = false
            loginBinding.tilUser.error = getString(R.string.error_empty_user)
        }
        if (loginBinding.etPassword.text!!.isEmpty()) {
            valid = false
            loginBinding.tilPassword.error = getString(R.string.error_empty_password)
        }
        if (valid) {
            showLoading()
            loginViewModel.login(
                loginBinding.etUser.text!!.toString(),
                loginBinding.etPassword.text!!.toString()
            )

            loginCountdownTimer?.start()
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }

    private fun startFilterReceiverService() {
        FilterReceiverService.enqueueWork(requireContext())
    }

    override fun onDetach() {
        super.onDetach()
        stopLoginService()
        loginCountdownTimer?.cancel()

    }

    private fun stopLoginService() {
        val intent = Intent(requireContext(), LoginResultReceiverService::class.java)
        requireContext().stopService(intent)
    }

    companion object {
        const val LOGIN_MAX_WAIT_MILLIS = 5000L
    }
}

