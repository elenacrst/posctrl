package `is`.posctrl.posctrl_android.data


import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.model.LoginResponse
import `is`.posctrl.posctrl_android.data.model.RegisterResponse
import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.ui.receipt.ReceiptAction
import android.content.Context
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.sql.DriverManager
import javax.inject.Inject

class PosCtrlRepository @Inject constructor() {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var xmlMapper: XmlMapper

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
        withContext(ioDispatcher) {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver")
                val connectionURL = "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
                Timber.e("connection url is $connectionURL")
                val connection = DriverManager.getConnection(connectionURL)
                val statement = connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UserLogin] \'$loginUser\', \'$loginPassword\'}")//called the procedure
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
                val connectionURL = "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
                Timber.e("connection url is $connectionURL")
                val connection = DriverManager.getConnection(connectionURL)
                val statement = connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UsersStores] \'$loggedInUser\'}")//called the procedure
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
            ResultWrapper.Error(message = context.applicationContext.getString(R.string.error_no_stores))
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
                val connectionURL = "jdbc:jtds:sqlserver://$server:$port/$DATABASE_NAME;instance=POSCTRL;user=$databaseUser;password=$databasePassword"
                Timber.e("connection url is $connectionURL")
                val connection = DriverManager.getConnection(connectionURL)
                val statement = connection.prepareCall("{call [PosCtrl-SelfService].dbo.[Settings.usp_UsersStoreRegisters] $storeNumber, \'$loggedInUser\'}")//called the procedure
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
            ResultWrapper.Error(message = context.applicationContext.getString(R.string.error_no_registers))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(Exception::class)
    suspend fun sendReceiptInfoMessage(
            loggedInUser: String = "",
            action: ReceiptAction = ReceiptAction.ALIFE,
            storeNumber: Int = -1,
            registerNumber: Int = -1/*, hostName: String, listeningPort: Int*/
    ): ResultWrapper<*> {
        withContext(Dispatchers.Default) {
            try {
                val message = "message"
                val bytes = message.toByteArray()
                val broadcastIp = "255.255.255.255"
                val port = 20000
                val sendSocket = DatagramSocket(null)
                sendSocket.reuseAddress = true
                sendSocket.bind(InetSocketAddress(port))
                sendSocket.broadcast = true
                val sendPacket = DatagramPacket(
                        bytes,
                        bytes.size, InetAddress.getByName(broadcastIp), port
                )
                sendSocket.send(sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ResultWrapper.Error()//todo connection failure
            }
        }

        return ResultWrapper.Success("")
    }
    //todo move get from prefs logic here instead of fragments/ view models

    companion object {
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
    }


}

class NoNetworkConnectionException : Exception()