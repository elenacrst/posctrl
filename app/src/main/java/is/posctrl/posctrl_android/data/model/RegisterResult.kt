package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RegisterResult(
        private var _registerNumber: String = ""
) : Parcelable {
    @get:JacksonXmlProperty(localName = "RegNum")
    var registerNumber: String
        set(value) {
            _registerNumber = value
        }
        get() = _registerNumber
}