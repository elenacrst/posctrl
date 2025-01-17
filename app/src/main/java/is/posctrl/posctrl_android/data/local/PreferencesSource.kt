package `is`.posctrl.posctrl_android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import javax.inject.Inject

class PreferencesSource @Inject constructor(private val context: Context) {

    internal fun customPrefs(): SharedPreferences = context.getSharedPreferences(
            PREFERENCES_FILE_NAME,
            Context.MODE_PRIVATE
    )

    internal fun defaultPrefs(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val PREFERENCES_FILE_NAME = "logged_in_preferences"
    }
}

inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
    val editor = this.edit()
    operation(editor)
    editor.apply()
}

fun SharedPreferences.clear() {
    edit {
        it.clear()
    }
}

/**
 * puts a key value pair in shared prefs if doesn't exists,
 * otherwise updates value on given [key]
 */
operator fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value) }
        is Long -> edit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

/**
 * finds value on given key.
 * [T] is the type of value
 * @param defaultValue optional default value - will take null for strings,
 * false for bool and -1 for numeric values if [defaultValue] is not specified
 */
inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        defaultValue: T? = null
): T? {
    return when (T::class) {
        String::class -> getString(key, defaultValue as? String?) as T?
        Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
        Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

class UnsupportedOperationException(message: String) : Exception(message)