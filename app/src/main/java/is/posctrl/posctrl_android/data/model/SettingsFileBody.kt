package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "PosCtrl")
@JsonIgnoreProperties(ignoreUnknown = true)
@Parcelize
data class SettingsFileBody(

        private var _loginServer: String = "",

        private var _loginPort: String = ""
) : Parcelable {
    @get:JacksonXmlProperty(localName = "ServerPath")
    var loginServer: String
        set(value) {
            _loginServer = value
        }
        get() = _loginServer

    @get:JacksonXmlProperty(localName = "ServerPort")
    var loginPort: String
        set(value) {
            _loginPort = value
        }
        get() = _loginPort
}