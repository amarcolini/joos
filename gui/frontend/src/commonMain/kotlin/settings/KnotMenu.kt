package settings

import GUIApp.Companion.textMetrics
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.serialization.MovableTrajectoryPiece
import com.amarcolini.joos.util.deg
import field.SplineKnot
import io.nacular.doodle.controls.dropdown.SelectBox
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

internal object KnotMenu {
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
        knot: SplineKnot,
        piece: MovableTrajectoryPiece?,
        lengthMenu: Boolean = false,
    ): View {
        return viewBuilder {
            +customMenus
            if (lengthMenu) {
                val lengthModeOptions = listOf("Match", "Fixed", "Free")
                +(Label("Length mode:") with SelectBox(lengthModeOptions).apply {
                    size = Size(
                        lengthModeOptions.maxOf { textMetrics.width(it) } + 40.0,
                        lengthModeOptions.maxOf { textMetrics.height(it) } + 10.0,
                    )
                    selection = when (knot.lengthMode) {
                        SplineKnot.LengthMode.MATCH_LENGTH -> 0
                        SplineKnot.LengthMode.FIXED_LENGTH -> 1
                        SplineKnot.LengthMode.FREE_LENGTH -> 2
                    }
                    this.changed += { selectBox ->
                        val lengthMode = when (selectBox.value.getOrNull()) {
                            "Match" -> SplineKnot.LengthMode.MATCH_LENGTH
                            "Fixed" -> SplineKnot.LengthMode.FIXED_LENGTH
                            "Free" -> SplineKnot.LengthMode.FREE_LENGTH
                            else -> null
                        }
                        lengthMode?.let { knot.lengthMode = it }
                    }
                })
            }

            if (piece != null) {
                val headingOptions = listOf("Tangent", "Spline", "Linear", "Constant")
                +(Label("Interpolation mode:") with SelectBox(headingOptions).apply {
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
                    this.changed += { selectBox ->
                        val currentHeading = knot.heading
                        val headingInterpolation = when (selectBox.value.getOrNull()) {
                            "Tangent" -> TangentHeading
                            "Spline" -> SplineHeading(currentHeading)
                            "Linear" -> LinearHeading(currentHeading)
                            "Constant" -> ConstantHeading
                            else -> null
                        }
                        headingInterpolation?.let { piece.heading = it }
                    }
                })
            }

//            +PushButton("hello :)")
//            +PushButton("what's your name?")
//            val values = listOf("nunya business", "my name's ___", "what's that over there?")
//            +SelectBox(values).apply {
//                size = Size(
//                    values.maxOf { textMetrics.width(it) } + 40.0,
//                    values.maxOf { textMetrics.height(it) } + 10.0,
//                )
//                println(size)
//            }

            minimumSize = Size(200.0, 100.0)
            size = minimumSize
            val radius = 10.0
            insets = (radius * 0.5 to 2.5).run { Insets(first, second, first, second) }
            render = {
                rect(bounds.atOrigin, radius, Color.White.darker(0.2f).paint)
            }
            layout = BetterListLayout(widthSource = WidthSource.Parent)
//            sizePreferencesChanged += { _, _, new ->
//                size = new.idealSize ?: new.minimumSize
//            }
        }
    }
}