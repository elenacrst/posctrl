package `is`.posctrl.posctrl_android.service

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
import androidx.core.content.ContextCompat.startForegroundService
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.*
import timber.log.Timber


class ALifeSenderService : Service() {

    private var isServiceStarted = false
    lateinit var prefs: PreferencesSource
    lateinit var appContext: Context
    lateinit var repository: PosCtrlRepository

    private fun initialize() {
        prefs = PreferencesSource(applicationContext)
        appContext = applicationContext
        val xmlMapper = XmlMapper(
                JacksonXmlModule().apply { setDefaultUseWrapper(false) }
        )
        repository = PosCtrlRepository(prefs, appContext, xmlMapper)
    }

    override fun onCreate() {
        super.onCreate()
        initialize()
        Timber.d("The service has been created")
        val notification = createNotification()
        startForeground(1, notification)
        LAUNCHER.onServiceCreated(this)
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
            when (intent.action) {
                Actions.START.name -> startService()
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


        GlobalScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.IO) {
                prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)] = true
                while (true) {
                    delay(PosCtrlRepository.ALIFE_FILTER_DELAY_SECONDS * 1000L)
                    val sendAlife: Boolean =
                            prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)]
                                    ?: true
                    if (!sendAlife) {
                        LAUNCHER.stopService(appContext)
                        break
                    } else {
                        repository.sendFilterProcessALife()
                    }
                }
            }
        }
    }

    companion object {
        private val LAUNCHER = ForegroundServiceLauncher(ALifeSenderService::class.java)

        @JvmStatic
        fun stop(context: Context) = LAUNCHER.stopService(context)

        fun enqueueWork(context: Context, action: String) {
            if (action == FilterReceiverService.Actions.START.name) {
                Intent(context, ALifeSenderService::class.java).also {
                    it.action = Actions.START.name
                    LAUNCHER.onStartService()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(context, it)
                        return
                    }
                    context.startService(it)
                }
            } else if (action == Actions.STOP.name) {
                stop(context)
            }
        }
    }

    enum class Actions {
        START, STOP
    }

}
