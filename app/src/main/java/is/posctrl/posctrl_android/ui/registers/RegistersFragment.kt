package `is`.posctrl.posctrl_android.ui.registers

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentRegistersBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import javax.inject.Inject


//todo show semitransparent overlay with the progress bar whenever loading
class RegistersFragment : BaseFragment() {

    private lateinit var registersBinding: FragmentRegistersBinding
    private lateinit var adapter: RegistersAdapter

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

        val args = RegistersFragmentArgs.fromBundle(
            requireArguments()
        )
        store = args.store

        return registersBinding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RegistersAdapter(RegisterCellListener { register ->
            startReceiptReceiverService()
            findNavController().navigate(
                RegistersFragmentDirections.toReceiptFragment(
                    register,
                    store
                )
            )
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
        registersBinding.rvRegisters.setOnSwipeListener(onDoubleTap = {
            baseFragmentHandler?.onDoubleTap()
        })
        handleRegisters(store.registers)
    }

    private fun startReceiptReceiverService() {
        val intent = Intent(requireContext(), ReceiptReceiverService::class.java)
        ReceiptReceiverService.enqueueWork(requireContext(), intent)
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }
}

