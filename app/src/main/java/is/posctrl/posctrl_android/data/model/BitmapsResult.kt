package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BitmapsResult(
    var bitmaps: List<String> = listOf(),//file names
    var errors: Int = 0
) : Parcelable