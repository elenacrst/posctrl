package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@Parcelize
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Text")
data class TextItem(
    private var _id: String = "",
    private var _string: String = ""
) : Parcelable {
    @get:JacksonXmlProperty(localName = "ID")
    var id: String
        set(value) {
            _id = value
        }
        get() = _id

    @get:JacksonXmlProperty(localName = "String")
    var string: String
        set(value) {
            _string = value
        }
        get() = _string
}