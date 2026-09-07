package de.mogeldev.keitaihotspot

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/**
 * Verwaltet die manuell gewählte App-Sprache, unabhängig von der Systemsprache.
 * "system" bedeutet: Systemsprache verwenden (kein Override).
 */
object LocaleHelper {
    private const val PREFS_NAME = "keitai_hotspot_prefs"
    private const val KEY_LANGUAGE = "app_language"

    // Reihenfolge bestimmt die Anzeige-Reihenfolge im Auswahl-Dialog.
    // Für eine weitere Sprache hier einfach den Sprachcode ergänzen und in nativeName() eintragen.
    val supportedLanguageCodes = listOf("system", "en", "de", "ja")

    fun nativeName(code: String): String = when (code) {
        "en" -> "English"
        "de" -> "Deutsch"
        "ja" -> "日本語"
        else -> code
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun setSelectedLanguage(context: Context, code: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, code)
            .apply()
    }

    fun wrap(context: Context): ContextWrapper {
        val code = getSelectedLanguage(context)
        if (code == "system") {
            return ContextWrapper(context)
        }
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return ContextWrapper(context.createConfigurationContext(config))
    }
}
