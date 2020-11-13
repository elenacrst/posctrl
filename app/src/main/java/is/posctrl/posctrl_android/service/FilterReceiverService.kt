package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository.Companion.DEFAULT_FILTER_PORT
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import javax.inject.Inject


class FilterReceiverService : JobIntentService() {

    @Inject
    lateinit var xmlMapper: XmlMapper

    @Inject
    lateinit var prefs: PreferencesSource

    @Inject
    lateinit var appContext: Application

    private var socket: DatagramSocket? = null

    private fun receiveUdp() {
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
                    val message = ByteArray(512)
                    p = DatagramPacket(message, message.size)
                    socket!!.receive(p)
                    Timber.d("received filter ${String(message).substring(0, p.length)}")
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
    }

    override fun onHandleWork(intent: Intent) {
        receiveUdp()
    }

    private fun publishResults(output: String) {
        val result = xmlMapper.readValue(output, FilteredInfoResponse::class.java)
        Timber.d("parsed filter $result")
        val intent = Intent(ACTION_RECEIVE_FILTER)
        intent.putExtra(EXTRA_FILTER, result)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        closeSocket()
        super.onDestroy()
    }

    companion object {
        private const val FILTER_RECEIVER_JOB = 2
        const val ACTION_RECEIVE_FILTER = "RECEIVE_FILTER"
        const val EXTRA_FILTER = "FILTER"

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, FilterReceiverService::class.java, FILTER_RECEIVER_JOB, intent)
        }
    }

}