package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
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
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.registers.RegistersFragment
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.activitycontracts.InstallUnknownContract
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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
        Timber.d("navigate to filter")
        startsOtherIntent = true
        navController.navigate(NavigationMainContainerDirections.toFilterFragment(filter))
    }

    override fun handleFilterElseLogin() {
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
