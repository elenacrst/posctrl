package `is`.posctrl.posctrl_android.data.model

data class LoginResponse(
        //val username: String?,
        //val userRights : Int?,
        val errorMessage: String?,
        val serverPath: String?,
        val serverPort: String?,
        val filterRespondTime: Int?,
        val appVersion: String?,
        val serverUser: String?,
        val serverUserDomain: String?,
        val serverUserPassword: String?,
        val serverSnapshotPath: String?
)