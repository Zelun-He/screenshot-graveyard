package com.zelun.screenshotgraveyard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import com.zelun.screenshotgraveyard.ui.ArchiveViewModel
import com.zelun.screenshotgraveyard.ui.GraveyardApp
import com.zelun.screenshotgraveyard.ui.GraveyardTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ArchiveViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(17, 16, 21)),
        )
        if (savedInstanceState == null) handleShare(intent)
        setContent { GraveyardTheme { GraveyardApp(viewModel) } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }
    private fun handleShare(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return
        if (intent.type?.startsWith("image/") != true) return
        val streams = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            else -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        }
        val uris = streams.ifEmpty {
            intent.clipData?.let { clip -> (0 until minOf(clip.itemCount, 20)).mapNotNull { clip.getItemAt(it).uri } }.orEmpty()
        }
        viewModel.import(uris)
        // Do not replay a consumed share on recreation.
        intent.action = Intent.ACTION_MAIN
        intent.removeExtra(Intent.EXTRA_STREAM)
        intent.clipData = null
    }
}
