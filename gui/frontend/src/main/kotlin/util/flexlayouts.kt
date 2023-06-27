package util

import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.Positionable
import io.nacular.doodle.core.PositionableContainer
import io.nacular.doodle.core.View
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import kotlin.math.min

val GROW = Size(1e6, 1e6)

class FlexColumn : Layout {
    override fun requiresLayout(
        child: Positionable,
        of: PositionableContainer,
        old: View.SizePreferences,
        new: View.SizePreferences
    ): Boolean = true

    override fun layout(container: PositionableContainer) {
        var y = container.insets.top
        val x = container.insets.left
        val w = container.width - container.insets.run { left + right }
        var remainingHeight = container.height - container.insets.run { top + bottom }
        val visibleChildren = container.children.mapNotNull {
            if (it.visible) it else null
        }

        val numChildrenCanIncrease = visibleChildren.size
        var minDecreaseAvailable = -1.0
        var numChildrenCanDecrease = 0

        val updateMinDecreaseInfo: (child: Positionable) -> Unit = { child ->
            if (child.height > child.minimumSize.height) {
                val decreaseAvailable = child.height - child.minimumSize.height
                minDecreaseAvailable =
                    if (minDecreaseAvailable < 0.0) decreaseAvailable else min(minDecreaseAvailable, decreaseAvailable)
                ++numChildrenCanDecrease
            }
        }

        visibleChildren.forEach { child ->
            val h = child.idealSize?.height ?: child.minimumSize.height
            child.bounds = Rectangle(x, y, w, h)
            y += h
            remainingHeight -= h
            updateMinDecreaseInfo(child)
        }

        while (remainingHeight != 0.0) {
            y = container.insets.top

            when {
                remainingHeight > 0.0 -> {
                    val proportionalIncrease = remainingHeight / numChildrenCanIncrease
                    visibleChildren.forEach { child ->
                        child.bounds = Rectangle(child.x, y, child.width, child.height + proportionalIncrease)
                        y += child.height
                    }
                    remainingHeight = 0.0
                }

                remainingHeight < 0.0 -> {
                    when {
                        numChildrenCanDecrease > 0 -> {
                            val proportionalDecrease =
                                min(minDecreaseAvailable, -remainingHeight / numChildrenCanDecrease)
                            remainingHeight += proportionalDecrease * numChildrenCanDecrease
                            numChildrenCanDecrease = 0
                            minDecreaseAvailable = -1.0

                            visibleChildren.forEach { child ->
                                when {
                                    child.height > child.minimumSize.height -> {
                                        child.bounds =
                                            Rectangle(child.x, y, child.width, child.height - proportionalDecrease)
                                        updateMinDecreaseInfo(child)
                                    }
                                    else -> child.bounds = Rectangle(child.x, y, child.width, child.height)
                                }
                                y += child.height
                            }
                        }
                        else -> break
                    }
                }
            }
        }
//        visibleChildren.forEach(PrePositionable::updateBounds)
    }
}

class FlexRow : Layout {
    override fun requiresLayout(
        child: Positionable,
        of: PositionableContainer,
        old: View.SizePreferences,
        new: View.SizePreferences
    ): Boolean = true

    override fun layout(container: PositionableContainer) {
        var x = container.insets.top
        val y = container.insets.left
        val h = container.height - container.insets.run { top + bottom }
        var remainingWidth = container.width - container.insets.run { left + right }
        val visibleChildren = container.children.mapNotNull {
            if (it.visible) (it) else null
        }

        val numChildrenCanIncrease = visibleChildren.size
        var minDecreaseAvailable = -1.0
        var numChildrenCanDecrease = 0

        val updateMinDecreaseInfo: (child: Positionable) -> Unit = { child ->
            if (child.width > child.minimumSize.width) {
                val decreaseAvailable = child.width - child.minimumSize.width
                minDecreaseAvailable =
                    if (minDecreaseAvailable < 0.0) decreaseAvailable else min(minDecreaseAvailable, decreaseAvailable)
                ++numChildrenCanDecrease
            }
        }

        visibleChildren.forEach { child ->
            val w = child.idealSize?.width ?: child.minimumSize.width
            child.bounds = Rectangle(x, y, w, h)
            x += w
            remainingWidth -= w

            updateMinDecreaseInfo(child)
        }

        while (remainingWidth != 0.0) {
            x = container.insets.left

            when {
                remainingWidth > 0.0 -> {
                    val proportionalIncrease = remainingWidth / numChildrenCanIncrease
                    visibleChildren.forEach { child ->
                        child.bounds = Rectangle(x, child.y, child.width + proportionalIncrease, child.height)
                        x += child.width
                    }
                    remainingWidth = 0.0
                }

                remainingWidth < 0.0 -> {
                    when {
                        numChildrenCanDecrease > 0 -> {
                            val proportionalDecrease =
                                min(minDecreaseAvailable, -remainingWidth / numChildrenCanDecrease)
                            remainingWidth += proportionalDecrease * numChildrenCanDecrease
                            numChildrenCanDecrease = 0
                            minDecreaseAvailable = -1.0

                            visibleChildren.forEach { child ->
                                when {
                                    child.width > child.minimumSize.width -> {
                                        child.bounds =
                                            Rectangle(x, child.y, child.width - proportionalDecrease, child.height)
                                        updateMinDecreaseInfo(child)
                                    }
                                    else -> child.bounds = Rectangle(x, child.y, child.width, child.height)
                                }
                                x += child.width
                            }
                        }
                        else -> break
                    }
                }
            }
        }
    }
}