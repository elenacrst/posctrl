package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "Store")
@Parcelize
data class StoreResult(
        private var _storeNumber: Int = -1,
        private var _storeName: String = "",
        private var _registers: List<RegisterResult> = listOf()
) : Parcelable {
    @get:JacksonXmlProperty(localName = "StoreNumber")
    var storeNumber: Int
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
}