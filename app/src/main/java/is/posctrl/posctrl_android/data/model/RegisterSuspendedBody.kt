package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "RegisterSuspend")
@Parcelize
data class RegisterSuspendedBody(
        @field:JacksonXmlProperty(localName = "Message")
        val message: String,
        @field:JacksonXmlProperty(localName = "StoreNumber")
        val storeNumber: Int,
        @field:JacksonXmlProperty(localName = "RegisterNumber")
        val registerNumber: Int
) : Parcelable