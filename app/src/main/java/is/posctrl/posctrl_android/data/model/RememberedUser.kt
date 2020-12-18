package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RememberedUser(
        var userId: String,
        var password: String,
        var date: Long
) : Parcelable