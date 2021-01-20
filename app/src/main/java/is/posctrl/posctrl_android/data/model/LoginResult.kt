package `is`.posctrl.posctrl_android.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.android.parcel.Parcelize

@JacksonXmlRootElement(localName = "LoginResult")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Parcelize
data class LoginResult(
        private var _errorMessage: String = "",
        private var _username: String = "",
        private var _serverPath: String = "",
        private var _serverPort: Int = -1,
        private var _filterRespondTime: Int = -1,
        private var _appVersion: String = "",
        private var _serverUser: String = "",
        private var _serverUserDomain: String = "",
        private var _serverPassword: String = "",
        private var _serverSnapshotPath: String = "",
        private var _masterPassword: String = "",
        private var _timeZone: String = "",
        private var _receiveNotification: Int = 0,
        private var _notificationSound: Boolean = false,
        private var _store: StoreResult = StoreResult(),
        private var _serverTime: String = "",
        private var _restartHour: Int = 23,//todo change to 0 after testing
        private var _texts: List<TextItem> = listOf()
) : Parcelable {
    @get:JacksonXmlProperty(localName = "ErrorMessage")
    var errorMessage: String
        set(value) {
            _errorMessage = value
        }
        get() = _errorMessage

    @get:JacksonXmlProperty(localName = "UserName")
    var username: String
        set(value) {
            _username = value
        }
        get() = _username

    @get:JacksonXmlProperty(localName = "ServerPath")
    var serverPath: String
        set(value) {
            _serverPath = value
        }
        get() = _serverPath

    @get:JacksonXmlProperty(localName = "ServerPort")
    var serverPort: Int
        set(value) {
            _serverPort = value
        }
        get() = _serverPort

    @get:JacksonXmlProperty(localName = "FilterRespondTime")
    var filterRespondTime: Int
        set(value) {
            _filterRespondTime = value
        }
        get() = _filterRespondTime

    @get:JacksonXmlProperty(localName = "SmartWatchVersion")
    var appVersion: String
        set(value) {
            _appVersion = value
        }
        get() = _appVersion

    @get:JacksonXmlProperty(localName = "ServerUser")
    var serverUser: String
        set(value) {
            _serverUser = value
        }
        get() = _serverUser

    @get:JacksonXmlProperty(localName = "ServerUserDomain")
    var serverUserDomain: String
        set(value) {
            _serverUserDomain = value
        }
        get() = _serverUserDomain

    @get:JacksonXmlProperty(localName = "ServerUserPassword")
    var serverPassword: String
        set(value) {
            _serverPassword = value
        }
        get() = _serverPassword

    @get:JacksonXmlProperty(localName = "SnapShotPath")
    var serverSnapshotPath: String
        set(value) {
            _serverSnapshotPath = value
        }
        get() = _serverSnapshotPath

    @get:JacksonXmlProperty(localName = "MasterPassword")
    var masterPassword: String
        set(value) {
            _masterPassword = value
        }
        get() = _masterPassword

    @get:JacksonXmlElementWrapper(localName = "TimeZone")
    var timeZone: String
        set(value) {
            _timeZone = value
        }
        get() = _timeZone

    @get:JacksonXmlProperty(localName = "ReceiveNotification")
    var receiveNotification: Int
        set(value) {
            _receiveNotification = value
        }
        get() = _receiveNotification

    @get:JacksonXmlProperty(localName = "NotificationSound")
    var notificationSound: Int
        set(value) {
            _notificationSound = value == 1
        }
        get() = if (_notificationSound) 1 else 0

    @get:JacksonXmlElementWrapper(localName = "Store")
    var store: StoreResult
        set(value) {
            _store = value
        }
        get() = _store

    @get:JacksonXmlProperty(localName = "ServerTime")
    var serverTime: String
        set(value) {
            _serverTime = value
        }
        get() = _serverTime

    @get:JacksonXmlElementWrapper(localName = "Texts")
    var texts: List<TextItem>
        set(value) {
            _texts = value
        }
        get() = _texts

    fun isReceivingNotifications(): Boolean {
        return (receiveNotification != 0)
    }

    fun isNotificationSoundEnabled(): Boolean {
        return _notificationSound
    }

    @get:JacksonXmlProperty(localName = "RestartHour")
    var restartHour: Int
        set(value) {
            _restartHour = value
        }
        get() = _restartHour
}