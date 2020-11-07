package `is`.posctrl.posctrl_android.data


import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.LoginResponse
import `is`.posctrl.posctrl_android.data.model.ReceiptInfoBody
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.ui.receipt.ReceiptAction
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.sql.Date
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class PosCtrlRepository @Inject constructor(
        val prefs: PreferencesSource,
        private val appContext: Context,
        private val xmlMapper: XmlMapper
) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /*
    server: String = "192.168.0.110", port: String = "1433",databaseUser: String = "sa", databasePassword: String = "PosCtrl.1234",
        loginUser: String = "aron", loginPassword: String = "foot.1234"
     */
    @Throws(Exception::class)
    suspend fun login(
            server: String, port: String, databaseUser: String, databasePassword: String,
            loginUser: String, loginPassword: String
    ): ResultWrapper<*> {
        var response: LoginResponse? = null
        val connectionURL =
                "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
        Timber.e("connection url is $connectionURL")
        withContext(Dispatchers.Default) {//todo set all coroutines to dispatchers default as it s faster
            try {
                val connection = DriverManager.getConnection(connectionURL)
                val statement =
                        connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UserLogin] \'$loginUser\', \'$loginPassword\'}")//called the procedure
                val result = statement.executeQuery()
                while (result.next()) {
                    response = LoginResponse(
                            result.getString(COL_ERROR_MESSAGE),
                            result.getString(COL_SERVER_PATH),
                            result.getString(COL_SERVER_PORT),
                            result.getInt(COL_FILTER_RESPOND_TIME),
                            result.getString(COL_VERSION),
                            result.getString(COL_SERVER_USER),
                            result.getString(COL_SERVER_USER_DOMAIN),
                            result.getString(COL_SERVER_USER_PASSWORD),
                            result.getString(COL_SERVER_SNAPSHOT_PATH)
                    )
                    Timber.e(
                            "login result error message=${result.getString(COL_ERROR_MESSAGE)} , server path=${
                                result.getString(
                                        COL_SERVER_PATH
                                )
                            }"
                    )
                }
                connection.close()

            } catch (e: Exception) {
                e.printStackTrace()
                throw NoNetworkConnectionException()
            }
        }

        return if (response?.errorMessage == null) {
            ResultWrapper.Success(response)
        } else {
            ResultWrapper.Error(message = response?.errorMessage)
        }
    }

    @Throws(Exception::class)
    suspend fun getStores(
            server: String,
            port: String,
            databaseUser: String,
            databasePassword: String,
            loggedInUser: String
    ): ResultWrapper<*> {
        val storesList = arrayListOf<StoreResponse>()
        withContext(ioDispatcher) {
            try {
                val connectionURL =
                        "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
                Timber.e("connection url is $connectionURL")
                val connection = DriverManager.getConnection(connectionURL)
                val statement =
                        connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UsersStores] \'$loggedInUser\'}")//called the procedure
                val result = statement.executeQuery()
                while (result.next()) {
                    val response = StoreResponse(
                            result.getLong(COL_STORE_NUMBER),
                            result.getString(COL_STORE_NAME)
                    )
                    Timber.e("get stores result item $response")
                    storesList.add(response)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                throw NoNetworkConnectionException()
            }
        }

        return if (storesList.isNotEmpty()) {
            ResultWrapper.Success(storesList)
        } else {
            ResultWrapper.Error(message = appContext.applicationContext.getString(R.string.error_no_stores))
        }
    }

    @Throws(Exception::class)
    suspend fun getRegisters(
            server: String,
            port: String,
            databaseUser: String,
            databasePassword: String,
            storeNumber: Int,
            loggedInUser: String
    ): ResultWrapper<*> {
        val registers = arrayListOf<RegisterResponse>()
        withContext(ioDispatcher) {
            try {
                val connectionURL =
                        "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
                Timber.e("connection url is $connectionURL")
                val connection = DriverManager.getConnection(connectionURL)
                val statement =
                        connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UsersStoreRegisters] $storeNumber, \'$loggedInUser\'}")//called the procedure
                val result = statement.executeQuery()
                while (result.next()) {
                    val response = RegisterResponse(
                            result.getInt(COL_REGISTER_NUMBER)
                    )
                    Timber.e("get registers result item $response")
                    registers.add(response)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                throw NoNetworkConnectionException()
            }
        }

        return if (registers.isNotEmpty()) {
            ResultWrapper.Success(registers)
        } else {
            ResultWrapper.Error(message = appContext.applicationContext.getString(R.string.error_no_registers))
        }
    }

    @SuppressLint("HardwareIds")
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendReceiptInfoMessage(
            action: ReceiptAction = ReceiptAction.OPEN,
            storeNumber: Int = -1,
            registerNumber: Int = -1,
    ): ResultWrapper<*> {
        withContext(Dispatchers.Default) {
            try {
                if (storeNumber == -1 || registerNumber == -1) {
                    return@withContext ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
                }

                val hostName = Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                )

                val format = "yyyy-MM-dd HH:mm:ss"
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                val timeZone = TimeZone.getDefault()
                val calendar: Calendar = Calendar.getInstance(timeZone)
                val dateString = sdf.format(Date(calendar.timeInMillis))

                val receiptInfo = ReceiptInfoBody(
                        appName = appContext.getString(R.string.app_name),
                        userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                                ?: "",
                        action = action.actionValue,
                        storeNumber = storeNumber,
                        registerNumber = registerNumber,
                        hostName = hostName,//+build product if required
                        listeningPort = (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                                ?: DEFAULT_LISTENING_PORT).toInt(),
                        time = dateString
                )
                Timber.d("receipt info $receiptInfo")
                val xmlMessage = xmlMapper.writeValueAsString(receiptInfo)
                val bytes = xmlMessage.toByteArray()
                val broadcastIp = "255.255.255.255"
                val port: String =
                        prefs.customPrefs()[appContext.getString(R.string.key_server_port)]
                                ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port.toInt()))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                        bytes,
                        bytes.size, InetAddress.getByName(broadcastIp),
                        port.toInt()
                )

                if (action == ReceiptAction.CLOSE) {
                    prefs.customPrefs()[appContext.getString(R.string.key_send_alife, storeNumber, registerNumber)] = false
                }

                sendSocket.send(sendPacket)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ResultWrapper.Error(code = ErrorCode.NO_DATA_CONNECTION.code)
            }
        }

        return ResultWrapper.Success("")
    }
    //todo move get from prefs logic here instead of fragments/ view models

    @SuppressLint("HardwareIds")
    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendReceiptInfoALife(
            storeNumber: Int,
            registerNumber: Int,
    ) {
        withContext(Dispatchers.Default) {
            try {
                val hostName = Settings.Secure.getString(
                        appContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                )

                val format = "yyyy-MM-dd HH:mm:ss"
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                val timeZone = TimeZone.getDefault()
                val calendar: Calendar = Calendar.getInstance(timeZone)
                val dateString = sdf.format(Date(calendar.timeInMillis))

                val receiptInfo = ReceiptInfoBody(
                        appName = appContext.getString(R.string.app_name),
                        userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                                ?: "",
                        action = ReceiptAction.ALIFE.actionValue,
                        storeNumber = storeNumber,
                        registerNumber = registerNumber,
                        hostName = hostName,//+build product if required
                        listeningPort = (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                                ?: DEFAULT_LISTENING_PORT).toInt(),
                        time = dateString
                )
                Timber.d("receipt info $receiptInfo")
                val xmlMessage = xmlMapper.writeValueAsString(receiptInfo)
                val bytes = xmlMessage.toByteArray()
                val broadcastIp = "255.255.255.255"
                val port: String =
                        prefs.customPrefs()[appContext.getString(R.string.key_server_port)]
                                ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port.toInt()))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                        bytes,
                        bytes.size, InetAddress.getByName(broadcastIp),
                        port.toInt()
                )

                prefs.customPrefs()[appContext.getString(R.string.key_send_alife, storeNumber, registerNumber)] = true

                while (true) {
                    delay(ALIFE_DELAY_SECONDS * 1000L)
                    val sendAlife: Boolean = prefs.customPrefs()[appContext.getString(R.string.key_send_alife, storeNumber, registerNumber)]
                            ?: true
                    if (!sendAlife) {
                        return@withContext
                    }
                    sendSocket.send(sendPacket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ALIFE_DELAY_SECONDS = 60
        const val DATABASE_NAME = "PosCtrl-SelfService"
        const val COL_ERROR_MESSAGE = "ErrorMessage"
        const val COL_SERVER_PATH = "ServerPath"
        const val COL_SERVER_PORT = "ServerPort"
        const val COL_FILTER_RESPOND_TIME = "FilterRespondTime"
        const val COL_VERSION = "SmartWatchVersion"
        const val COL_SERVER_USER = "ServerUser"//for shared media folder
        const val COL_SERVER_USER_DOMAIN = "ServerUserDomain"
        const val COL_SERVER_USER_PASSWORD = "ServerUserPassword"
        const val COL_SERVER_SNAPSHOT_PATH = "SnapShotPath"
        const val COL_STORE_NUMBER = "StoreNumber"
        const val COL_STORE_NAME = "StoreName"
        const val COL_REGISTER_NUMBER = "RegisterNumber"
        const val DEFAULT_SERVER_PORT = "11970"
        const val DEFAULT_LISTENING_PORT = "20000"
    }
}

class NoNetworkConnectionException : Exception()