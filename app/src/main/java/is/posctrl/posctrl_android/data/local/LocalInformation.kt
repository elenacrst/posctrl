package `is`.posctrl.posctrl_android.data.local

import javax.inject.Inject

class LocalInformation @Inject constructor(private val preferencesSource: PreferencesSource) :
    ILocalInformation {

    override fun saveUserId(userId: String) {
        preferencesSource.customPrefs()[KEY_USER_ID] = userId
    }

    override fun getUserId(): String {
        return preferencesSource.customPrefs()[KEY_USER_ID] ?: ""
    }

    override fun clearSharedPrefs() {
        preferencesSource.customPrefs().edit().clear().apply()
    }

    companion object {
        const val PREFERENCES_FILE_NAME = "preferences"
        const val KEY_USER_ID = "userId"
    }
}