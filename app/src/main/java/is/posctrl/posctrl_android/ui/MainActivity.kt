package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.ui.filter.FilterActivity
import `is`.posctrl.posctrl_android.util.Event
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber


class MainActivity : BaseActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent
    private var broadcastReceiver = createFilterReceiver()

    private var filterItemMessages: List<FilteredInfoResponse> = mutableListOf()

    private var mainReceiverDisabled = false

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
    }

    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.storesFragment, R.id.registersFragment -> {
                    mainReceiverDisabled = false

                    if (filterItemMessages.isNotEmpty()) {
                        navigateToFilter(filterItemMessages[0])
                        filterItemMessages = filterItemMessages - filterItemMessages[0]
                    }
                }
                R.id.receiptFragment -> {
                    mainReceiverDisabled = true
                }
                else -> {
                    mainReceiverDisabled = false
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
                if (!mainReceiverDisabled) {
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
    }

    private fun navigateToFilter(filter: FilteredInfoResponse) {
        val intent = Intent(this, FilterActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(FilterReceiverService.EXTRA_FILTER, filter)
        startActivity(intent)
    }

    override fun handleFilter(result: FilteredInfoResponse?) {
        result?.let {
            filterItemMessages = filterItemMessages + result
            filterItemMessages = filterItemMessages - filterItemMessages[0]
            navigateToFilter(it)
        }
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
}
