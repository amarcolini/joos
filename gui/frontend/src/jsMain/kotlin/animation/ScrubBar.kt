package animation

import GUIApp
import com.amarcolini.joos.serialization.format
import field.DraggableTrajectory
import field.Field
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.*
import io.nacular.doodle.event.PointerMotionListener
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.system.SystemPointerEvent
import kotlinx.browser.window
import kotlin.math.min

object ScrubBar : View() {
    var renderWidth: Double = width
        private set

    init {
        visible = false
        minimumSize = Size(100.0, 40.0)
        height = minimumSize.height
        TimeManager.listeners += { _, _ ->
            rerenderNow()
        }
        Field.boundsChanged += { _, _, new ->
            renderWidth = min(new.width, new.height)
        }
        window.asDynamic()["hmm"] = {
            val percent = TimeManager.time / TimeManager.duration
            "time: ${TimeManager.time}, percent: $percent, pose: ${DraggableTrajectory.currentPath[percent * DraggableTrajectory.currentPath.length()]}"
        }

        pointerMotionChanged += PointerMotionListener.dragged {
            if (SystemPointerEvent.Button.Button2 !in it.buttons) {
                val offset = (width - renderWidth) / 2
                TimeManager.setTime(
                    TimeManager.duration * (it.location.x.coerceIn(
                        offset,
                        renderWidth + offset
                    ) - offset) / renderWidth,
                    false
                )
            }
        }
    }

    override fun render(canvas: Canvas) {
        canvas.clear()
        val rectPoint = Point((width - renderWidth) / 2, 0.0)
        canvas.rect(Rectangle(rectPoint, Size(renderWidth, height)), 5.0, Color.White.darker(0.2f).paint)
        canvas.rect(
            Rectangle(rectPoint, Size((TimeManager.time / TimeManager.duration) * renderWidth, height)),
            5.0,
            Color.Blue.lighter(0.25f).paint
        )
        val string = "${TimeManager.time.format(1)} / ${TimeManager.duration.format(1)}"
        val stringSize = GUIApp.textMetrics.size(string)
        canvas.text(string, Point(width - stringSize.width, height - stringSize.height) * 0.5, Color.Black.paint)
    }
}