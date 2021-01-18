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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fasterxml.jackson.dataformat.xml.XmlMapper
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
                try {
                    val message = ByteArray(512)
                    p = DatagramPacket(message, message.size)
                    socket!!.receive(p)
//                    Timber.d("received ${String(message).substring(0, p.length)}")
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
        val result = xmlMapper.readValue(output, ReceiptResponse::class.java)
        val intent = Intent(ACTION_RECEIVE_RECEIPT)
        intent.putExtra(EXTRA_RECEIPT, result)
        LocalBroadcastManager.getInstance(appContext)
                .sendBroadcast(intent)//todo use directly application context, no need to inject this, same for other service
    }

    companion object {
        private const val RECEIPT_RECEIVER_JOB = 1
        const val ACTION_RECEIVE_RECEIPT = "is.posctrl.posctrl_android.RECEIVE_RECEIPT"
        const val EXTRA_RECEIPT = "RECEIPT"

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, ReceiptReceiverService::class.java, RECEIPT_RECEIVER_JOB, intent)
        }
    }

}