package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.BaseFragment
import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilterResults
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.FragmentFilterBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class FilterFragment : BaseFragment() {

    private lateinit var vibrationPauseTimer: CountDownTimer
    private lateinit var filterReactTimer: CountDownTimer
    private lateinit var filterBinding: FragmentFilterBinding
    private lateinit var vibrator: Vibrator
    private lateinit var mediaPlayer: MediaPlayer
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

        initializeVibration()
        initializeMediaPlayer()

        filterBinding.btYes.setOnClickListener {
            filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.ACCEPTED)
            findNavController().navigateUp()
        }
        filterBinding.btNo.setOnClickListener {
            filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.REJECTED)
            findNavController().navigateUp()
        }
        filterReactTimer = object : CountDownTimer(
            TimeUnit.SECONDS.toMillis(
                (prefs.customPrefs()[getString(R.string.key_filter_respond_time), DEFAULT_FILTER_RESPOND_TIME_SECONDS]
                    ?: DEFAULT_FILTER_RESPOND_TIME_SECONDS).toLong()
            ),
            1000
        ) {
            override fun onFinish() {
                filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.TIMED_OUT)
                findNavController().navigateUp()
                requireContext().toast(getString(R.string.message_timed_out))
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
    }

    private fun initializeVibration() {
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        startVibration()
        vibrationPauseTimer = object : CountDownTimer(TimeUnit.SECONDS.toMillis(16), 1000) {
            override fun onFinish() {
                startVibration()
                vibrationPauseTimer.start()
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.dingaling)
        if (prefs.customPrefs()[getString(R.string.key_notification_sound), true] == true) {
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.start()
            }
        }
    }

    private fun startVibration() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 200L, 500L), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 200L, 500L), 0)
            }
        }
    }

    private fun getBitmapsLoadingObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(successListener = {
            hideLoading()
            if (!filterViewModel.bitmaps.value.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.bitmaps.value?.toTypedArray())
            }
            filterReactTimer.start()
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

        val args = FilterFragmentArgs.fromBundle(requireArguments())
        filter = args.filter
        filter?.let {
            val path = if (it.pictures.isNotEmpty()) {
                it.pictures[0].imageAddress
            } else {
                ""
            }
            Timber.d("path $path")
            filterViewModel.downloadBitmaps(path,
                it.pictures.map { picture -> picture.imageAddress.split('\\').last() })
        }

        return filterBinding.root
    }

    override fun onDetach() {
        super.onDetach()
        vibrator.cancel()
        mediaPlayer.release()
        vibrationPauseTimer.cancel()
        filterReactTimer.cancel()
    }

    companion object {
        const val DEFAULT_FILTER_RESPOND_TIME_SECONDS = 5
    }
}

