package `is`.posctrl.posctrl_android.ui.filter

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilterResults
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.databinding.ActivityFilterBinding
import `is`.posctrl.posctrl_android.service.FilterReceiverService
import `is`.posctrl.posctrl_android.ui.BaseActivity
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class FilterActivity : BaseActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allowScreenUnlock()
        filterBinding = DataBindingUtil.setContentView(this, R.layout.activity_filter)

        filter = intent.getParcelableExtra(FilterReceiverService.EXTRA_FILTER)
        filter?.let {
            downloadFilterSnapshots(it)
        }
        filterViewModel.bitmapsEvent.observe(this, getBitmapsLoadingObserver())
        filterBinding.rvSnapshots.adapter = picturesAdapter
        filterBinding.filter = filter

        initializeVibration()
        initializeMediaPlayer()

        filterBinding.btYes.setOnClickListener {
            filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.ACCEPTED)
            finish()
        }
        filterBinding.btNo.setOnClickListener {
            filterViewModel.sendFilterMessage(filter?.itemLineId ?: -1, FilterResults.REJECTED)
            finish()
        }
        filterReactTimer = createFilterReactTimer()
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
            finish()
            toast(getString(R.string.message_timed_out))
        }

        override fun onTick(millisUntilFinished: Long) {
        }
    }

    private fun downloadFilterSnapshots(it: FilteredInfoResponse) {
        val path = if (it.pictures.isNotEmpty()) {
            it.pictures[0].imageAddress
        } else {
            ""
        }
        Timber.d("path $path")
        filterViewModel.downloadBitmaps(path,
                it.pictures.map { picture -> picture.imageAddress }
        )
    }

    private fun initializeVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
        mediaPlayer = MediaPlayer.create(this, R.raw.dingaling)
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
        return createLoadingObserver(successListener = {
            hideLoading()
            if (!filterViewModel.bitmaps.value?.bitmaps.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.bitmaps.value?.bitmaps?.toTypedArray())
            }
            if (filterViewModel.bitmaps.value?.errors != 0) {
                toast(getString(R.string.error_partial_download))
            }
            filterReactTimer?.start()
        }, errorListener = {
            if (!filterViewModel.bitmaps.value?.bitmaps.isNullOrEmpty()) {
                picturesAdapter.setData(filterViewModel.bitmaps.value?.bitmaps?.toTypedArray())
            }
            filterReactTimer?.start()
        })
    }

    private fun allowScreenUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            this.window.addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun showLoading() {
        filterBinding.pbLoading.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        filterBinding.pbLoading.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator?.cancel()
        mediaPlayer?.release()
        vibrationPauseTimer?.cancel()
        filterReactTimer?.cancel()
        secondVibrationPauseTimer?.cancel()
    }

    override fun handleLogout() {
        setResult(MainActivity.RESULT_LOGOUT)
        finish()
    }

    companion object {
        const val DEFAULT_FILTER_RESPOND_TIME_SECONDS = 5
    }
}