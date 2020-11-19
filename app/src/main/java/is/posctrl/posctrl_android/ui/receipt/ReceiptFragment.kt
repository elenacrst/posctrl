package `is`.posctrl.posctrl_android.ui.receipt

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.data.model.ReceiptResponse
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.FragmentReceiptBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.service.ReceiptReceiverService
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import javax.inject.Inject


class ReceiptFragment : BaseFragment() {

    private lateinit var receiptBinding: FragmentReceiptBinding

    @Inject
    lateinit var receiptViewModel: ReceiptViewModel

    @Inject
    lateinit var prefs: PreferencesSource

    private lateinit var store: StoreResponse
    private lateinit var register: RegisterResponse
    private var broadcastReceiver = createReceiptReceiver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        receiptBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_receipt, container, false)

        val args = ReceiptFragmentArgs.fromBundle(
            requireArguments()
        )
        store = args.store
        register = args.register
        receiptViewModel.sendReceiptInfoMessage(
            ReceiptAction.OPEN,
            (store.storeNumber ?: 0).toInt(),
            register.registerNumber ?: 0
        )
        receiptViewModel.sendReceiptInfoALife(
            (store.storeNumber ?: 0).toInt(),
            register.registerNumber ?: 0
        )

        return receiptBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiptBinding.svBase.setOnSwipeListener(onSwipeLeft = {
            findNavController().navigate(
                NavigationMainContainerDirections.toAppOptionsFragment(
                    register,
                    store
                )
            )
        })
        receiptBinding.tvTitle.text = getString(
            R.string.title_receipt_incomplete_values,
            store.storeNumber.toString() + " " + store.storeName,
            register.registerNumber ?: -1
        )
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }

    private fun createReceiptReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val bundle = intent.extras
                if (bundle != null) {
                    if (intent.action == FilterReceiverService.ACTION_RECEIVE_FILTER) {
                        val result =
                            bundle.getParcelable<FilteredInfoResponse>(FilterReceiverService.EXTRA_FILTER)
                        baseFragmentHandler?.handleFilter(result)
                    } else if (intent.action == ReceiptReceiverService.ACTION_RECEIVE_RECEIPT) {
                        val result =
                            bundle.getParcelable<ReceiptResponse>(ReceiptReceiverService.EXTRA_RECEIPT)
                        handleReceipt(result)
                    }

                }
            }
        }
    }

    private fun handleReceipt(result: ReceiptResponse?) {
        result?.let {
            if (it.storeNumber != store.storeNumber?.toInt() || register.registerNumber != it.registerNumber) {
                return@let
            }

            val lastTxn: Int = prefs.customPrefs()[getString(R.string.key_last_txn)] ?: -1

            if (lastTxn != it.clearTextFlag && lastTxn != -1) {
                if (receiptBinding.llReceipt.childCount > 1) {
                    receiptBinding.llReceipt.removeViews(1, receiptBinding.llReceipt.childCount - 1)
                }
                receiptBinding.tvTitle.text = getString(
                    R.string.title_receipt_values,
                    store.storeNumber.toString() + " " + store.storeName,
                    register.registerNumber ?: -1,
                    it.clearTextFlag.toString()
                )
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
        val intentFilter = IntentFilter(ReceiptReceiverService.ACTION_RECEIVE_RECEIPT)
        intentFilter.addAction(FilterReceiverService.ACTION_RECEIVE_FILTER)
        LocalBroadcastManager.getInstance(requireContext().applicationContext).registerReceiver(
            broadcastReceiver,
            intentFilter
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext().applicationContext)
            .unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        receiptViewModel.sendReceiptInfoMessage(
            ReceiptAction.CLOSE, (store.storeNumber
                ?: 0).toInt(),
            register.registerNumber ?: 0
        )
    }
}

