package `is`.posctrl.posctrl_android.service

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class UdpReceiverService : JobIntentService() {

    private var socket: DatagramSocket? = null

    private fun receiveUdp() {
        var p: DatagramPacket
        try {
            while (true) {
                val serverPort = 20000//todo change to get from prefs
                if (socket == null) {
                    socket = DatagramSocket(serverPort)
                    socket!!.broadcast = false
                }
                socket!!.reuseAddress = true
                socket!!.soTimeout = 10 * 1000
                Timber.d("waiting to receive data via udp")
                try {
                    val message = ByteArray(512 * 8) //8bytes x n params for a profile
                    p = DatagramPacket(message, message.size)
                    socket!!.receive(p)
                    Timber.d("received ${String(message).substring(0, p.length)}")

                } catch (e: SocketTimeoutException) {
                    e.printStackTrace()
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
        Timber.d("created udp receiver service")
    }

    override fun onHandleWork(intent: Intent) {
        receiveUdp()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("destroyed udp receiver service")
    }

    companion object {
        private const val UDP_RECEIVER_JOB = 1
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, UdpReceiverService::class.java, UDP_RECEIVER_JOB, intent)
        }
    }

}