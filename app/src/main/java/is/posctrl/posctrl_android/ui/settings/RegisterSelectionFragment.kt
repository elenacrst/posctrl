package `is`.posctrl.posctrl_android.ui.settings

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentRegistersBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.registers.*
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsViewModel
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import `is`.posctrl.posctrl_android.util.extensions.showConfirmDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import javax.inject.Inject


//todo show semitransparent overlay with the progress bar whenever loading
class RegisterSelectionFragment : BaseFragment() {

    private lateinit var registersBinding: FragmentRegistersBinding
    private lateinit var adapter: RegistersAdapter

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private lateinit var store: StoreResult

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        registersBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_registers, container, false)

        val args = RegisterSelectionFragmentArgs.fromBundle(
            requireArguments()
        )
        store = args.store
        registersBinding.tvTitle.text = getString(R.string.title_select_suspend)

        return registersBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RegistersAdapter(RegisterCellListener { register ->
            requireContext().showConfirmDialog(
                getString(
                    R.string.confirm_suspend_register,
                    register.registerNumber.toInt(),
                    store.storeNumber
                )
            ) {
                appOptionsViewModel.suspendRegister(
                    store.storeNumber, register.registerNumber.toInt()
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
        }, onDoubleTap = {
            baseFragmentHandler?.onDoubleTap()
        })

        handleRegisters(store.registers)
    }

    private fun handleRegisters(registers: List<RegisterResult>) {
        if (registers.isNotEmpty()) {
            if (registers.size > 6) {
                registersBinding.rvRegisters.layoutManager =
                    GridLayoutManager(requireContext(), 3)
            } else {
                registersBinding.rvRegisters.layoutManager =
                    GridLayoutManager(requireContext(), 2)
            }
            adapter.setData(registers.toTypedArray())
            registersBinding.registers.visibility = View.VISIBLE
            registersBinding.tvEmptyView.visibility = View.GONE
        } else {
            registersBinding.registers.visibility = View.GONE
            registersBinding.tvEmptyView.visibility = View.VISIBLE
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }
}

