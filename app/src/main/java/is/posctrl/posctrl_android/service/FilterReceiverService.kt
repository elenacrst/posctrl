package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.PosCtrlRepository.Companion.DEFAULT_FILTER_PORT
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.FilterResults
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.PRIORITY_HIGH
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat.startForegroundService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException


class FilterReceiverService : Service() {

    private var isServiceStarted = false
    lateinit var xmlMapper: XmlMapper
    lateinit var prefs: PreferencesSource
    lateinit var appContext: Context
    lateinit var repository: PosCtrlRepository

    private var socket: DatagramSocket? = null
    private var message: ByteArray = ByteArray(1024)

    private fun initialize() {
        xmlMapper = XmlMapper(
                JacksonXmlModule().apply { setDefaultUseWrapper(false) }
        )
        prefs = PreferencesSource(applicationContext)
        appContext = applicationContext
        repository = PosCtrlRepository(prefs, appContext, xmlMapper)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun receiveUdp() {
        var p: DatagramPacket
        try {
            while (true) {
                val serverPort: Int =
                        prefs.customPrefs()[appContext.getString(R.string.key_filter_port), DEFAULT_FILTER_PORT]
                                ?: DEFAULT_FILTER_PORT
                if (socket == null) {
                    socket = DatagramSocket(serverPort)
                    socket!!.broadcast = false
                }
                socket!!.reuseAddress = true
                socket!!.soTimeout = 60 * 1000
                Timber.d("waiting to receive filter via udp on port $serverPort")
                try {
                    p = DatagramPacket(message, 1000)
                    socket!!.receive(p)
                    val msg = String(message).substring(0, p.length)
                    Timber.d("received filter $msg ${msg.length}")
                    publishResults(msg)
                    // publishResults("<FilteredInfo><ItemLineID>15862</ItemLineID><StoreNumber>22</StoreNumber><StoreName>Bónus Hraunbæ</StoreName><RegisterNumber>3</RegisterNumber><TxnNumber>7</TxnNumber><ItemID>73721</ItemID><ItemName>COTTAGE CHEESE 250G</ItemName><ItemSeqNumber>7</ItemSeqNumber><Quantity>1,00</Quantity><TotalPrice>1,10</TotalPrice><FilterName>Item Number</FilterName><FilterQuestion>Is this correct Item ?</FilterQuestion><SnapShot></SnapShot></FilteredInfo>")
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

    private suspend fun sendFilterResultReceived(itemLineId: Int) {
        repository.sendFilterResultMessage(itemLineId, FilterResults.FILTER_INFO_RECEIVED)
    }

    private fun closeSocket() {
        socket?.close()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            initialize()
            Timber.d("The service has been created")
            val notification = createNotification()
            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    "Filter notifications channel",
                    NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Filter notifications service channel"
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
                .setContentTitle("Listening for filter notifications")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_HIGH) // for under android 26 compatibility
                .build()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun publishResults(msg: String) {
        val result = xmlMapper.readValue(msg, FilteredInfoResponse::class.java)
        // Timber.d("parsed filter $result")
        val currentFilterString = getString(
                R.string.filter_values,
                result.storeNumber.toString(),
                result.registerNumber.toString(),
                result.txn.toString(),
                result.itemSeqNumber.toString()
        )
        if (prefs.customPrefs()[getString(R.string.key_last_filter), ""] ?: "" != currentFilterString) {
            sendFilterResultReceived(result.itemLineId!!)
            val intent = Intent(ACTION_RECEIVE_FILTER)
            intent.putExtra(EXTRA_FILTER, result)
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
            prefs.customPrefs()[getString(R.string.key_last_filter)] = currentFilterString
        }
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
        try {
            if (isServiceStarted) return
            isServiceStarted = true

            //start sending alife filter process message
            GlobalScope.launch {
                ALifeSenderService.enqueueWork(appContext, ALifeSenderService.Actions.START.name)
            }

            GlobalScope.launch(Dispatchers.Default) {
                receiveUdp()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        const val ACTION_RECEIVE_FILTER = "is.posctrl.posctrl_android.RECEIVE_FILTER"
        const val EXTRA_FILTER = "FILTER"

        private val LAUNCHER = ForegroundServiceLauncher(FilterReceiverService::class.java)

        @JvmStatic
        fun stop(context: Context) = LAUNCHER.stopService(context)

        fun enqueueWork(context: Context, action: String) {
            if (action == Actions.START.name) {
                Intent(context, FilterReceiverService::class.java).also {
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
