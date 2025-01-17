package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.ScreenOffAdminReceiver
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.ChargingService
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.registers.RegistersFragment
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.activitycontracts.InstallUnknownContract
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject


class MainActivity : BaseActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent
    private lateinit var navHostFragment: NavHostFragment

    private var startsOtherIntent: Boolean = false

    @Inject
    lateinit var preferencesSource: PreferencesSource

    private val installPackagesRequest = registerForActivityResult(InstallUnknownContract()) {}
    private var isActivityVisible: Boolean = false

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("created main")
        /*val lp = window.attributes
        lp.screenBrightness = 0.1f
        window.attributes = lp*/
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.lifecycleOwner = this

        setupNavController()
        initializeActivityComponent()
        activityComponent.inject(this)
        startService(Intent(baseContext, ChargingService::class.java))


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            installPackagesRequest.launch(InstallUnknownContract().createIntent(this, null))
        }
        globalViewModel.downloadApkEvent.observe(this, createDownloadObserver())

        setupAdmin()

    }

    override fun onResume() {
        super.onResume()
        val isActive = devicePolicyManager.isDeviceOwnerApp(packageName)//devicePolicyManager.isAdminActive(compName)

        if (!isActive) {
            toast("not device owner")
            /* val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
             intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
             intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Save battery life and unlock screen only when filter item notification is received")
             startActivityForResult(intent, RESULT_ENABLE)*/
        } else {
            devicePolicyManager.setLockTaskPackages(compName, arrayOf(packageName))

            if (devicePolicyManager.isLockTaskPermitted(packageName)) {
                startLockTask()
            } else {
                toast("not allowed to lock task")
                // Because the package isn't allowlisted, calling startLockTask() here
                // would put the activity into screen pinning mode.
            }

            //  devicePolicyManager.reboot(compName)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (RESULT_ENABLE == requestCode) {
            if (resultCode == RESULT_OK) {
                toast("admin enabled")
            } else {
                toast("admin not enabled")
            }
        }

    }

    private fun setupAdmin() {
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, ScreenOffAdminReceiver::class.java)
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true

    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
    }

    private fun setupNavController() {
        navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.registersFragment -> {
                    startsOtherIntent = false
                    Timber.d("registers nav")
                    val first = getFirstFilter()
                    if (first != null) {
                        navigateToFilter(first)
                    }
                }
                R.id.filterFragment -> {
                    startsOtherIntent = false
                }
                R.id.loginFragment -> {
                    globalViewModel.setShouldReceiveLoginResult(true)
                }
                else -> {
                    startsOtherIntent = false
                }
            }
        }
    }

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

    private fun navigateToFilter(filter: FilteredInfoResponse) {
        val wl = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP, "w:bbbb")
        wl.acquire()

        Timber.d("navigate to filter")
        val req = GlobalScope.launch {
            withContext(Dispatchers.Default) {
                delay(400)
            }

        }
        GlobalScope.launch {
            req.join()
            withContext(Dispatchers.Main) {
                navController.navigate(NavigationMainContainerDirections.toFilterFragment(filter))
            }
        }

    }

    override fun handleFilterElseLogin() {
        Timber.d("handle filter 1.5")
        if (startsOtherIntent || navController.currentDestination?.id == R.id.loginFragment || navController.currentDestination?.id == R.id.filterFragment) {
            if (navController.currentDestination?.id == R.id.loginFragment) {
                globalViewModel.setShouldReceiveLoginResult(true)
            }
            return
        }
        Timber.d("handle filter2 ")
        val filter = getFirstFilter()
        filter?.let {
            navigateToFilter(it)
        }
    }

    override fun onBackPressed() {
        val kiosk = preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true]
        if ((navHostFragment.childFragmentManager.fragments[0] is LoginFragment || navHostFragment.childFragmentManager.fragments[0] is RegistersFragment)
                && kiosk == true
        ) {
            return
        }
        super.onBackPressed()
    }

    private fun setupKiosk() {
        if (preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true] == true) {
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
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        globalViewModel.setShouldReceiveLoginResult(true)
        navController.navigate(NavigationMainContainerDirections.toLoginFragment())
        startActivity(openAppIntent)

        globalViewModel.clearFilterMessages()
    }

    companion object {
        const val RESULT_ENABLE = 11
    }
}

interface BaseFragmentHandler {
    fun showLoading()
    fun hideLoading()
    fun handleFilterElseLogin()
    fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit = { },
            errorListener: () -> Unit = { }
    ): Observer<Event<ResultWrapper<*>>>

    fun onDoubleTap()
    fun downloadApk(function: () -> Unit = {})
    fun startReceivingReceipt() {}
}
