package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.databinding.FragmentFilterBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.glide.load
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import javax.inject.Inject


class FilterFragment : BaseFragment() {

    private lateinit var filterBinding: FragmentFilterBinding

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var filterViewModel: FilterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filterViewModel.downloadBitmaps(arrayListOf("22-3-85-7-2.jpg", "22-3-85-7-1.jpg"))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterViewModel.bitmapsEvent.observe(viewLifecycleOwner, getBitmapsLoadingObserver())
    }

    private fun getBitmapsLoadingObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(successListener = {
            hideLoading()
            if (!filterViewModel.bitmaps.value.isNullOrEmpty()) {
                filterBinding.ivSnapshot.load(requireContext(), filterViewModel.bitmaps.value!![0])
            }
        })
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(
                requireActivity()
            )
        ).inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        filterBinding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_filter, container, false)

        return filterBinding.root
    }
}

