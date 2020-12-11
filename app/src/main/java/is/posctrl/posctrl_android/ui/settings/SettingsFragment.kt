package `is`.posctrl.posctrl_android.ui.settings

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.get
import `is`.posctrl.posctrl_android.di.ActivityModule
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var prefs: PreferencesSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_app, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<EditTextPreference>(getString(R.string.key_login_server))?.title =
            prefs.defaultPrefs()["title_database_server", getString(R.string.title_database_server)]
                ?: getString(R.string.title_database_server)
        findPreference<EditTextPreference>(getString(R.string.key_login_server))?.dialogTitle =
            prefs.defaultPrefs()["dialog_database_server", getString(R.string.dialog_database_server)]
                ?: getString(R.string.dialog_database_server)
        findPreference<EditTextPreference>(getString(R.string.key_login_port))?.title =
            prefs.defaultPrefs()["title_server_port", getString(R.string.title_server_port)]
                ?: getString(R.string.title_server_port)
        findPreference<EditTextPreference>(getString(R.string.key_login_port))?.dialogTitle =
            prefs.defaultPrefs()["title_server_port", getString(R.string.title_server_port)]
                ?: getString(R.string.title_server_port)
        findPreference<PreferenceCategory>(getString(R.string.key_header_settings))?.title =
            prefs.defaultPrefs()["header_settings", getString(R.string.header_settings)]
                ?: getString(R.string.header_settings)
    }

    override fun onAttach(context: Context) {
        (context.applicationContext as PosCtrlApplication).appComponent.activityComponent(
            ActivityModule(requireActivity())
        ).inject(this)
        super.onAttach(context)
    }
}