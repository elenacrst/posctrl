package `is`.posctrl.posctrl_android.util.extensions

import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.databinding.DialogEtBinding
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

    val dialog = MaterialAlertDialogBuilder(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
            .setTitle(titleRes)
            .setView(binding.root)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                positiveCallback(binding.edit.text.toString())
            }
            .create()
    dialog.show()
}