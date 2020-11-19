package `is`.posctrl.posctrl_android.data.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BitmapsResult(
    var bitmaps: List<Bitmap> = listOf(),
    var errors: Int = 0
) : Parcelable