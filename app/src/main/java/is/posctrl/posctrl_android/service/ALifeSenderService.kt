package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.PRIORITY_HIGH
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat.startForegroundService
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject


class ALifeSenderService : Service() {

    private var isServiceStarted = false
    private var wakeLock: PowerManager.WakeLock? = null

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var appContext: Application

    @Inject
    lateinit var repository: PosCtrlRepository

    override fun onCreate() {
        super.onCreate()
        (applicationContext as PosCtrlApplication).appComponent.inject(this)
        Timber.d("The service has been created")
        val notification = createNotification()
        startForeground(1, notification)
    }

    @Suppress("DEPRECATION")
    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                    notificationChannelId,
                    "Send filter ALife channel",
                    NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Filter ALife sender service channel"
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder: Notification.Builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                        this,
                        notificationChannelId
                ) else Notification.Builder(this)

        return builder
                .setContentTitle("Sending filter ALife messages")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_HIGH) // for under android 26 compatibility
                .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            Timber.d("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> Timber.d("This should never happen. No action in the received intent")
            }
        } else {
            Timber.d(
                    "with a null intent. It has been probably restarted by the system."
            )
        }
        return START_REDELIVER_INTENT
    }

    @SuppressLint("WakelockTimeout")
    private fun startService() {
        if (isServiceStarted) return
        Timber.d("Starting the foreground service task")
        isServiceStarted = true



        prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)] = true
        sendAlife()


    }

    private fun sendAlife() {
        val req = GlobalScope.launch {
            withContext(Dispatchers.IO) {
                delay(PosCtrlRepository.ALIFE_FILTER_DELAY_SECONDS * 1000L)
            }
        }
        GlobalScope.launch {
            val sendAlife: Boolean =
                    prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)]
                            ?: true
            withContext(Dispatchers.IO) {
                req.join()

                if (sendAlife) {
                    repository.sendFilterProcessALife()
                }
            }
            if (!sendAlife) {
                stopService()
            } else {
                sendAlife()
            }
        }

    }

    private fun stopService() {
        Timber.d("Stopping the foreground service")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.d("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }

    companion object {
        fun enqueueWork(context: Context) {
            Intent(context, ALifeSenderService::class.java).also {
                it.action = Actions.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Timber.d("Starting the service in >=26 Mode")
                    startForegroundService(context, it)
                    return
                }
                Timber.d("Starting the service in < 26 Mode")
                context.startService(it)
            }
        }
    }

    enum class Actions {
        START, STOP
    }

}
