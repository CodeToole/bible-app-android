package com.waitaminutedigital.biblestudy

import android.content.Context
import android.content.SharedPreferences

actual object StorageProvider {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("biblestudy_prefs", Context.MODE_PRIVATE)
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }
}
