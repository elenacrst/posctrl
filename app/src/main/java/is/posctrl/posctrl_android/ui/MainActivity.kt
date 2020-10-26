package `is`.posctrl.posctrl_android.ui

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.databinding.ActivityMainBinding
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainBinding.lifecycleOwner = this

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
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

}
