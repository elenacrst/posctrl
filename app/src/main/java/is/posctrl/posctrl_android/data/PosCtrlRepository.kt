package `is`.posctrl.posctrl_android.data


import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.*
import `is`.posctrl.posctrl_android.ui.receipt.ReceiptAction
import `is`.posctrl.posctrl_android.util.extensions.getAppDirectory
import `is`.posctrl.posctrl_android.util.extensions.getAppVersion
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class PosCtrlRepository @Inject constructor(
    val prefs: PreferencesSource,
    private val appContext: Context,
    private val xmlMapper: XmlMapper
) {
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
                    hostName = getDeviceIdentifier(),
                    listeningPort = (prefs.customPrefs()[appContext.getString(R.string.key_listen_port)]
                        ?: DEFAULT_LISTENING_PORT).toInt(),
                    time = getLocalTimeString()
                )
                Timber.d("receipt info $receiptInfo")
                val xmlMessage = xmlMapper.writeValueAsString(receiptInfo)
                val bytes = xmlMessage.toByteArray()
                val broadcastIp = "255.255.255.255"
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size,
                    InetAddress.getByName(broadcastIp),
                    port
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

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendLoginMessage(
        userId: String,
        password: String
    ): ResultWrapper<*> {
        withContext(Dispatchers.Default) {
            try {
                val loginBody = LoginBody(
                    appName = appContext.getString(R.string.app_name),
                    userId = userId,
                    hostName = getDeviceIdentifier(),
                    listeningPort = DEFAULT_LOGIN_LISTENING_PORT,//todo check if preference required/ settings item
                    time = getLocalTimeString(),
                    appVersion = appContext.getAppVersion(),
                    password = password
                )
                Timber.d("login body $loginBody")
                val xmlMessage = xmlMapper.writeValueAsString(loginBody)
                val bytes = xmlMessage.toByteArray()
                val ip =
                    prefs.defaultPrefs()[appContext.getString(R.string.key_login_server), ""]
                        ?: ""
                val port =
                    prefs.defaultPrefs()[appContext.getString(R.string.key_login_port), "0"]
                        ?: "0"
                Timber.d("ip $ip, port $port")
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port.toInt()))
                //  sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size,
                    InetAddress.getByName(ip),
                    port.toInt()
                )
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
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size, InetAddress.getByName(broadcastIp),
                    port
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
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size, InetAddress.getByName(broadcastIp),
                    port
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
                val appVersion = appContext.getAppVersion()
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
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size, InetAddress.getByName(broadcastIp),
                    port
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
                val appVersion = appContext.getAppVersion()
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
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size, InetAddress.getByName(broadcastIp),
                    port
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
        return "android-${
            Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }"
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
            withContext(Dispatchers.Default) {
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
                        val ac = AuthenticationContext(user, password?.toCharArray(), "")
                        val session: Session = connection.authenticate(ac)
                        fileNames.forEach { fullAddress ->
                            try {
                                val fileName = fullAddress.split("\\$sharedFolder\\").last()
                                (session.connectShare(sharedFolder) as? DiskShare?)?.let { share ->
                                    val s: MutableSet<SMB2ShareAccess> = HashSet()
                                    s.add(SMB2ShareAccess.FILE_SHARE_READ)
                                    val file = share.openFile(
                                        fileName,
                                        EnumSet.of(AccessMask.GENERIC_READ),
                                        null,
                                        s,
                                        SMB2CreateDisposition.FILE_OPEN,
                                        null
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
            val message = appContext.applicationContext.getString(R.string.error_no_snapshots)
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
                val port =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_port), DEFAULT_SERVER_PORT]
                        ?: DEFAULT_SERVER_PORT
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                    bytes,
                    bytes.size, InetAddress.getByName(broadcastIp),
                    port
                )
                sendSocket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDownloadsDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)//todo implement for API Q
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun downloadApk(): ResultWrapper<*> {
        var foundApk = true
        try {
            withContext(Dispatchers.Default) {
                val client = SMBClient()
                val server = prefs.customPrefs()[appContext.getString(R.string.key_server_path), ""]
                var sharedFolder =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_snapshot_path), ""]
                        ?: ""
                sharedFolder = sharedFolder.split("\\").last()
                Timber.d("shared: $server $sharedFolder ")
                val user = prefs.customPrefs()[appContext.getString(R.string.key_server_user), ""]
                val password =
                    prefs.customPrefs()[appContext.getString(R.string.key_server_password), ""]

                client.connect(server)
                    .use { connection ->
                        val ac = AuthenticationContext(user, password?.toCharArray(), "")
                        val session: Session = connection.authenticate(ac)

                        try {
                            //val fileName = fullAddress.split("\\$sharedFolder\\").last()
                            (session.connectShare(sharedFolder) as? DiskShare?)?.let { share ->
                                val s: MutableSet<SMB2ShareAccess> = HashSet()
                                s.add(SMB2ShareAccess.FILE_SHARE_READ)
                                val f = share.list("", "*.APK").firstOrNull()
                                f?.let {
                                    Timber.d("File : %s", f.fileName)
                                    val file = share.openFile(
                                        f.fileName,
                                        EnumSet.of(AccessMask.GENERIC_READ),
                                        null,
                                        s,
                                        SMB2CreateDisposition.FILE_OPEN,
                                        null
                                    )
                                    val inputStream = file.inputStream
                                    copyStreamToFile(inputStream)
                                } ?: {
                                    foundApk = false
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val message =
                                appContext.applicationContext.getString(R.string.error_download_update)
                            foundApk = false
                            return@withContext ResultWrapper.Error(message = message)
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val message = appContext.applicationContext.getString(R.string.error_download_update)
            foundApk = false
            return ResultWrapper.Error(message = message)
        }
        return if (foundApk) {
            ResultWrapper.Success("")
        } else {
            ResultWrapper.Error(appContext.getString(R.string.error_app_update))
        }

    }

    private fun copyStreamToFile(inputStream: InputStream) {
        val directory = appContext.getAppDirectory()
        val outputFile = File(directory, APK_FILE_NAME)
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }
//todo ask again for storage permissions after login if apk has to be downloaded

    companion object {
        const val ALIFE_DELAY_SECONDS = 60
        const val ALIFE_FILTER_DELAY_SECONDS = 60
        const val DEFAULT_SERVER_PORT = 11970
        const val DEFAULT_LISTENING_PORT = "20000"
        const val DEFAULT_LOGIN_LISTENING_PORT = 29998
        const val DEFAULT_FILTER_PORT = 29999
        const val APP_DIR = "/posctrl/"
        const val APK_FILE_NAME = "update.apk"
    }
}

class NoNetworkConnectionException : Exception()