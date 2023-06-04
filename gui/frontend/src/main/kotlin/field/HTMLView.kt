package field

import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.geometry.Size
import kotlinx.browser.window
import org.w3c.dom.Element

abstract class HTMLView(private val id: String = "$prefix${count++}") : View() {
    companion object {
        private var count = 0
        const val prefix = "142743"
    }

    private val label = Label().apply {
        accessibilityLabel = id
        size = Size.Empty
        visible = true
    }

    init {
        children += label
    }

    val htmlElement: Element? get() = window.document.querySelector("div[aria-label='$id']")?.parentElement
}