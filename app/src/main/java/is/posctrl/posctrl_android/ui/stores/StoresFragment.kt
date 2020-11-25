package `is`.posctrl.posctrl_android.ui.stores

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentStoresBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

class StoresFragment : BaseFragment() {

    private lateinit var storesBinding: FragmentStoresBinding
    private lateinit var adapter: StoresAdapter
    private var stores = arrayOf<StoreResult>()

    @Inject
    lateinit var prefs: PreferencesSource

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        storesBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_stores, container, false)

        val args =
                StoresFragmentArgs.fromBundle(
                        requireArguments()
                )
        stores = args.stores

        return storesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StoresAdapter(StoreCellListener { store ->
            prefs.customPrefs()[getString(R.string.key_store_number)] = store.storeNumber
            prefs.customPrefs()[getString(R.string.key_store_name)] = store.storeName
            findNavController().navigate(StoresFragmentDirections.toRegistersFragment(store))
        })
        storesBinding.rvStores.adapter = adapter
        adapter.setData(stores)

        storesBinding.llBase.setOnSwipeListener(onSwipeLeft = {
            findNavController().navigate(NavigationMainContainerDirections.toAppOptionsFragment(null, null))
        })
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

