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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun setupNavController() {
        navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.registersFragment -> {
                    val first = getFirstFilter()
                    if (first != null) {
                        navigateToFilter(first)
                    }
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
        startsOtherIntent = true
        val intent = Intent(this, FilterActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(FilterReceiverService.EXTRA_FILTER, filter)
        startActivityForResult(intent, RC_FILTER)//todo use activity result api
    }

    override fun handleFilter() {
        val filter = getFirstFilter()
        filter?.let {
            navigateToFilter(it)
        }
    }

    override fun onResume() {
        super.onResume()
        handleFilter()
    }

    override fun onBackPressed() {
        val kiosk = preferencesSource.defaultPrefs()[getString(R.string.key_kiosk_mode), true]
        Timber.d("kiosk main: $kiosk")
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
    fun handleFilter()
    fun createLoadingObserver(
            successListener: (ResultWrapper<*>?) -> Unit = { },
            errorListener: () -> Unit = { }
    ): Observer<Event<ResultWrapper<*>>>

    fun onDoubleTap()
    fun downloadApk(function: () -> Unit = {})
    fun startReceivingReceipt() {}
}
