package com.example.smartspend

import android.content.Context
import android.preference.PreferenceManager
import java.util.Locale

    object LocaleHelper {

        private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

        fun setLocale(context: Context, language: String): Context {
            persist(context, language)
            return updateResources(context, language)
        }

        private fun persist(context: Context, language: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(SELECTED_LANGUAGE, language).apply()
        }

        fun onAttach(context: Context): Context {
            val lang = getPersistedData(context, Locale.getDefault().language)
            return setLocale(context, lang)
        }

        private fun getPersistedData(context: Context, defaultLanguage: String): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString(SELECTED_LANGUAGE, defaultLanguage) ?: defaultLanguage
        }

        private fun updateResources(context: Context, language: String): Context {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
