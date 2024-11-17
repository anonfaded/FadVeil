package com.fadedhood.fadveil.theme

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.fadedhood.fadveil.R

object ThemeManager {
    private const val PREF_AMOLED_MODE = "amoled_mode"
    
    fun setAmoledMode(activity: AppCompatActivity, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(activity)
            .edit()
            .putBoolean(PREF_AMOLED_MODE, enabled)
            .apply()
            
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        
        if (enabled) {
            activity.setTheme(R.style.Theme_Fadveil_Amoled)
        } else {
            activity.setTheme(R.style.Theme_Fadveil)
        }
        
        activity.recreate()
    }
    
    fun isAmoledMode(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_AMOLED_MODE, false)
    }

    fun applyTheme(activity: AppCompatActivity) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        
        if (isAmoledMode(activity)) {
            activity.setTheme(R.style.Theme_Fadveil_Amoled)
        } else {
            activity.setTheme(R.style.Theme_Fadveil)
        }
    }
}