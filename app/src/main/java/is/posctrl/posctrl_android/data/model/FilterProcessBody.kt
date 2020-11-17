package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "FilterProcess")
@Parcelize
data class FilterProcessBody(
        @field:JacksonXmlProperty(localName = "AppName")
        val appName: String,
        @field:JacksonXmlProperty(localName = "AppVer")
        val appVersion: String,
        @field:JacksonXmlProperty(localName = "UserID")
        val userId: String,
        @field:JacksonXmlProperty(localName = "Action")
        val action: String,
        @field:JacksonXmlProperty(localName = "HostName")
        val hostName: String,
        @field:JacksonXmlProperty(localName = "Port")
        val listeningPort: Int,
        @field:JacksonXmlProperty(localName = "LocalTime")
        val time: String
) : Parcelable

enum class FilterAction(val actionValue: String) {
    OPEN("Open"), CLOSE("Close"), ALIFE("ALife")
}
//todo show filter notification inside onResume only if setting is enabled