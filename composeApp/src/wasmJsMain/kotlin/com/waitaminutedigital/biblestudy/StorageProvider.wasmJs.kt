package com.waitaminutedigital.biblestudy

import kotlinx.browser.window

actual object StorageProvider {
    actual fun getString(key: String, defaultValue: String): String {
        return try {
            window.localStorage.getItem(key) ?: defaultValue
        } catch (e: Throwable) {
            println("StorageProvider wasmJs getItem error: ${e.message}")
            defaultValue
        }
    }

    actual fun putString(key: String, value: String) {
        try {
            window.localStorage.setItem(key, value)
        } catch (e: Throwable) {
            println("StorageProvider wasmJs setItem error: ${e.message}")
        }
    }
}
