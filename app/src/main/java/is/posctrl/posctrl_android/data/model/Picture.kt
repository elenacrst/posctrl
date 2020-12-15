package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import kotlinx.android.parcel.Parcelize

@Parcelize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Picture(
    private var _imageAddress: String = ""
) : Parcelable {
    @get:JacksonXmlProperty(localName = "jpg")
    var imageAddress: String?
        set(value) {
            _imageAddress = value ?: ""
        }
        get() = _imageAddress
}