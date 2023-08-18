package settings

import GUIApp.Companion.textMetrics
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.serialization.MovableTrajectoryPiece
import com.amarcolini.joos.util.deg
import field.HeadingKnot
import field.PathKnot
import field.SplineKnot
import io.nacular.doodle.controls.dropdown.Dropdown
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.darker
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.WidthSource
import io.nacular.doodle.layout.constraints.Strength.Companion.Weak
import util.BetterListLayout
import util.BetterViewBuilder.Companion.space
import util.BetterViewBuilder.Companion.viewBuilder
import util.NumberField
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
        customMenus: List<View>,
        knot: PathKnot,
        lengthMenu: Boolean = false,
    ): View {
        return viewBuilder {
            +customMenus

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
            layout = BetterListLayout(widthSource = WidthSource.Parent)
            sizePreferencesChanged += { _, _, new ->
                size = new.idealSize ?: new.minimumSize
            }
        }
    }

    fun createHeadingKnotMenu(
        customMenus: List<View>,
        knot: HeadingKnot,
        piece: MovableTrajectoryPiece
    ): View {
        return viewBuilder {
            +customMenus

            val numField = NumberField(
                (piece.heading as? ValueHeading)?.target?.degrees ?: 0.0,
                1, true, {
                    knot.tangent = it.deg
                }
            )
            val valueEditor = (Label("Target heading:") with numField).apply {
                visible = piece.heading is ValueHeading
            }

            fun updateValueEditor() {
                when (val heading = piece.heading) {
                    is ValueHeading -> {
                        numField.value = (heading.target.degrees)
                        valueEditor.visible = true
                    }

                    else -> {
                        valueEditor.visible = false
                    }
                }
            }

            val headingOptions = listOf("Tangent", "Spline", "Linear", "Constant")
            +(Label("Length mode:") with Dropdown(headingOptions).apply {
                size = Size(
                    headingOptions.maxOf { textMetrics.width(it) } + 40.0,
                    headingOptions.maxOf { textMetrics.height(it) } + 10.0,
                )
                selection = when (piece.heading) {
                    TangentHeading -> 0
                    is SplineHeading -> 1
                    is LinearHeading -> 2
                    ConstantHeading -> 3
                }
                this.changed += { dropdown ->
                    val currentHeading = numField.value.deg
                    val headingInterpolation = when (dropdown.value.getOrNull()) {
                        "Tangent" -> TangentHeading
                        "Spline" -> SplineHeading(currentHeading)
                        "Linear" -> LinearHeading(currentHeading)
                        "Constant" -> ConstantHeading
                        else -> null
                    }
                    headingInterpolation?.let { piece.heading = it }
                    updateValueEditor()
                }
            })

            +valueEditor

            minimumSize = Size(220.0, 100.0)
            size = minimumSize
            val radius = 10.0
            insets = (7.0 to 7.0).run { Insets(first, second, first, second) }
            render = {
                rect(bounds.atOrigin, radius, Color.White.darker(0.2f).paint)
            }
            layout = BetterListLayout(widthSource = WidthSource.Parent)
            sizePreferencesChanged += { _, _, new ->
                size = new.idealSize ?: new.minimumSize
            }
        }
    }
}