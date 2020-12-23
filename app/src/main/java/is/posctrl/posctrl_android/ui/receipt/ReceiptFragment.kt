package `is`.posctrl.posctrl_android.ui.receipt

import `is`.posctrl.posctrl_android.ui.base.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.ReceiptResponse
import `is`.posctrl.posctrl_android.data.model.RegisterResult
import `is`.posctrl.posctrl_android.data.model.StoreResult
import `is`.posctrl.posctrl_android.databinding.FragmentReceiptBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.base.GlobalViewModel
import `is`.posctrl.posctrl_android.ui.login.LoginFragment
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsViewModel
import `is`.posctrl.posctrl_android.util.extensions.getWifiLevel
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import `is`.posctrl.posctrl_android.util.extensions.showConfirmDialog
import android.content.Context
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ReceiptFragment : BaseFragment() {

    private lateinit var receiptBinding: FragmentReceiptBinding

    @Inject
    lateinit var receiptViewModel: ReceiptViewModel

    @Inject
    lateinit var appOptionsViewModel: AppOptionsViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private lateinit var store: StoreResult
    private lateinit var register: RegisterResult

    private val globalViewModel: GlobalViewModel by activityViewModels()
    private var shouldClearReceiptScreen: Boolean = false

    private var batteryCheckTimer: CountDownTimer = createBatteryCheckTimer()

    private fun createBatteryCheckTimer() =
            object : CountDownTimer(TimeUnit.MINUTES.toMillis(LoginFragment.BATTERY_CHECK_INTERVAL_MINUTES), 1000) {
                override fun onFinish() {
                    startBatteryTimer()
                }

                override fun onTick(millisUntilFinished: Long) {
                }
            }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        receiptBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_receipt, container, false)

        val args = ReceiptFragmentArgs.fromBundle(
                requireArguments()
        )
        store = args.store
        register = args.register
        receiptViewModel.sendReceiptInfoMessage(
                ReceiptAction.OPEN,
                store.storeNumber,
                register.registerNumber.toInt()
        )
        receiptViewModel.sendReceiptInfoALife(
                store.storeNumber,
                register.registerNumber.toInt()
        )

        return receiptBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiptBinding.svBase.setOnSwipeListener(onSwipeLeft = {
            var confirmText = prefs.defaultPrefs()["confirm_suspend_register", getString(
                    R.string.confirm_suspend_register,
                    register.registerNumber.toInt(),
                    store.storeNumber
            )] ?: getString(
                    R.string.confirm_suspend_register,
                    register.registerNumber.toInt(),
                    store.storeNumber
            )
            confirmText = confirmText.replace("%1\$d", register.registerNumber)
            confirmText = confirmText.replace("%2\$d", store.storeNumber.toString())
            requireContext().showConfirmDialog(confirmText) {
                appOptionsViewModel.suspendRegister(
                        store.storeNumber, register.registerNumber.toInt()
                )
            }
        })
        var incompleteValText = prefs.defaultPrefs()["title_receipt_incomplete_values",
                getString(
                        R.string.title_receipt_incomplete_values,
                        store.storeNumber,
                        register.registerNumber.toInt()
                )] ?: getString(
                R.string.title_receipt_incomplete_values,
                store.storeNumber,
                register.registerNumber.toInt()
        )
        incompleteValText = incompleteValText.replace("%1\$d", store.storeNumber.toString())
        incompleteValText = incompleteValText.replace("%2\$d", register.registerNumber)

        receiptBinding.tvTitle.text = incompleteValText

        globalViewModel.receiptItems.observe(viewLifecycleOwner, createReceiptItemsObserver())
        shouldClearReceiptScreen = true

        globalViewModel.wifiSignal.observe(viewLifecycleOwner, createWifiObserver())
        globalViewModel.setWifiSignal(requireContext().getWifiLevel())
    }

    private fun createWifiObserver(): Observer<Int> {
        return Observer {
            when (it) {
                0 -> {
                    receiptBinding.ivWifi.setImageResource(R.drawable.ic_wifi_1)
                }
                1 -> {
                    receiptBinding.ivWifi.setImageResource(R.drawable.ic_wifi_2)
                }
                2 -> {
                    receiptBinding.ivWifi.setImageResource(R.drawable.ic_wifi_3)
                }
                3 -> {
                    receiptBinding.ivWifi.setImageResource(R.drawable.ic_wifi_4)
                }
                4 -> {
                    receiptBinding.ivWifi.setImageResource(R.drawable.ic_wifi_5)
                }
            }
        }
    }

    private fun createReceiptItemsObserver(): Observer<List<ReceiptResponse>> {
        return Observer {
            if (!it.isNullOrEmpty()) {
                receiptBinding.llReceipt.removeAllViews()
                shouldClearReceiptScreen = false
                it.forEach { item ->
                    Timber.d("item ${item.line}")
                    handleReceipt(item)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
                ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
                true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                receiptViewModel.sendReceiptInfoMessage(
                        ReceiptAction.CLOSE, store.storeNumber,
                        register.registerNumber.toInt()
                )
                findNavController().navigateUp()
                globalViewModel.receiptItems.removeObservers(viewLifecycleOwner)
                globalViewModel.clearReceipt()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
                this,  // LifecycleOwner
                callback
        )
    }

    private fun handleReceipt(result: ReceiptResponse?) {
        result?.let {
            if (it.storeNumber != store.storeNumber || register.registerNumber.toInt() != it.registerNumber) {
                return@let
            }

            val lastTxn: Int = prefs.customPrefs()[getString(R.string.key_last_txn)] ?: -1

            if (lastTxn != it.clearTextFlag && lastTxn != -1) {
                receiptBinding.llReceipt.removeAllViews()
                var receiptValuesText = prefs.defaultPrefs()["title_receipt_values",
                        getString(
                                R.string.title_receipt_values,
                                store.storeNumber,
                                register.registerNumber.toInt(),
                                it.clearTextFlag
                        )] ?: getString(
                        R.string.title_receipt_values,
                        store.storeNumber,
                        register.registerNumber.toInt(),
                        it.clearTextFlag
                )
                receiptValuesText = receiptValuesText.replace("%1\$d", store.storeNumber.toString())
                receiptValuesText = receiptValuesText.replace("%2\$d", register.registerNumber)
                receiptValuesText = receiptValuesText.replace("%3\$d", it.clearTextFlag.toString())
                receiptBinding.tvTitle.text = receiptValuesText
            }

            val view = generateFormattedTextView(it, it.line)
            receiptBinding.llReceipt.addView(view)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()

            prefs.customPrefs()[getString(R.string.key_last_txn)] = it.clearTextFlag

        }
    }

    private fun generateFormattedTextView(it: ReceiptResponse, text: String): TextView {
        val textView = TextView(requireContext())
        val color = getCSharpColor(it.color)
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.courier_prime)


        textView.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        color
                )
        )
        textView.text = text
        if (it.bold == 1) {
            if (it.italic == 1) {
                textView.setTypeface(typeface, Typeface.BOLD_ITALIC)
            } else {
                textView.setTypeface(typeface, Typeface.BOLD)
            }
        } else if (it.italic == 1) {
            textView.setTypeface(typeface, Typeface.ITALIC)
        } else {
            textView.setTypeface(typeface, Typeface.NORMAL)
        }
        return textView
    }

    private fun getCSharpColor(color: String): Int {
        return when (color) {
            "Black" -> {
                android.R.color.black
            }
            "Blue" -> {
                android.R.color.holo_blue_dark
            }
            "Gray" -> {
                android.R.color.darker_gray
            }
            "Green" -> {
                android.R.color.holo_green_dark
            }
            "Orange" -> {
                android.R.color.holo_orange_dark
            }
            "Purple" -> {
                android.R.color.holo_purple
            }
            "Red" -> {
                android.R.color.holo_red_dark
            }
            else -> {
                android.R.color.black
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        startBatteryTimer()
    }

    private fun startBatteryTimer() {
        batteryCheckTimer.start()
        val bm = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        Timber.d("battery level is $batLevel")
        when (batLevel) {
            in 20..39 -> {
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            in 40..59 -> {
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            in 60..79 -> {
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            in 80..99 -> {
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            100 -> {
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
            else -> {
                //0-19
                receiptBinding.tvBattery.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources, R.drawable.ic_battery_1, null), null, null, null)
            }
        }
        receiptBinding.tvBattery.text = batLevel.toString()
        receiptBinding.tvBattery.append("%")
    }

    override fun onPause() {
        super.onPause()
        batteryCheckTimer.cancel()
    }
}

