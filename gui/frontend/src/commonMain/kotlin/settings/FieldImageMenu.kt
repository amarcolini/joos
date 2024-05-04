package settings

import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.controls.text.TextField
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.darker
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.event.*
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.WidthSource
import util.BetterListLayout
import util.BetterViewBuilder

class FieldImageMenu(private val completed: ((String) -> Unit)) : BetterViewBuilder() {
    init {
        +Label("Enter URL:")
        +TextField("").apply {
            focusable = true
            this.minimumSize = Size(20.0, 20.0)
            this.enabled = true
            this.keyChanged += object : KeyListener {
                override fun pressed(event: KeyEvent) {
                    if (event.key == KeyText.Enter) completed.invoke(text)
                }
            }
            GUIApp.focusManager.requestFocus(this)
        }
        +PushButton("Submit").apply {
            pointerChanged += object : PointerListener {
                override fun pressed(event: PointerEvent) {
                    completed.invoke(text)
                }
            }
        }
        minimumSize = Size(200.0, 100.0)
        size = minimumSize
        val radius = 10.0
        insets = (10.0 to 10.0).run { Insets(first, second, first, second) }
        render = {
            rect(bounds.atOrigin, radius, Color.White.darker(0.2f).paint)
        }
        layout = BetterListLayout(widthSource = WidthSource.Parent)
        sizePreferencesChanged += { _, _, new ->
            size = new.idealSize ?: new.minimumSize
        }
        doLayout()
    }
}