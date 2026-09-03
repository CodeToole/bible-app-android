package com.waitaminutedigital.biblestudy

expect object StorageProvider {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
}
