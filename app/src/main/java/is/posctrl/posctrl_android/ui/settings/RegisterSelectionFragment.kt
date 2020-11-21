package `is`.posctrl.posctrl_android.ui.settings

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.FragmentRegistersBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.registers.*
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsViewModel
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import `is`.posctrl.posctrl_android.util.extensions.showConfirmDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import javax.inject.Inject


//todo show semitransparent overlay with the progress bar whenever loading
class RegisterSelectionFragment : BaseFragment() {

    private lateinit var registersBinding: FragmentRegistersBinding
    private lateinit var adapter: RegistersAdapter

    @Inject
    lateinit var registersViewModel: RegistersViewModel

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private lateinit var store: StoreResponse

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        registersBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_registers, container, false)

        val args = RegistersFragmentArgs.fromBundle(
            requireArguments()
        )
        store = args.store
        registersViewModel.getRegisters(
            prefs.defaultPrefs()[getString(R.string.key_database_server)] ?: "",
            prefs.defaultPrefs()[getString(R.string.key_database_port)] ?: "",
            prefs.defaultPrefs()[getString(R.string.key_database_user)] ?: "",
            prefs.defaultPrefs()[getString(R.string.key_database_password)] ?: "",
            store.storeNumber!!.toInt(),
            prefs.customPrefs()[getString(R.string.key_logged_user)] ?: ""
        )
        registersBinding.tvTitle.text = getString(R.string.title_select_suspend)

        return registersBinding.root
    }

    private fun getRegistersObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(
            successListener = {
                hideLoading()
                registersViewModel.registers.value?.let {
                    if (it.isNotEmpty()) {
                        adapter.setData(it.toTypedArray())
                        registersBinding.registers.visibility = View.VISIBLE
                        registersBinding.tvEmptyView.visibility = View.GONE
                    } else {
                        registersBinding.registers.visibility = View.GONE
                        registersBinding.tvEmptyView.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registersViewModel.registersEvent.observe(viewLifecycleOwner, getRegistersObserver())

        adapter = RegistersAdapter(RegisterCellListener { register ->
            requireContext().showConfirmDialog(
                getString(
                    R.string.confirm_suspend_register,
                    register.registerNumber,
                    store.storeNumber
                )
            ) {
                appOptionsViewModel.suspendRegister(
                    store.storeNumber?.toInt()
                        ?: -1, register.registerNumber ?: -1
                )
                findNavController().navigateUp()
            }
        })
        registersBinding.rvRegisters.adapter = adapter
        adapter.setData(arrayOf())
        registersBinding.store = store

        registersBinding.clBase.setOnSwipeListener(onSwipeLeft = {
            findNavController().navigate(
                NavigationMainContainerDirections.toAppOptionsFragment(
                    null,
                    store
                )
            )
        })
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }
}

