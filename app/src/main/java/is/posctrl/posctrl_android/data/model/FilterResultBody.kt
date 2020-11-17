package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "FilterResult")
@Parcelize
data class FilterResultBody(
        @field:JacksonXmlProperty(localName = "AppName")
        val appName: String,
        @field:JacksonXmlProperty(localName = "ItemLineID")
        val itemLineId: Int,
        @field:JacksonXmlProperty(localName = "Result")
        val result: Int,
        @field:JacksonXmlProperty(localName = "LocalTime")
        val time: String
) : Parcelable

enum class FilterResults(val result: Int) {
    FILTER_INFO_RECEIVED(10),
    ACCEPTED(11),
    REJECTED(19),
    TIMED_OUT(18)
}