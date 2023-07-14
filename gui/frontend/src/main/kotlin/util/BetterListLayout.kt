package util

import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.Positionable
import io.nacular.doodle.core.PositionableContainer
import io.nacular.doodle.core.View.SizePreferences
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.WidthSource
import io.nacular.doodle.layout.WidthSource.Children
import io.nacular.doodle.layout.WidthSource.Parent
import kotlin.math.max

class BetterListLayout(private val spacing: Double = 0.0, private val widthSource: WidthSource = Children) : Layout {

    constructor(spacing: Int, widthSource: WidthSource = Children) : this(spacing.toDouble(), widthSource)

    override fun requiresLayout(
        child: Positionable,
        of: PositionableContainer,
        old: SizePreferences,
        new: SizePreferences
    ): Boolean = widthSource != Parent && old.idealSize != new.idealSize

    override fun layout(container: PositionableContainer) {
        // TODO: Can this be cleaned up to use idealSize?
        val insets = container.insets
        var y = insets.top

        val width = when (widthSource) {
            Parent -> container.width
            else -> container.children.filter { it.visible }.maxOfOrNull { it.idealSize?.width ?: it.width } ?: 0.0
        }

        var i = 0

        container.children.filter { it.visible }.forEach {
            val height = it.idealSize?.height ?: max(it.height, it.minimumSize.height)
            it.bounds = Rectangle(insets.left, y, max(0.0, width - (insets.left + insets.right)), height)

            y += height + if (++i < container.children.size) spacing else 0.0
        }

        val size = Size(if (widthSource == Children) width + insets.left + insets.right else width, y + insets.bottom)

        container.idealSize = size
        if (widthSource == Children) container.minimumSize = size
    }

    override fun idealSize(container: PositionableContainer, default: Size?): Size {
        val insets = container.insets
        var y = insets.top

        var width = when (widthSource) {
            Parent -> container.width
            else -> container.children.firstOrNull()?.let { it.idealSize?.width ?: it.width } ?: 0.0
        }

        var i = 0

        container.children.filter { it.visible }.forEach {
            if (widthSource == Children) {
                width = max(width, (it.idealSize?.width ?: it.width) + insets.left + insets.right)
            }

            y += it.height + if (++i < container.children.size) spacing else 0.0
        }

        return Size(width, y + insets.bottom)
    }
}