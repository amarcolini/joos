package field

import io.nacular.doodle.core.View
import io.nacular.doodle.event.*
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Vector2D
import io.nacular.doodle.system.SystemPointerEvent

class Dragger(private val view: View) : PointerListener, PointerMotionListener {
    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseMoved: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position.
     */
    var mouseDown: (Point) -> Unit = {}

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseEntered: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseExited: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseUp: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and position delta.
     */
    var mouseDragged: (Point, Vector2D) -> Unit = { _, _ -> }

    /**
     * Accepts the current state of the mouse.
     */
    var stateChanged: (SystemPointerEvent.Type) -> Unit = {}

    init {
        view.pointerChanged += this
        view.pointerMotionChanged += this
    }

    private var initialPosition = Point.Origin
    private var activePointer = null as Pointer?
    private var consumedDrag = false

    override fun released(event: PointerEvent) {
        val activeInteraction = activeInteraction(event)
        if (activePointerChanged(event) && activeInteraction?.state == SystemPointerEvent.Type.Up) {
            captureInitialState(event)
            mouseUp(activeInteraction.location, view.contains(activeInteraction.location))
            stateChanged(activeInteraction.state)
            if (consumedDrag) {
                event.consume()
                consumedDrag = false
            }
        }
    }

    override fun pressed(event: PointerEvent) {
        if (activePointer == null || event.targetInteractions.find { it.pointer == activePointer } == null) {
            captureInitialState(event)
            GUIApp.focusManager.requestFocus(view)
            mouseDown(event.location)
            stateChanged(SystemPointerEvent.Type.Down)
        }
    }

    override fun entered(event: PointerEvent) {
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            mouseEntered(it.location, it.state == SystemPointerEvent.Type.Down)
            stateChanged(it.state)
        }
    }

    override fun exited(event: PointerEvent) {
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            mouseExited(it.location, it.state == SystemPointerEvent.Type.Down)
            stateChanged(it.state)
        }
    }

    override fun moved(event: PointerEvent) {
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            mouseMoved(it.location, it.state == SystemPointerEvent.Type.Down)
            stateChanged(it.state)
        }
    }

    override fun dragged(event: PointerEvent) {
        event.changedInteractions.find { it.pointer == activePointer }?.let { activeInteraction ->
            val currentPos = view.toLocal(activeInteraction.location, event.target)
            val delta = currentPos - initialPosition
            mouseDragged(currentPos, delta)
            stateChanged(activeInteraction.state)
            event.consume()
            consumedDrag = true
        }
    }

    private fun activePointerChanged(event: PointerEvent): Boolean = activeInteraction(event) != null

    private fun activeInteraction(event: PointerEvent): Interaction? =
        event.changedInteractions.find { it.pointer == activePointer }

    private fun captureInitialState(event: PointerEvent) {
        activePointer = null

        val interaction =
            event.targetInteractions.firstOrNull { it.state == SystemPointerEvent.Type.Down || it.state == SystemPointerEvent.Type.Drag }
                ?: return

        activePointer = interaction.pointer
        initialPosition = view.toLocal(interaction.location, event.target)
    }
}