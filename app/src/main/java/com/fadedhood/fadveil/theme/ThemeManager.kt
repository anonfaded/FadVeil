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
            
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        activity.setTheme(
            if (enabled) R.style.Theme_Fadveil_Amoled 
            else R.style.Theme_Fadveil
        )
        
        activity.recreate()
    }
    
    fun isAmoledMode(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_AMOLED_MODE, false)
    }

    fun applyTheme(activity: AppCompatActivity) {
        AppCompatDelegate.setDefaultNightMode(
            if (isAmoledMode(activity)) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        activity.setTheme(
            if (isAmoledMode(activity)) R.style.Theme_Fadveil_Amoled 
            else R.style.Theme_Fadveil
        )
    }
}