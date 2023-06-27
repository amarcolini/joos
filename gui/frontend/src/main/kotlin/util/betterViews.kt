package util

import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.focus.FocusTraversalPolicy
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.utils.ObservableList

open class BetterViewBuilder protected constructor() : View() {
    companion object {
        fun viewBuilder(lambda: BetterViewBuilder.() -> Unit) = BetterViewBuilder().apply { lambda() }
    }

    operator fun View.unaryPlus() {
        children += this
    }

    operator fun Collection<View>.unaryPlus() {
        children += this
    }

    public override var layout: Layout?
        get() = super.layout
        set(value) {
            super.layout = value
        }

    public override val children: ObservableList<View>
        get() = super.children

    public override var focusTraversalPolicy: FocusTraversalPolicy?
        get() = super.focusTraversalPolicy
        set(value) {
            super.focusTraversalPolicy = value
        }

    public override var insets: Insets
        get() = super.insets
        set(value) {
            super.insets = value
        }

    public override var isFocusCycleRoot: Boolean
        get() = super.isFocusCycleRoot
        set(value) {
            super.isFocusCycleRoot = value
        }

    @JsName("addedToDisplayLambda")
    var addedToDisplay = {}

    override fun addedToDisplay() {
        addedToDisplay.invoke()
    }

    @JsName("removedFromDisplayLambda")
    var removedFromDisplay = {}

    override fun removedFromDisplay() {
        removedFromDisplay.invoke()
    }

    @JsName("shouldYieldFocusLambda")
    var shouldYieldFocus = { true }

    override fun shouldYieldFocus(): Boolean = shouldYieldFocus.invoke()

    var render: (Canvas) -> Unit = {}

    override fun render(canvas: Canvas) {
        render.invoke(canvas)
    }

    var contains: (point: Point) -> Boolean = { super.contains(it) }

    override fun contains(point: Point): Boolean = contains.invoke(point)

    var intersects: (point: Point) -> Boolean = { super.intersects(it) }

    override fun intersects(point: Point): Boolean = intersects.invoke(point)

    public override fun doLayout() {
        super.doLayout()
    }

    public override fun ancestorOf(view: View): Boolean {
        return super.ancestorOf(view)
    }

    public override fun relayout() {
        super.relayout()
    }

    public override fun child(at: Point): View? {
        return super.child(at)
    }
}