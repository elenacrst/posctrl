package `is`.posctrl.posctrl_android.data


import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.*
import `is`.posctrl.posctrl_android.ui.receipt.ReceiptAction
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
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
            "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword;sendStringParametersAsUnicode=false;"
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
        withContext(Dispatchers.Default) {
            try {
                val connectionURL =
                    "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword;sendStringParametersAsUnicode=false;"
                Timber.e("connection url is $connectionURL, user $loggedInUser")
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
        withContext(Dispatchers.Default) {
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
                val receiptInfo = ReceiptInfoBody(
                    appName = appContext.getString(R.string.app_name),
                    userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                        ?: "",
                    action = action.actionValue,
                    storeNumber = storeNumber,
                    registerNumber = registerNumber,
                    hostName = getDeviceIdentifier(),//+build product if required
                    listeningPort = (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                        ?: DEFAULT_LISTENING_PORT).toInt(),
                    time = getLocalTimeString()
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
                    bytes.size,
                    InetAddress.getByName(broadcastIp),
                    port.toInt()
                )
                if (action == ReceiptAction.CLOSE) {
                    prefs.customPrefs()[appContext.getString(
                        R.string.key_send_alife,
                        storeNumber,
                        registerNumber
                    )] = false
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

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendReceiptInfoALife(
        storeNumber: Int,
        registerNumber: Int,
    ) {
        withContext(Dispatchers.Default) {
            try {
                val receiptInfo = ReceiptInfoBody(
                    appName = appContext.getString(R.string.app_name),
                    userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                        ?: "",
                    action = ReceiptAction.ALIFE.actionValue,
                    storeNumber = storeNumber,
                    registerNumber = registerNumber,
                    hostName = getDeviceIdentifier(),//+build product if required
                    listeningPort = (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                        ?: DEFAULT_LISTENING_PORT).toInt(),
                    time = getLocalTimeString()
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
                prefs.customPrefs()[appContext.getString(
                    R.string.key_send_alife,
                    storeNumber,
                    registerNumber
                )] = true
                while (true) {
                    delay(ALIFE_DELAY_SECONDS * 1000L)
                    val sendAlife: Boolean = prefs.customPrefs()[appContext.getString(
                        R.string.key_send_alife,
                        storeNumber,
                        registerNumber
                    )]
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

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendSuspendRegisterMessage(
        storeNumber: Int,
        registerNumber: Int,
    ) {
        withContext(Dispatchers.Default) {
            try {
                val registerSuspendedBody = RegisterSuspendedBody(
                    message = "Register suspended",
                    storeNumber = storeNumber,
                    registerNumber = registerNumber
                )
                Timber.d("register suspended body: $registerSuspendedBody")
                val xmlMessage = xmlMapper.writeValueAsString(registerSuspendedBody)
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
                sendSocket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendFilterProcessMessage(action: FilterAction) {//for open & close, not for a life
        withContext(Dispatchers.Default) {
            try {
                val appVersion = try {
                    val pInfo: PackageInfo =
                        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                    pInfo.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    "unknown"
                }
                val filterProcessBody = FilterProcessBody(
                    appName = appContext.getString(R.string.app_name),
                    appVersion = appVersion,
                    userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                        ?: "",
                    action = action.actionValue,
                    hostName = getDeviceIdentifier(),
                    listeningPort = prefs.customPrefs()[appContext.getString(R.string.key_filter_port), DEFAULT_FILTER_PORT]
                        ?: DEFAULT_FILTER_PORT,
                    time = getLocalTimeString()
                )
                Timber.d("filter process body: $filterProcessBody")
                val xmlMessage = xmlMapper.writeValueAsString(filterProcessBody)
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
                if (action == FilterAction.CLOSE) {
                    prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)] =
                        false
                }
                sendSocket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendFilterProcessALife() {
        withContext(Dispatchers.Default) {
            try {
                val appVersion = try {
                    val pInfo: PackageInfo =
                        appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                    pInfo.versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    "unknown"
                }
                val filterProcessBody = FilterProcessBody(
                    appName = appContext.getString(R.string.app_name),
                    appVersion = appVersion,
                    userId = prefs.customPrefs()[appContext.getString(R.string.key_logged_user)]
                        ?: "",
                    action = FilterAction.ALIFE.actionValue,
                    hostName = getDeviceIdentifier(),
                    listeningPort = prefs.customPrefs()[appContext.getString(R.string.key_filter_port), DEFAULT_FILTER_PORT]
                        ?: DEFAULT_FILTER_PORT,
                    time = getLocalTimeString()
                )
                Timber.d("filter process body: $filterProcessBody")
                val xmlMessage = xmlMapper.writeValueAsString(filterProcessBody)
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
                prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)] = true
                while (true) {
                    delay(ALIFE_FILTER_DELAY_SECONDS * 1000L)
                    val sendAlife: Boolean =
                        prefs.customPrefs()[appContext.getString(R.string.key_send_alife_filter)]
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

    @SuppressLint("HardwareIds")
    private fun getDeviceIdentifier(): String {
        return Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun getLocalTimeString(): String {
        val format = "yyyy-MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(format, Locale.ENGLISH)
        val timeZone = TimeZone.getDefault()
        val calendar: Calendar = Calendar.getInstance(timeZone)
        return sdf.format(Date(calendar.timeInMillis))
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun downloadBitmaps(snapshotPath: String, fileNames: List<String>): ResultWrapper<*> {
        val bitmaps = mutableListOf<Bitmap>()
        var errors = 0
        try {
            withContext(Dispatchers.IO) {
                val client = SMBClient()
                val paths = snapshotPath.split("\\").filter { it.isNotEmpty() }
                Timber.d("paths ${paths.joinToString(",")}")
                val server = paths[0]
                val sharedFolder = paths[1]
                val user = prefs.customPrefs()[appContext.getString(R.string.key_server_user), ""]
                val password =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_password), ""]

                client.connect(server)
                    .use { connection ->
                        val ac = AuthenticationContext(
                            user,
                            password?.toCharArray(),
                            ""
                        )
                        val session: Session = connection.authenticate(ac)
                        fileNames.forEach { fullAddress ->
                            try {
                                val fileName = fullAddress.split("\\$sharedFolder\\").last()
                                (session.connectShare(sharedFolder) as? DiskShare?)?.let { share ->
                                    val s: MutableSet<SMB2ShareAccess> = HashSet()
                                    s.add(SMB2ShareAccess.FILE_SHARE_READ)
                                    val file = share.openFile(
                                        fileName,
                                        EnumSet.of(AccessMask.GENERIC_READ), null, s,
                                        SMB2CreateDisposition.FILE_OPEN, null
                                    )
                                    val inputStream = file.inputStream
                                    val bitmap = BitmapFactory.decodeStream(inputStream)
                                    bitmap?.let {
                                        bitmaps += it
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errors++
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val message =
                appContext.applicationContext.getString(R.string.error_no_snapshots)

            return ResultWrapper.Error(message = message)
        }
        if (errors > 0) {
            return if (bitmaps.isNotEmpty()) {
                ResultWrapper.Success(BitmapsResult(bitmaps, errors))
            } else {
                val message = appContext.applicationContext.getString(R.string.error_no_snapshots)
                ResultWrapper.Error(message = message)
            }
        }

        return ResultWrapper.Success(BitmapsResult(bitmaps, 0))
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendFilterResultMessage(
        itemLineId: Int, result: FilterResults
    ) {
        withContext(Dispatchers.Default) {
            try {
                val filterResult = FilterResultBody(
                    appName = appContext.getString(R.string.app_name),
                    itemLineId,
                    result.result,
                    getLocalTimeString()
                )
                Timber.d("filter result body: $filterResult")
                val xmlMessage = xmlMapper.writeValueAsString(filterResult)
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
                sendSocket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ALIFE_DELAY_SECONDS = 60
        const val ALIFE_FILTER_DELAY_SECONDS = 60
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
        const val DEFAULT_FILTER_PORT = 29999
    }
}

class NoNetworkConnectionException : Exception()