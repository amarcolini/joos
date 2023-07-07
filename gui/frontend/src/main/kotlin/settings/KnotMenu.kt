package settings

import GUIApp.Companion.focusManager
import GUIApp.Companion.popupManager
import GUIApp.Companion.scheduler
import GUIApp.Companion.textMetrics
import field.DraggableTrajectory
import field.PathKnot
import field.SplineKnot
import io.nacular.doodle.controls.dropdown.Dropdown
import io.nacular.doodle.controls.popupmenu.Menu
import io.nacular.doodle.controls.popupmenu.MenuFactoryImpl
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.darker
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.ListLayout
import io.nacular.doodle.layout.WidthSource
import io.nacular.doodle.layout.constraints.Strength.Companion.Weak
import util.BetterViewBuilder.Companion.space
import util.BetterViewBuilder.Companion.viewBuilder
import util.TrajectoryMetadata
import kotlin.math.max

object KnotMenu {
    private infix fun Label.with(other: View) = space(this, other) { (label, control) ->
        parent.height.writable eq max(control.height.readOnly, label.height.readOnly)

        label.left eq 0.0
        label.centerY eq parent.centerY
        (control.right eq parent.right)..Weak
        control.centerY eq parent.centerY

        label.right lessEq control.left
        control.width.preserve
    }.apply {
        minimumSize = Size(
            this@with.minimumSize.width + other.minimumSize.width,
            max(this@with.minimumSize.height, other.minimumSize.height)
        )
        doLayout()
    }

    fun createPathKnotMenu(
        segment: TrajectoryMetadata.PieceWithData,
        knot: PathKnot,
        lengthMenu: Boolean = false,
        close: (Menu) -> Unit = {},
    ): View {
        val menus = MenuFactoryImpl(popupManager, scheduler, focusManager)
        return viewBuilder {
            +menus(close) {
                menu("Add") {
                    action("Spline") {
                        DraggableTrajectory.addSplineAfter(segment)
                    }
                    action("Line") {
                        DraggableTrajectory.addLineAfter(segment)
                    }
                }
                action("Delete") {
                    DraggableTrajectory.delete(segment)
                }
            }

            if (lengthMenu) {
                val lengthModeOptions = listOf("Match", "Fixed", "Free")
                +(Label("Length mode:") with Dropdown(lengthModeOptions).apply {
                    size = Size(
                        lengthModeOptions.maxOf { textMetrics.width(it) } + 40.0,
                        lengthModeOptions.maxOf { textMetrics.height(it) } + 10.0,
                    )
                    selection = when (knot.lengthMode) {
                        SplineKnot.LengthMode.MATCH_LENGTH -> 0
                        SplineKnot.LengthMode.FIXED_LENGTH -> 1
                        SplineKnot.LengthMode.FREE_LENGTH -> 2
                    }
                    this.changed += { dropdown ->
                        val lengthMode = when (dropdown.value.getOrNull()) {
                            "Match" -> SplineKnot.LengthMode.MATCH_LENGTH
                            "Fixed" -> SplineKnot.LengthMode.FIXED_LENGTH
                            "Free" -> SplineKnot.LengthMode.FREE_LENGTH
                            else -> null
                        }
                        lengthMode?.let { knot.lengthMode = it }
                    }
                })
            }

            minimumSize = Size(200.0, 100.0)
            size = minimumSize
            val radius = 10.0
            insets = (radius * 0.5 to 2.5).run { Insets(first, second, first, second) }
            render = {
                rect(bounds.atOrigin, radius, Color.White.darker(0.2f).paint)
            }
            layout = ListLayout(widthSource = WidthSource.Parent)
            sizePreferencesChanged += { _, _, new ->
                size = new.idealSize ?: new.minimumSize
            }
        }
    }
}