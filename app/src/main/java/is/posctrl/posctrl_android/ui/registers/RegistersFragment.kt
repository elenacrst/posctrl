package `is`.posctrl.posctrl_android.ui.registers

import `is`.posctrl.posctrl_android.ui.base.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentRegistersBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.ui.base.GlobalViewModel
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsFragment
import `is`.posctrl.posctrl_android.util.extensions.getWifiLevel
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


//todo show semitransparent overlay with the progress bar whenever loading
class RegistersFragment : BaseFragment() {

    private lateinit var registersBinding: FragmentRegistersBinding
    private lateinit var adapter: RegistersAdapter

    @Inject
    lateinit var prefs: PreferencesSource

    private lateinit var store: StoreResult

    private val globalViewModel: GlobalViewModel by activityViewModels()
    private var batteryCheckTimer: CountDownTimer = createBatteryCheckTimer()

    private fun createBatteryCheckTimer() =
            object : CountDownTimer(TimeUnit.MINUTES.toMillis(LoginFragment.BATTERY_CHECK_INTERVAL_MINUTES), 1000) {
                override fun onFinish() {
                    startBatteryTimer()
                }

                override fun onTick(millisUntilFinished: Long) {
                }
            }

    override fun onResume() {
        super.onResume()
        hideLoading()
    }

    private fun startBatteryTimer() {
        batteryCheckTimer.start()
        val bm = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        Timber.d("battery level is $batLevel")
        when (batLevel) {
            in 20..39 -> {
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            in 40..59 -> {
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_2, null), null, null, null)
            }
            in 60..79 -> {
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_3, null), null, null, null)
            }
            in 80..99 -> {
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_4, null), null, null, null)
            }
            100 -> {
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_5, null), null, null, null)
            }
            else -> {
                //0-19
                registersBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_0, null), null, null, null)
            }
        }
        registersBinding.tvBattery.text = batLevel.toString()
        registersBinding.tvBattery.append("%")
    }

    override fun onDetach() {
        super.onDetach()
        batteryCheckTimer.cancel()
    }

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
            val columns = prefs.customPrefs()[getString(R.string.key_registers_columns), -1] ?: -1
            if (columns in 1..4) {
                registersBinding.rvRegisters.layoutManager =
                        GridLayoutManager(requireContext(), columns)
            } else {
                registersBinding.rvRegisters.layoutManager =
                        GridLayoutManager(requireContext(), DEFAULT_COLUMN_COUNT)
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

        adapter = RegistersAdapter(requireContext(), RegisterCellListener { register ->
            startReceiptReceiverService()
            findNavController().navigate(
                    RegistersFragmentDirections.toReceiptFragment(
                            register,
                            store
                    )
            )
        }, false)
        registersBinding.rvRegisters.adapter = adapter
        adapter.setData(arrayOf())
        registersBinding.store = store

        registersBinding.clBase.setOnSwipeListener(onSwipeLeft = {
            findNavController().navigate(
                    NavigationMainContainerDirections.toAppOptionsFragment(
                            arrayOf(AppOptionsFragment.AppOptions.APP_VERSION.name, AppOptionsFragment.AppOptions.USER_INFO.name,
                                    AppOptionsFragment.AppOptions.LOGOUT.name, AppOptionsFragment.AppOptions.SUSPEND_REGISTER.name,
                                    AppOptionsFragment.AppOptions.STORE_INFO.name),
                            null,
                            store
                    )
            )
        })
        handleRegisters(store.registers)
        setupTexts()
        globalViewModel.wifiSignal.observe(viewLifecycleOwner, createWifiObserver())
        globalViewModel.setWifiSignal(requireContext().getWifiLevel())
        startBatteryTimer()
    }

    private fun createWifiObserver(): Observer<Int> {
        return Observer {
            when (it) {
                0 -> {
                    registersBinding.ivWifi.setImageResource(R.drawable.ic_wifi_1)
                }
                1 -> {
                    registersBinding.ivWifi.setImageResource(R.drawable.ic_wifi_2)
                }
                2 -> {
                    registersBinding.ivWifi.setImageResource(R.drawable.ic_wifi_3)
                }
                3 -> {
                    registersBinding.ivWifi.setImageResource(R.drawable.ic_wifi_4)
                }
                4 -> {
                    registersBinding.ivWifi.setImageResource(R.drawable.ic_wifi_5)
                }
            }
        }
    }

    private fun setupTexts() {
        var emptyText = prefs.defaultPrefs()["error_registers_store", getString(
                R.string.error_registers_store,
                store.storeNumber,
                store.storeName
        )]
                ?: getString(R.string.error_registers_store, store.storeNumber, store.storeName)
        emptyText = emptyText.replace("%1\$d", store.storeNumber.toString())
        emptyText = emptyText.replace("%2\$s", store.storeName)
        registersBinding.tvEmptyView.text = emptyText
        registersBinding.tvTitle.text =
                prefs.defaultPrefs()["title_select_register", getString(R.string.title_select_register)]
                        ?: getString(R.string.title_select_register)
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

    companion object {
        const val DEFAULT_COLUMN_COUNT = 3
    }
}

