package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "Login")
@Parcelize
data class LoginBody(
        @field:JacksonXmlProperty(localName = "AppName")
        val appName: String,
        @field:JacksonXmlProperty(localName = "AppVer")
        val appVersion: String,
        @field:JacksonXmlProperty(localName = "UserID")
        val userId: String,
        @field:JacksonXmlProperty(localName = "Password")
        val password: String,
        @field:JacksonXmlProperty(localName = "HostName")
        val hostName: String,
        @field:JacksonXmlProperty(localName = "LocalTime")
        val time: String,
        @field:JacksonXmlProperty(localName = "Port")
        val listeningPort: Int = 29998,
) : Parcelable