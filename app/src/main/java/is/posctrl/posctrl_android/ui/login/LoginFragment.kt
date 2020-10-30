package `is`.posctrl.posctrl_android.ui.login

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.databinding.FragmentLoginBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import `is`.posctrl.posctrl_android.util.extensions.showInputDialog
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

class LoginFragment : BaseFragment() {

    private lateinit var loginBinding: FragmentLoginBinding

    @Inject
    lateinit var loginViewModel: LoginViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private fun getLoginObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(
                successListener = {
                    loginViewModel.loginResponse.value?.let {
                        prefs.customPrefs()[getString(R.string.key_server_path)] = it.serverPath
                        prefs.customPrefs()[getString(R.string.key_server_port)] = it.serverPort
                        prefs.customPrefs()[getString(R.string.key_filter_respond_time)] = it.filterRespondTime
                        prefs.customPrefs()[getString(R.string.key_version)] = it.appVersion
                        prefs.customPrefs()[getString(R.string.key_server_user)] = it.serverUser
                        prefs.customPrefs()[getString(R.string.key_server_domain)] = it.serverUserDomain
                        prefs.customPrefs()[getString(R.string.key_server_password)] = it.serverUserPassword
                        prefs.customPrefs()[getString(R.string.key_server_snapshot_path)] = it.serverSnapshotPath
                        prefs.customPrefs()[getString(R.string.key_logged_user)] = loginBinding.etUser.text.toString()
                    }
                    loginViewModel.getStores(
                            prefs.customPrefs()[getString(R.string.key_database_server)] ?: "",
                            prefs.customPrefs()[getString(R.string.key_database_port)] ?: "",
                            prefs.customPrefs()[getString(R.string.key_database_user)] ?: "",
                            prefs.customPrefs()[getString(R.string.key_database_password)] ?: "",
                            prefs.customPrefs()[getString(R.string.key_logged_user)] ?: ""
                    )
                }
        )
    }

    private fun getStoresObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(
                successListener = {
                    hideLoading()
                    loginViewModel.stores.value?.let {
                        if (it.size > 1) {
                            findNavController().navigate(LoginFragmentDirections.toStoresFragment(it.toTypedArray()))
                        } else {
                            prefs.customPrefs()[getString(R.string.key_store_number)] = it[0].storeNumber
                            prefs.customPrefs()[getString(R.string.key_store_name)] = it[0].storeName
                            requireContext().getString(R.string.message_store_value, it[0].storeNumber
                                    ?: -1, it[0].storeName)
                            findNavController().navigate(LoginFragmentDirections.toRegistersFragment(it[0]))
                        }
                    }
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
        loginViewModel.storesEvent.observe(viewLifecycleOwner, getStoresObserver())

        loginBinding.clBase.setOnSwipeListener(onSwipeBottom = {
            requireContext().showInputDialog(R.string.insert_security_code) {
                if (it == SECURITY_CODE) {
                    findNavController().navigate(LoginFragmentDirections.toDatabaseSettingsFragment())
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
            loginViewModel.login(
                    prefs.customPrefs()[getString(R.string.key_database_server)] ?: "",
                    prefs.customPrefs()[getString(R.string.key_database_port)] ?: "",
                    prefs.customPrefs()[getString(R.string.key_database_user)] ?: "",
                    prefs.customPrefs()[getString(R.string.key_database_password)] ?: "",
                    loginBinding.etUser.text!!.toString(),
                    loginBinding.etPassword.text!!.toString()
            )
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

