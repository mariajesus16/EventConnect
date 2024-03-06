package com.example.eventconnect

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class LocaleHelper {

    companion object {
        const val PREF_LANG = "app_lang"

        fun setLocale(context: Context, language: String) {
            val locale = Locale(language)
            Locale.setDefault(locale)

            val resources = context.resources
            val configuration = resources.configuration
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()
            editor.putString(PREF_LANG, language)
            editor.apply()

            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        fun resetToDefault(context: Context) {
            setLocale(context, "en") // Restablece al idioma predeterminado en inglés
            recreateActivity(context)
        }

        private fun recreateActivity(context: Context) {
            if (context is AppCompatActivity) {
                val intent = Intent(context, context.javaClass)
                context.finish()
                context.startActivity(intent)
            }
        }

        private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

        fun persist(context: Context, language: String) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit().putString(SELECTED_LANGUAGE, language).apply()
        }

        fun getLanguage(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(SELECTED_LANGUAGE, null)
        }
    }
}
