package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "Action")
@Parcelize
data class ActionBody(
        @field:JacksonXmlProperty(localName = "AppName")
        val appName: String,
        @field:JacksonXmlProperty(localName = "AppVer")
        val appVersion: String,
        @field:JacksonXmlProperty(localName = "Process")
        val process: String,
        @field:JacksonXmlProperty(localName = "ActionType")
        val actionType: String = "",
        @field:JacksonXmlProperty(localName = "ClientName")
        val hostName: String
) : Parcelable

enum class Process(val value: String) {
    PROGRAM_START("ProgramStart"),
    PROGRAM_END("ProgramEnd")
}