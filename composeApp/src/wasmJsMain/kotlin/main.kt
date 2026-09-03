import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.waitaminutedigital.biblestudy.App
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val canvas = document.getElementById("ComposeTarget") ?: return
    ComposeViewport(canvas) {
        App()
    }
}
