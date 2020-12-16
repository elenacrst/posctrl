package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.ChargingService
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.filter.FilterActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.registers.RegistersFragment
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.activitycontracts.InstallUnknownContract
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber
import javax.inject.Inject


class MainActivity : BaseActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent
    private var broadcastReceiver = createFilterReceiver()

    private var filterItemMessages: List<FilteredInfoResponse> = mutableListOf()

    private lateinit var navHostFragment: NavHostFragment

    private var startsOtherIntent: Boolean = false

    @Inject
    lateinit var preferencesSource: PreferencesSource

    private val installPackagesRequest = registerForActivityResult(InstallUnknownContract()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allowScreenUnlock()
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.lifecycleOwner = this

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
                broadcastReceiver,
                IntentFilter(FilterReceiverService.ACTION_RECEIVE_FILTER)
        )

        setupNavController()
        initializeActivityComponent()
        activityComponent.inject(this)
        startService(Intent(baseContext, ChargingService::class.java))


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            installPackagesRequest.launch(InstallUnknownContract().createIntent(this, null))
        }
        globalViewModel.downloadApkEvent.observe(this, createDownloadObserver())
    }

    private fun setupNavController() {
        navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.registersFragment -> {
                    if (filterItemMessages.isNotEmpty()) {
                        navigateToFilter(filterItemMessages[0])
                        filterItemMessages = filterItemMessages - filterItemMessages[0]
                    }
                }
                R.id.receiptFragment -> {
                    startReceivingReceipt()
                }
                else -> {
                }
            }
        }
    }

    private fun allowScreenUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            this.window.addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }//todo move to base activity

    private fun initializeActivityComponent() {
        activityComponent = (application as PosCtrlApplication).appComponent
                .activityComponent(ActivityModule(this))
    }

    override fun showLoading() {
        mainBinding.pbLoading.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        mainBinding.pbLoading.visibility = View.GONE
    }

    private fun createFilterReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bundle = intent.extras
                Timber.d("received filter 1")
                if (bundle != null) {
                    val result =
                            bundle.getParcelable<FilteredInfoResponse>(FilterReceiverService.EXTRA_FILTER)
                    handleFilter(result)
                }
            }
        }
    }

    private fun navigateToFilter(filter: FilteredInfoResponse) {
        startsOtherIntent = true
        val intent = Intent(this, FilterActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(FilterReceiverService.EXTRA_FILTER, filter)
        startActivityForResult(intent, RC_FILTER)//todo use activity result api
    }

    override fun handleFilter(result: FilteredInfoResponse?) {
        result?.let {
            filterItemMessages = filterItemMessages + result
            filterItemMessages = filterItemMessages - filterItemMessages[0]
            navigateToFilter(it)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                (navHostFragment.childFragmentManager.fragments[0] is LoginFragment || navHostFragment.childFragmentManager.fragments[0] is RegistersFragment)
                && preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode), true] == true
        ) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        if ((navHostFragment.childFragmentManager.fragments[0] is LoginFragment || navHostFragment.childFragmentManager.fragments[0] is RegistersFragment)
                && preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode), true] == true
        ) {
            return
        }
        super.onBackPressed()
    }

    private fun setupKiosk() {
        if (preferencesSource.customPrefs()[getString(R.string.key_kiosk_mode), true] == true) {
            val activityManager = applicationContext
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        setupKiosk()
    }

    override fun handleLogout() {
        startsOtherIntent = true
        val openAppIntent = Intent(this, MainActivity::class.java)
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(openAppIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == RC_FILTER && resultCode == RESULT_LOGOUT) {
            handleLogout()
        }
    }

    companion object {
        const val RC_FILTER = 1
        const val RESULT_LOGOUT = 10
    }
}

interface BaseFragmentHandler {
    fun showLoading()
    fun hideLoading()
    fun handleFilter(result: FilteredInfoResponse?)
    fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit = { },
            errorListener: () -> Unit = { }
    ): Observer<Event<ResultWrapper<*>>>

    fun onDoubleTap()
    fun downloadApk(function: () -> Unit = {})
    fun startReceivingReceipt() {}
}
