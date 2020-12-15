package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "Receipt")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Parcelize
data class ReceiptResponse(
    private var _storeNumber: Int = -1,
    private var _registerNumber: Int = -1,
    private var _clearTextFlag: Int = -1,//if it's different from the one received before, clear text before appending this line
    private var _color: String = "",
    private var _bold: Int = -1,//if 1, bold text
    private var _italic: Int = -1,//if 1, italic text
    private var _endFlag: Int = -1,
    private var _line: String = "",

    ) : Parcelable {
    @get:JacksonXmlProperty(localName = "StoreNumber")
    var storeNumber: Int
        set(value) {
            _storeNumber = value
        }
        get() = _storeNumber

    @get:JacksonXmlProperty(localName = "RegisterNumber")
    var registerNumber: Int
        set(value) {
            _registerNumber = value
        }
        get() = _registerNumber

    @get:JacksonXmlProperty(localName = "TxnNumber")
    var clearTextFlag: Int
        //if it's different from the one received before, clear text before appending this line
        set(value) {
            _clearTextFlag = value
        }
        get() = _clearTextFlag

    @get:JacksonXmlProperty(localName = "Color")
    var color: String
        set(value) {
            _color = value
        }
        get() = _color

    @get:JacksonXmlProperty(localName = "Bold")
    var bold: Int
        //if 1, bold text
        set(value) {
            _bold = value
        }
        get() = _bold

    @get:JacksonXmlProperty(localName = "Italic")
    var italic: Int
        //if 1, italic text
        set(value) {
            _italic = value
        }
        get() = _italic

    @get:JacksonXmlProperty(localName = "EndOfTxn")//if 1, clear text before appending this line
    var endFlag: Int
        set(value) {
            _endFlag = value
        }
        get() = _endFlag

    @get:JacksonXmlProperty(localName = "Line")
    var line: String
        set(value) {
            _line = value
        }
        get() = _line


}