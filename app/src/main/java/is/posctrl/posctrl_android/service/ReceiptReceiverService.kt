package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.model.ReceiptResponse
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


class ReceiptReceiverService : JobIntentService() {

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
                val serverPort =
                    (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                        ?: PosCtrlRepository.DEFAULT_LISTENING_PORT).toInt()
                if (socket == null) {
                    socket = DatagramSocket(serverPort)
                    socket!!.broadcast = false
                }
                socket!!.reuseAddress = true
                socket!!.soTimeout = 60 * 1000
                Timber.d("waiting to receive data via udp")
                try {
                    val message = ByteArray(512)
                    p = DatagramPacket(message, message.size)
                    socket!!.receive(p)
                    Timber.d("received ${String(message).substring(0, p.length)}")
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
        Timber.d("created udp receiver service")
    }

    override fun onHandleWork(intent: Intent) {
        receiveUdp()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("destroyed udp receiver service")
    }

    private fun publishResults(output: String) {
        val result = xmlMapper.readValue(output, ReceiptResponse::class.java)
        val intent = Intent(ACTION_RECEIVE_RECEIPT)
        intent.putExtra(EXTRA_RECEIPT, result)
        sendBroadcast(intent)
    }

    companion object {
        private const val RECEIPT_RECEIVER_JOB = 1
        const val ACTION_RECEIVE_RECEIPT = "RECEIVE_RECEIPT"
        const val EXTRA_RECEIPT = "RECEIPT"

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, ReceiptReceiverService::class.java, RECEIPT_RECEIVER_JOB, intent)
        }
    }

}