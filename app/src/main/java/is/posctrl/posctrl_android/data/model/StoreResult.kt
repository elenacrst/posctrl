package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "Store")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Parcelize
data class StoreResult(
        private var _storeNumber: String = "",
        private var _storeName: String = "",
        private var _registers: List<RegisterResult> = listOf(),
        private var _registersColumns: Int = -1
) : Parcelable {
    @get:JacksonXmlProperty(localName = "StoreNumber")
    var storeNumber: String
        set(value) {
            _storeNumber = value
        }
        get() = _storeNumber

    @get:JacksonXmlProperty(localName = "StoreName")
    var storeName: String
        set(value) {
            _storeName = value
        }
        get() = _storeName

    @get:JacksonXmlElementWrapper(localName = "Registers")
    var registers: List<RegisterResult>
        set(value) {
            _registers = value
        }
        get() = _registers

    @get:JacksonXmlProperty(localName = "RegCol")
    var registersColumns: Int
        set(value) {
            _registersColumns = value
        }
        get() = _registersColumns
}