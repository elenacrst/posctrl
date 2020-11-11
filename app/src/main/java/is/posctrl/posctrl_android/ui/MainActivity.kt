package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity(), FilterHandler {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent
    private var broadcastReceiver = createFilterReceiver()

    private var filterItemMessages: List<FilteredInfoResponse> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.lifecycleOwner = this

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.storesFragment, R.id.registersFragment, R.id.receiptFragment -> {
                    navController.navigate(
                        NavigationMainContainerDirections.toFilterFragment(
                            filterItemMessages[0]
                        )
                    )
                    filterItemMessages = filterItemMessages - filterItemMessages[0]
                }
            }
        }

        initializeActivityComponent()
        activityComponent.inject(this)
    }

    private fun initializeActivityComponent() {
        activityComponent = (application as PosCtrlApplication).appComponent
            .activityComponent(ActivityModule(this))
    }

    fun showLoading() {
        mainBinding.pbLoading.visibility = View.VISIBLE
    }

    fun hideLoading() {
        mainBinding.pbLoading.visibility = View.GONE
    }

    private fun createFilterReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bundle = intent.extras
                if (bundle != null) {
                    val result =
                        bundle.getParcelable<FilteredInfoResponse>(FilterReceiverService.EXTRA_FILTER)
                    handleFilter(result)
                }
            }
        }
    }

    private fun handleFilter(result: FilteredInfoResponse?) {
        result?.let {
            filterItemMessages = filterItemMessages + result
            if (navController.currentDestination?.id in arrayOf(
                    R.id.storesFragment,
                    R.id.registersFragment,
                    R.id.receiptFragment
                )
            ) {
                navController.navigate(
                    NavigationMainContainerDirections.toFilterFragment(
                        filterItemMessages[0]
                    )
                )
                filterItemMessages = filterItemMessages - filterItemMessages[0]
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            broadcastReceiver,
            IntentFilter(FilterReceiverService.ACTION_RECEIVE_FILTER)
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun getFilters(): List<FilteredInfoResponse> {
        return filterItemMessages
    }
}

interface FilterHandler {
    fun getFilters(): List<FilteredInfoResponse>
}
