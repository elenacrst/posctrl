package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.PosCtrlRepository.Companion.DEFAULT_LOGIN_LISTENING_PORT
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.model.LoginResult
import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.PRIORITY_HIGH
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat.startForegroundService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import javax.inject.Inject


class LoginResultReceiverService : Service() {

    private var isServiceStarted = false
    private var wakeLock: PowerManager.WakeLock? = null

    @Inject
    lateinit var xmlMapper: XmlMapper

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var appContext: Application

    @Inject
    lateinit var repository: PosCtrlRepository

    private var socket: DatagramSocket? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun receiveUdp() {
        var p: DatagramPacket
        try {
            while (true) {
                val serverPort: Int = DEFAULT_LOGIN_LISTENING_PORT
                if (socket == null) {
                    socket = DatagramSocket(serverPort)
                    socket!!.broadcast = false
                }
                socket!!.reuseAddress = true
                socket!!.soTimeout = 60 * 1000
                Timber.d("waiting to receive login result via udp on port $serverPort")
                try {
                    val message = ByteArray(1024)
                    p = DatagramPacket(message, message.size)
                    socket!!.receive(p)
                    Timber.d("received login result ${String(message).substring(0, p.length)}")
                    publishResults(String(message).substring(0, p.length))
                } catch (e: SocketTimeoutException) {
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            closeSocket()
            e.printStackTrace()
        }
        closeSocket()
    }

    private fun closeSocket() {
        socket?.close()
    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext as PosCtrlApplication).appComponent.inject(this)
        Timber.d("The service has been created")
        val notification = createNotification()
        startForeground(1, notification)
    }

    @Suppress("DEPRECATION")
    private fun createNotification(): Notification {
        val notificationChannelId = "LOGIN_SERVICE_CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                    notificationChannelId,
                    "Login channel",
                    NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Login service channel"
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
                .setContentTitle("Waiting for login")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_HIGH) // for under android 26 compatibility
                .build()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun publishResults(output: String) {
        val result = xmlMapper.readValue(output, LoginResult::class.java)
        Timber.d("parsed login result $result")
        val intent = Intent(ACTION_RECEIVE_LOGIN)
        intent.putExtra(EXTRA_LOGIN, result)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
    }

    override fun onDestroy() {
        closeSocket()
        super.onDestroy()
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
        GlobalScope.launch(Dispatchers.Default) {
            receiveUdp()
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
        const val ACTION_RECEIVE_LOGIN = "is.posctrl.posctrl_android.RECEIVE_LOGIN"
        const val EXTRA_LOGIN = "LOGIN"

        fun enqueueWork(context: Context) {
            Intent(context, LoginResultReceiverService::class.java).also {
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
