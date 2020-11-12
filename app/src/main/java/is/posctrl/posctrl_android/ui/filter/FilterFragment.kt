package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.FragmentFilterBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.FilterHandler
import `is`.posctrl.posctrl_android.util.Event
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import javax.inject.Inject


class FilterFragment : BaseFragment() {

    private lateinit var filterBinding: FragmentFilterBinding
    private lateinit var vibrator: Vibrator
    private lateinit var mediaPlayer: MediaPlayer
    private var filterHandler: FilterHandler? = null
    private var filter: FilteredInfoResponse? = null

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var filterViewModel: FilterViewModel

    @Inject
    lateinit var picturesAdapter: SnapshotsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterViewModel.bitmapsEvent.observe(viewLifecycleOwner, getBitmapsLoadingObserver())

        filterBinding.rvSnapshots.adapter = picturesAdapter
        filterBinding.filter = filter

        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 100L, 400L), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 100L, 400L), 0)
            }
        }

        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.dingaling)
        if (prefs.customPrefs()[getString(R.string.key_notification_sound), true] as Boolean) {
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.start()
            }
        }
    }

    private fun getBitmapsLoadingObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(successListener = {
            hideLoading()
            if (!filterViewModel.bitmaps.value.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.bitmaps.value?.toTypedArray())
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
        filterHandler = context as? FilterHandler
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        filterBinding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_filter, container, false)

        val args = FilterFragmentArgs.fromBundle(requireArguments())
        filter = args.filter
        filter?.let {
            filterViewModel.downloadBitmaps(it.pictures.map { picture -> picture.imageAddress })
        } ?: run {
            filterViewModel.downloadBitmaps(
                    arrayListOf(
                            "22-3-85-7-2.jpg",
                            "22-3-85-7-1.jpg"
                    )
            )//todo remove after testing
        }

        return filterBinding.root
    }

    override fun onDetach() {
        super.onDetach()
        vibrator.cancel()
        mediaPlayer.release()
    }
}

