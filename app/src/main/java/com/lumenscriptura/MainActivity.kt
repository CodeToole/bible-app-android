package com.lumenscriptura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.lumenscriptura.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase App Distribution In-App Update Alerts
        if (BuildConfig.DEBUG) {
            FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
                .addOnFailureListener {
                    // Handle failure
                }
        }

        setContent {
            val bibleService = remember { BibleService(applicationContext) }
            App(bibleService)
        }
    }
}
