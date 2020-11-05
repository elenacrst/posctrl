package `is`.posctrl.posctrl_android.ui.receipt

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.ReceiptResponse
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.FragmentReceiptBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.UdpReceiverService
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
        receiptViewModel.sendReceiptInfoALife((store.storeNumber ?: 0).toInt(),
                register.registerNumber ?: 0)

        return receiptBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiptBinding.svBase.setOnSwipeListener(onSwipeLeft = {
            findNavController().navigate(NavigationMainContainerDirections.toAppOptionsFragment(register))
        })
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
                    val result =
                            bundle.getParcelable<ReceiptResponse>(UdpReceiverService.EXTRA_RECEIPT)
                    handleReceipt(result)
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
            if (lastTxn != it.clearTextFlag && lastTxn != -1 || result.endFlag == 1) {
                receiptBinding.llReceipt.removeAllViews()
            }

            val values = it.line.split(Regex("\\s{2,}"))
            if (values.size == 3) {//alignment for rows of type name - qty - price
                val lineLayout = LinearLayout(requireContext())
                lineLayout.orientation = LinearLayout.HORIZONTAL
                lineLayout.weightSum = 4f
                val largeViewParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                largeViewParams.weight = 2f
                val smallerViewParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                smallerViewParams.weight = 1f
                val productNameText = generateFormattedTextView(it, values[0])
                val quantityText = generateFormattedTextView(it, values[1])
                val priceText = generateFormattedTextView(it, values[2])
                lineLayout.addView(productNameText, largeViewParams)
                lineLayout.addView(quantityText, smallerViewParams)
                lineLayout.addView(priceText, smallerViewParams)

                receiptBinding.llReceipt.addView(lineLayout)
            } else {
                receiptBinding.llReceipt.addView(generateFormattedTextView(it, it.line))
            }

            prefs.customPrefs()[getString(R.string.key_last_txn)] = it.clearTextFlag
        }
    }

    private fun generateFormattedTextView(it: ReceiptResponse, text: String): TextView {
        val textView = TextView(requireContext())
        when (it.color) {
            "Black" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.black
                        )
                )
            }
            "Blue" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_blue_dark
                        )
                )
            }
            "Gray" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.darker_gray
                        )
                )
            }
            "Green" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_green_dark
                        )
                )
            }
            "Orange" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_orange_dark
                        )
                )
            }
            "Purple" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_purple
                        )
                )
            }
            "Red" -> {
                textView.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                android.R.color.holo_red_dark
                        )
                )
            }
        }
        textView.text = text
        if (it.bold == 1) {
            if (it.italic == 1) {
                textView.setTypeface(textView.typeface, Typeface.BOLD_ITALIC)
            } else {
                textView.setTypeface(textView.typeface, Typeface.BOLD)
            }
        } else if (it.italic == 1) {
            textView.setTypeface(textView.typeface, Typeface.ITALIC)
        }
        return textView
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(
                broadcastReceiver,
                IntentFilter(UdpReceiverService.ACTION_RECEIVE_RECEIPT)
        )
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        receiptViewModel.sendReceiptInfoMessage(ReceiptAction.CLOSE, (store.storeNumber
                ?: 0).toInt(),
                register.registerNumber ?: 0)
    }
}

