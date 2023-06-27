package field

import GUIApp
import io.nacular.doodle.core.View
import io.nacular.doodle.event.*
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Vector2D
import io.nacular.doodle.system.SystemPointerEvent

class Dragger(private val view: View) : PointerListener, PointerMotionListener {
    var allowOSConsume = true

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseMoved: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position.
     */
    var mousePressed: (Point, Set<SystemPointerEvent.Button>) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseEntered: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and whether the mouse is pressed.
     */
    var mouseExited: (Point, Boolean) -> Unit = { _, _ -> }

    /**
     * Accepts the mouse position and whether the mouse is within the view.
     */
    var mouseReleased: (Point, Boolean) -> Unit = { _, _ -> }

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

    private var currentEvent: PointerEvent? = null
    private fun allowConsume(event: PointerEvent, actions: () -> Unit) {
        currentEvent = event
        actions()
        currentEvent = null
    }

    fun consumeEvent() {
        currentEvent?.consume()
    }

    override fun released(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
        val activeInteraction = activeInteraction(event)
        if (activePointerChanged(event) && activeInteraction?.state == SystemPointerEvent.Type.Up) {
            captureInitialState(event)
            allowConsume(event) {
                mouseReleased(activeInteraction.location, view.contains(activeInteraction.location))
                stateChanged(activeInteraction.state)
            }
            if (consumedDrag) {
                event.consume()
                consumedDrag = false
            }
        }
    }

    override fun pressed(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
        if (activePointer == null || event.targetInteractions.find { it.pointer == activePointer } == null) {
            captureInitialState(event)
            if (view == event.target) {
                GUIApp.focusManager.requestFocus(view)
            }
            allowConsume(event) {
                mousePressed(event.location, event.buttons)
                stateChanged(SystemPointerEvent.Type.Down)
            }
        }
    }

    override fun entered(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            allowConsume(event) {
                mouseEntered(it.location, it.state == SystemPointerEvent.Type.Down)
                stateChanged(it.state)
            }
        }
    }

    override fun exited(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            allowConsume(event) {
                mouseExited(it.location, it.state == SystemPointerEvent.Type.Down)
                stateChanged(it.state)
            }
        }
    }

    override fun moved(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
        (activeInteraction(event) ?: event.changedInteractions.firstOrNull())?.let {
            allowConsume(event) {
                mouseMoved(it.location, it.state == SystemPointerEvent.Type.Down)
                stateChanged(it.state)
            }
        }
    }

    override fun dragged(event: PointerEvent) {
        if (!allowOSConsume) event.preventOsHandling()
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