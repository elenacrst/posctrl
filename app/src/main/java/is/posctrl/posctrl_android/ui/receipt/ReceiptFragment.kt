package `is`.posctrl.posctrl_android.ui.receipt

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.NavigationMainContainerDirections
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.FragmentReceiptBinding
import `is`.posctrl.posctrl_android.databinding.FragmentRegistersBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.service.UdpReceiverService
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.setOnSwipeListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import javax.inject.Inject


class ReceiptFragment : BaseFragment() {

    private lateinit var receiptBinding: FragmentReceiptBinding

    @Inject
    lateinit var receiptViewModel: ReceiptViewModel

   /* @Inject
    lateinit var prefs: PreferencesSource*/

    private lateinit var store: StoreResponse
    private lateinit var register: RegisterResponse

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
        receiptViewModel.sendReceiptInfoMessage()

        return receiptBinding.root
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(ActivityModule(requireActivity())).inject(this)
        super.onAttach(context)
    }
}

