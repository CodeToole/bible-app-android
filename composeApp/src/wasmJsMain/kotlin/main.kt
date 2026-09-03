import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.waitaminutedigital.biblestudy.App
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class, kotlin.js.ExperimentalWasmJsInterop::class)
fun main() {
    try {
        println("Starting WasmJs main initialization...")
        val target = document.getElementById("ComposeTarget") ?: document.body
        if (target == null) {
            println("ERROR: Neither 'ComposeTarget' nor 'document.body' found in DOM!")
            return
        }
        println("Container element located successfully. Mounting ComposeViewport...")
        ComposeViewport(target) {
            App()
        }
        println("ComposeViewport mount sequence initiated.")

        val loader = document.getElementById("AppLoader") as? HTMLElement
        if (loader != null) {
            window.setTimeout({
                loader.style.opacity = "0"
                window.setTimeout({
                    loader.remove()
                    println("HTML fallback loader removed from DOM.")
                    null
                }, 400)
                null
            }, 150)
        }
    } catch (e: Throwable) {
        println("FATAL ERROR in WasmJs main(): ${e.message}")
    }
}
