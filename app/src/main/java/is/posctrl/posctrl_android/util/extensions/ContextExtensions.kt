package `is`.posctrl.posctrl_android.util.extensions

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.databinding.DialogConfirmBinding
import `is`.posctrl.posctrl_android.databinding.DialogEtBinding
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.showInputDialog(titleRes: Int, positiveCallback: (input: String) -> Unit) {
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
        .setTitle(titleRes)
        .setView(binding.root)
        .setPositiveButton(R.string.action_ok) { _, _ ->
            positiveCallback(binding.edit.text.toString())
        }
        .create()
    dialog.show()
}

fun Context.showConfirmDialog(title: String, positiveCallback: () -> Unit) {
    val binding: DialogConfirmBinding = DataBindingUtil.inflate(
        LayoutInflater.from(this),
        R.layout.dialog_confirm,
        null,
        false
    )
    binding.tvTitle.text = title

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