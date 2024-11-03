package com.example.smartspend

import android.content.Context
import android.preference.PreferenceManager
import android.content.ContextWrapper
import java.util.Locale

object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    /**
     * Sets the locale for the app and persists the selected language
     */
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    /**
     * Attaches the persisted locale to the context
     */
    fun onAttach(context: Context): Context {
        val lang = getPersistedData(context, Locale.getDefault().language)
        return setLocale(context, lang)
    }

    /**
     * Persists the selected language in SharedPreferences
     */
    private fun persist(context: Context, language: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
    }

    /**
     * Retrieves the persisted language or returns the default language
     */
    private fun getPersistedData(context: Context, defaultLanguage: String): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(SELECTED_LANGUAGE, defaultLanguage) ?: defaultLanguage
    }

    /**
     * Updates the app's resources with the selected locale
     */
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * Retrieves the current locale of the app
     */
    fun getLocale(context: Context): Locale {
        val config = context.resources.configuration
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.locales.get(0)
        } else {
            config.locale
        }
    }
}
