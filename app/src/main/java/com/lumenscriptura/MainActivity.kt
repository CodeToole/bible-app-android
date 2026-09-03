package com.lumenscriptura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase App Distribution In-App Update Alerts
        if (BuildConfig.DEBUG) {
            try {
                val appDistributionClass = Class.forName("com.google.firebase.appdistribution.FirebaseAppDistribution")
                val getInstanceMethod = appDistributionClass.getMethod("getInstance")
                val instance = getInstanceMethod.invoke(null)
                val updateMethod = appDistributionClass.getMethod("updateIfNewReleaseAvailable")
                val task = updateMethod.invoke(instance) as? com.google.android.gms.tasks.Task<*>
                task?.addOnFailureListener {
                    // Handle failure
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            val bibleService = remember { BibleService(applicationContext) }
            App(bibleService)
        }
    }
}
