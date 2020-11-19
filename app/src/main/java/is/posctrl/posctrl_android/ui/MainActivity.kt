package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber
import java.util.ArrayList


class MainActivity : AppCompatActivity(), BaseFragmentHandler {

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

        if (intent.hasExtra(EXTRA_FILTER_LIST) && intent.hasExtra(FilterReceiverService.EXTRA_FILTER)) {
            intent.getParcelableArrayListExtra<FilteredInfoResponse>(
                EXTRA_FILTER_LIST
            )?.let {
                filterItemMessages = it
            }
            val filter =
                intent.getParcelableExtra<FilteredInfoResponse>(FilterReceiverService.EXTRA_FILTER)
            handleFilter(filter)
        }
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
                        navController.navigate(
                            NavigationMainContainerDirections.toFilterFragment(
                                filterItemMessages[0]
                            )
                        )
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

    private fun createFilterReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!mainReceiverDisabled) {
                    val bundle = intent.extras
                    Timber.d("received filter 1")
                    if (bundle != null) {
                        val result =
                            bundle.getParcelable<FilteredInfoResponse>(FilterReceiverService.EXTRA_FILTER)
                        result?.let {
                            unlockScreenNavigateToFilter(it)
                        }
                    }

                }
            }
        }
    }

    private fun unlockScreenNavigateToFilter(filter: FilteredInfoResponse) {
        val intent = Intent(this, MainActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(FilterReceiverService.EXTRA_FILTER, filter)
        intent.putParcelableArrayListExtra(EXTRA_FILTER_LIST, ArrayList(filterItemMessages))
        startActivity(intent)
    }

    override fun handleFilter(result: FilteredInfoResponse?) {
        result?.let {
            filterItemMessages = filterItemMessages + result
            navController.navigate(
                NavigationMainContainerDirections.toFilterFragment(
                    filterItemMessages[0]
                )
            )
            filterItemMessages = filterItemMessages - filterItemMessages[0]
        }
    }

    companion object {
        const val EXTRA_FILTER_LIST = "FILTER_LIST"
    }
}

interface BaseFragmentHandler {
    fun showLoading()
    fun hideLoading()
    fun handleFilter(result: FilteredInfoResponse?)
}
