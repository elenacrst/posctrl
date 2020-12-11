package `is`.posctrl.posctrl_android.service

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.clear
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.ui.base.BaseActivity
import `is`.posctrl.posctrl_android.ui.MainActivity
import `is`.posctrl.posctrl_android.ui.settings.appoptions.AppOptionsViewModel
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_POWER_CONNECTED
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import timber.log.Timber
import javax.inject.Inject


class ChargingService : Service() {
    private val chargeDetector = createChargingReceiver()

    @Inject
    lateinit var prefs: PreferencesSource

    private fun createChargingReceiver(): BroadcastReceiver {
        return ChargingReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForeground() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        startForeground(
            CHARGING_SERVICE, NotificationCompat.Builder(
                this,
                CHARGING_CHANNEL_ID
            ) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    prefs.defaultPrefs()["title_notification_charging", getString(R.string.title_notification_charging)]
                        ?: getString(R.string.title_notification_charging)
                )
                .setContentText(
                    prefs.defaultPrefs()["message_notification_charging", getString(R.string.message_notification_charging)]
                        ?: getString(R.string.message_notification_charging)
                )
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHARGING_CHANNEL_ID,
                "Charging service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("started charging service")
        (applicationContext as PosCtrlApplication).appComponent.inject(this)
        startForeground()

        val filter = IntentFilter()
        filter.addAction(ACTION_POWER_CONNECTED)
        registerReceiver(chargeDetector, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.d("destroyed charging service")
        unregisterReceiver(chargeDetector)
        super.onDestroy()

    }

    companion object {
        const val CHARGING_SERVICE = 2
        const val CHARGING_CHANNEL_ID = "ChargingService"
    }
}

class ChargingReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("charging - received broadcast")
        val preferencesSource = PreferencesSource(context.applicationContext)
        preferencesSource.customPrefs().clear()
        Timber.d("charging - cleared prefs")
        stopFilterReceiverService(context)
        stopReceiptReceiverService(context)
        val appOptionsViewModel =
            AppOptionsViewModel(PosCtrlRepository(preferencesSource, context, XmlMapper()))
        appOptionsViewModel.closeFilterNotifications()
        //todo might be useless to track app visibility
        val i = Intent(BaseActivity.ACTION_LOGOUT)
        LocalBroadcastManager.getInstance(context).sendBroadcast(i)
    }

    private fun stopFilterReceiverService(context: Context) {
        val intent = Intent(context, FilterReceiverService::class.java)
        context.stopService(intent)
    }

    private fun stopReceiptReceiverService(context: Context) {
        val intent = Intent(context, ReceiptReceiverService::class.java)
        context.stopService(intent)
    }
}

