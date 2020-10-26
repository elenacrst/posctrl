package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StoreResponse(
        val storeNumber: Long?,
        val storeName: String?
) : Parcelable