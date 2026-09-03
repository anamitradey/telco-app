package com.telco.safetysdk.internal

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    val sp: SharedPreferences =
        context.getSharedPreferences("safety_sdk", Context.MODE_PRIVATE)

    fun getString(key: String, def: String? = null): String? = sp.getString(key, def)
    fun putString(key: String, value: String?) = sp.edit().putString(key, value).apply()
    fun getLong(key: String, def: Long = 0L) = sp.getLong(key, def)
    fun putLong(key: String, value: Long) = sp.edit().putLong(key, value).apply()
    fun getBool(key: String, def: Boolean = false) = sp.getBoolean(key, def)
    fun putBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun getInt(key: String, def: Int = 0) = sp.getInt(key, def)
    fun putInt(key: String, value: Int) = sp.edit().putInt(key, value).apply()
    fun getStringSet(key: String): Set<String> = sp.getStringSet(key, emptySet()) ?: emptySet()
    fun putStringSet(key: String, value: Set<String>) = sp.edit().putStringSet(key, value).apply()
}
