package `is`.posctrl.posctrl_android.data.local

interface ILocalInformation {
    fun saveUserId(userId: String)
    fun getUserId(): String
    fun clearSharedPrefs()

}