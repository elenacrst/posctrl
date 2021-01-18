package `is`.posctrl.posctrl_android.util.extensions

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.RememberedUser
import `is`.posctrl.posctrl_android.databinding.DialogConfirmBinding
import `is`.posctrl.posctrl_android.databinding.DialogEtBinding
import `is`.posctrl.posctrl_android.databinding.DialogLoadingBinding
import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.drakeet.support.toast.ToastCompat
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


fun Activity.toast(message: String) {
    if (isFinishing || isDestroyed) {
        return
    }
    ToastCompat.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}

fun Context.showInputDialog(title: String, positiveCallback: (input: String) -> Unit) {
    val prefs = PreferencesSource(applicationContext)
    val binding: DialogEtBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.dialog_et,
            null,
            false
    )
    binding.edit.inputType = InputType.TYPE_CLASS_NUMBER

    val dialog = MaterialAlertDialogBuilder(
            this,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
    )
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(
                    prefs.defaultPrefs()["action_ok", applicationContext.getString(R.string.action_ok)]
                            ?: applicationContext.getString(R.string.action_ok)
            ) { _, _ ->
                positiveCallback(binding.edit.text.toString())
            }
            .create()
    dialog.show()
}

fun Context.showConfirmDialog(title: String, registerNumber: Int, positiveCallback: () -> Unit) {
    val prefs = PreferencesSource(this)
    val binding: DialogConfirmBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.dialog_confirm,
            null,
            false
    )
    binding.tvTitle.text = title
    binding.tvRegister.text = registerNumber.toString()
    binding.btYes.text = prefs.defaultPrefs()["action_suspend", getString(R.string.action_suspend)]
            ?: getString(R.string.action_suspend)

    val dialog = MaterialAlertDialogBuilder(
            this
    )
            .setView(binding.root)
            .create()
    binding.btYes.setOnClickListener {
        dialog.dismiss()
        positiveCallback()
    }
    dialog.show()
}

fun Context.getAppVersion(): String {
    return try {
        val pInfo: PackageInfo =
                packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        "unknown"
    }
}

fun Context.getAppDirectory(): File {
    val directory = File(
            applicationContext.getExternalFilesDir(null)
                    .toString() + PosCtrlRepository.APP_DIR
    )
    if (!directory.exists()) {
        directory.mkdirs()
    }
    return directory
}

fun Context.showUpdateDialog(): androidx.appcompat.app.AlertDialog {
    val prefs = PreferencesSource(this)
    val binding: DialogLoadingBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.dialog_loading,
            null,
            false
    )
    val dialog = MaterialAlertDialogBuilder(
            this,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
    )
            .setTitle(
                    prefs.defaultPrefs()["title_downloading_update", applicationContext.getString(R.string.title_downloading_update)]
                            ?: applicationContext.getString(R.string.title_downloading_update)
            )
            .setMessage(
                    prefs.defaultPrefs()["message_wait_download", applicationContext.getString(R.string.message_wait_download)]
                            ?: applicationContext.getString(R.string.message_wait_download)
            )
            .setView(binding.root)
            .setCancelable(false)
            .create()
    dialog.show()
    return dialog
}

fun Context.getRememberedUsers(prefs: PreferencesSource): List<RememberedUser> {
    val list = mutableListOf<RememberedUser>()

    var rememberedUsersString = (prefs.defaultPrefs()[getString(R.string.key_users), ""] ?: "")
    val rememberedUsers = rememberedUsersString.split(";")
    rememberedUsers.forEach {
        if (it.split(",").size == 3) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.split(",")[2].toLong()
            if (System.currentTimeMillis() - calendar.timeInMillis <= TimeUnit.DAYS.toMillis(14)) {
                list.add(RememberedUser(it.split(",")[0], it.split(",")[1], it.split(",")[2].toLong()))
            } else {
                rememberedUsersString = rememberedUsersString.replace(it, "")
            }
        }

    }
    prefs.defaultPrefs()[getString(R.string.key_users)] = rememberedUsersString
    list.sortBy { it.userId }
    return list
}

fun Context.getWifiLevel(): Int {
    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
    val linkSpeed = wifiManager.connectionInfo.rssi
    return WifiManager.calculateSignalLevel(linkSpeed, 4)
}
