package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RegisterResponse(
        val registerNumber: Int?
) : Parcelable