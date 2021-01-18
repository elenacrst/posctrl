package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilterResults
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityFilterBinding
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.base.BaseFragment
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.*
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.view.*
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class FilterFragment : BaseFragment() {

    private var vibrationPauseTimer: CountDownTimer? = null
    private var secondVibrationPauseTimer: CountDownTimer? = null
    private var filterReactTimer: CountDownTimer? = null
    private lateinit var filterBinding: ActivityFilterBinding
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var filter: FilteredInfoResponse? = null

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var filterViewModel: FilterViewModel

    @Inject
    lateinit var picturesAdapter: SnapshotsAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        filterBinding = DataBindingUtil
                .inflate(inflater, R.layout.activity_filter, container, false)
        val args = FilterFragmentArgs.fromBundle(
                requireArguments()
        )
        filter = args.filter
        return filterBinding.root
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
                filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.ACCEPTED)
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
                this,  // LifecycleOwner
                callback
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter?.let {
            downloadFilterSnapshots(it)
        }
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
        filterReactTimer = createFilterReactTimer()

        setupTexts()
    }

    private fun setupTexts() {
        filterBinding.tvItemLabel.text =
                prefs.defaultPrefs()["label_item", getString(R.string.label_item)]
                        ?: getString(R.string.label_item)
        filterBinding.tvQuantityLabel.text =
                prefs.defaultPrefs()["label_quantity", getString(R.string.label_quantity)]
                        ?: getString(
                                R.string.label_quantity
                        )
        filterBinding.tvPriceLabel.text =
                prefs.defaultPrefs()["label_price", getString(R.string.label_price)]
                        ?: getString(R.string.label_price)
        filterBinding.btYes.text =
                prefs.defaultPrefs()["action_accept", getString(R.string.action_accept)]
                        ?: getString(R.string.action_accept)
        filterBinding.btNo.text =
                prefs.defaultPrefs()["action_reject", getString(R.string.action_reject)]
                        ?: getString(R.string.action_reject)
    }

    private fun createFilterReactTimer() = object : CountDownTimer(
            TimeUnit.SECONDS.toMillis(
                    (prefs.customPrefs()[getString(R.string.key_filter_respond_time), DEFAULT_FILTER_RESPOND_TIME_SECONDS]
                            ?: DEFAULT_FILTER_RESPOND_TIME_SECONDS).toLong()
            ),
            1000
    ) {
        override fun onFinish() {
            filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.TIMED_OUT)
            findNavController().navigateUp()
            requireActivity().toast(
                    prefs.defaultPrefs()["message_timed_out", getString(R.string.message_timed_out)]
                            ?: getString(R.string.message_timed_out)
            )
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }

    private fun downloadFilterSnapshots(it: FilteredInfoResponse) {
        val path = if (it.pictures?.isNullOrEmpty() == false) {
            it.pictures!![0].imageAddress
        } else {
            ""
        }
        Timber.d("path $path")
        filterViewModel.downloadBitmaps(path!!,
                it.pictures!!.map { picture -> picture.imageAddress!! }
        )

    }

    private fun initializeVibration() {
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrationPauseTimer = object : CountDownTimer(TimeUnit.SECONDS.toMillis(5), 1000) {
            override fun onFinish() {
                vibrator?.cancel()
                setupVibrationSoundOnceAWhile()
            }

            override fun onTick(millisUntilFinished: Long) {
                startVibrationIndefinitely()
            }
        }
        vibrationPauseTimer?.start()
    }

    private fun setupVibrationSoundOnceAWhile() {
        secondVibrationPauseTimer = object : CountDownTimer(TimeUnit.SECONDS.toMillis(5L), 1000) {
            override fun onFinish() {
                startVibrationOnce()
                if (prefs.customPrefs()[getString(R.string.key_notification_sound), true] == true) {
                    mediaPlayer?.start()
                }
                secondVibrationPauseTimer?.start()
            }

            override fun onTick(millisUntilFinished: Long) {

            }
        }
        secondVibrationPauseTimer?.start()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.dingaling)
        if (prefs.customPrefs()[getString(R.string.key_notification_sound), true] == true) {
            mediaPlayer?.start()
        }
    }

    private fun startVibrationIndefinitely() {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                                longArrayOf(
                                        200L,
                                        100L,
                                        200L,
                                        100L
                                ), -1
                        )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(200L, 100L, 200L, 100L), -1)
            }
        }
    }

    private fun startVibrationOnce() {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                                longArrayOf(
                                        200L,
                                        100L,
                                        200L,
                                        100L
                                ), -1
                        )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(200L, 100L, 200L, 100L), -1)
            }
        }
    }

    private fun getBitmapsLoadingObserver(): Observer<Event<ResultWrapper<*>>> {
        return (requireActivity() as BaseActivity).createLoadingObserver(successListener = {
            hideLoading()
            if (!filterViewModel.snapshotDownloadResult.value?.bitmaps.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.snapshotDownloadResult.value?.bitmaps?.toTypedArray())
            }
            if (filterViewModel.snapshotDownloadResult.value?.errors != 0) {
                requireActivity().toast(
                        prefs.defaultPrefs()["error_partial_download", getString(R.string.error_partial_download)]
                                ?: getString(R.string.error_partial_download)
                )
            }
            filterReactTimer?.start()
        }, errorListener = {
            if (!filterViewModel.snapshotDownloadResult.value?.bitmaps.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.snapshotDownloadResult.value?.bitmaps?.toTypedArray())
            }
            filterReactTimer?.start()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vibrator?.cancel()
        mediaPlayer?.release()
        vibrationPauseTimer?.cancel()
        filterReactTimer?.cancel()
        secondVibrationPauseTimer?.cancel()
        vibrator = null
        mediaPlayer = null
        vibrationPauseTimer = null
        filterReactTimer = null
        secondVibrationPauseTimer = null
    }

    companion object {
        const val DEFAULT_FILTER_RESPOND_TIME_SECONDS = 5
    }
}

