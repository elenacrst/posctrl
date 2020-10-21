package `is`.posctrl.posctrl_android.data


import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PosCtrlRepository @Inject constructor() {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Throws(Exception::class)
    suspend fun login(): ResultWrapper<*> {

        withContext(ioDispatcher) {
            try {
                //todo call stored procedure
            } catch (e: Exception) {
                e.printStackTrace()
                throw NoNetworkConnectionException()
            }
        }

        return ResultWrapper.Success("")//todo return response
    }
}

class NoNetworkConnectionException : Exception()