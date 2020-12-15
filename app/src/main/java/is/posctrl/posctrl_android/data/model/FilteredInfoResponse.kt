package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "FilteredInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Parcelize
data class FilteredInfoResponse(
    private var _storeNumber: Int = -1,
    private var _registerNumber: Int = -1,
    private var _itemLineId: Int = -1,
    private var _storeName: String = "",
    private var _itemId: Int = -1,//if 1, bold text
    private var _itemName: String = "",//if 1, italic text
    private var _quantity: String = "0.0",
    private var _totalPrice: String = "0.0",
    // private var _filterName: String = "",
    private var _filterQuestion: String = "",
    private var _pictures: List<Picture> = listOf(),
    private var _itemSeqNumber: Int = -1,
    private var _txn: Int = -1
) : Parcelable {

    @get:JacksonXmlProperty(localName = "ItemLineID")
    var itemLineId: Int?
        set(value) {
            _itemLineId = value ?: -1
        }
        get() = _itemLineId

    @get:JacksonXmlProperty(localName = "StoreNumber")
    var storeNumber: Int?
        set(value) {
            _storeNumber = value ?: -1
        }
        get() = _storeNumber

    @get:JacksonXmlProperty(localName = "StoreName")
    var storeName: String?
        set(value) {
            _storeName = value ?: ""
        }
        get() = _storeName

    @get:JacksonXmlProperty(localName = "RegisterNumber")
    var registerNumber: Int?
        set(value) {
            _registerNumber = value ?: -1
        }
        get() = _registerNumber

    @get:JacksonXmlProperty(localName = "ItemID")
    var itemId: Int?
        set(value) {
            _itemId = value ?: -1
        }
        get() = _itemId

    @get:JacksonXmlProperty(localName = "ItemName")
    var itemName: String?
        set(value) {
            _itemName = value ?: ""
        }
        get() = _itemName

    @get:JacksonXmlProperty(localName = "Quantity")
    var quantity: String?
        set(value) {
            _quantity = value ?: ""
        }
        get() = _quantity

    @get:JacksonXmlProperty(localName = "TotalPrice")
    var totalPrice: String?
        set(value) {
            _totalPrice = value ?: ""
        }
        get() = _totalPrice

    /* @get:JacksonXmlProperty(localName = "FilterName")
     var filterName: String
         set(value) {
             _filterName = value
         }
         get() = _filterName*/

    @get:JacksonXmlProperty(localName = "FilterQuestion")
    var filterQuestion: String?
        set(value) {
            _filterQuestion = value ?: ""
        }
        get() = _filterQuestion

    @get:JacksonXmlElementWrapper(localName = "SnapShot")
    var pictures: List<Picture>?
        set(value) {
            _pictures = value ?: listOf()
        }
        get() = _pictures

    var quantityNumber = {
        val quantityText = _quantity.replace(",", ".")
        quantityText.toDoubleOrNull()
    }

    var totalPriceNumber = {
        val totalPriceText = _totalPrice.replace(",", ".")
        totalPriceText.toDoubleOrNull()
    }

    @get:JacksonXmlProperty(localName = "ItemSeqNumber")
    var itemSeqNumber: Int?
        set(value) {
            _itemSeqNumber = value ?: -1
        }
        get() = _itemSeqNumber

    @get:JacksonXmlProperty(localName = "TxnNumber")
    var txn: Int?
        set(value) {
            _txn = value ?: -1
        }
        get() = _txn
}