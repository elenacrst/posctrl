package `is`.posctrl.posctrl_android.ui.stores

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.databinding.FragmentStoresBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil

class StoresFragment : BaseFragment() {

    private lateinit var storesBinding: FragmentStoresBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        storesBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_stores, container, false)

        return storesBinding.root
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

