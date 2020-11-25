package `is`.posctrl.posctrl_android.ui.settings

import `is`.posctrl.posctrl_android.R
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_app, rootKey)
    }

}