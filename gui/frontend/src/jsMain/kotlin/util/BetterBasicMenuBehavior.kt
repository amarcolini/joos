package util

import GUIApp
import io.nacular.doodle.controls.popupmenu.Menu
import io.nacular.doodle.controls.popupmenu.MenuBehavior
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.theme.basic.BasicMenuBehavior
import io.nacular.doodle.utils.PropertyObserver
import io.nacular.measured.units.Measure
import io.nacular.measured.units.Time

class BetterBasicMenuBehavior(
    var removeDropShadows: Boolean = false,
    var customWidth: Boolean = false,
    var basicMenuBehaviorConfig: BasicMenuBehavior.Config,
    val delegate: MenuBehavior =
        BasicMenuBehavior(
            GUIApp.textMetrics, GUIApp.pathMetrics, config = basicMenuBehaviorConfig
        )
) : MenuBehavior() {
    override fun childrenClipPath(view: Menu): View.ClipPath? = delegate.childrenClipPath(view)

    override fun clipCanvasToBounds(view: Menu): Boolean = delegate.clipCanvasToBounds(view)

    override fun contains(view: Menu, point: Point): Boolean = delegate.contains(view, point)

    private val listener: PropertyObserver<View, Rectangle> = { view, _, _ ->
        if (customWidth) {
            val temp = view.foregroundColor
            view.foregroundColor = Color(1u, 1u, 2u)
            view.foregroundColor = temp
        }
    }

    override fun install(view: Menu) {
        view.boundsChanged += listener
        delegate.install(view)
    }

    override fun mirrorWhenRightToLeft(view: Menu): Boolean = delegate.mirrorWhenRightToLeft(view)

    override fun render(view: Menu, canvas: Canvas) {
        if (removeDropShadows) canvas.rect(
            view.bounds.atOrigin,
            radius = basicMenuBehaviorConfig.menuRadius,
            fill = basicMenuBehaviorConfig.menuFillPaint
        ) else delegate.render(view, canvas)
    }

    override fun uninstall(view: Menu) {
        view.boundsChanged -= listener
        delegate.uninstall(view)
    }

    private inner class DelegateConfig<T : ItemInfo>(private val menu: Menu, private val delegate: ItemConfig<T>) :
        ItemConfig<T> by delegate {
        override fun preferredSize(item: T): Size = delegate.preferredSize(item).run {
            Size(if (customWidth) (menu.width - menu.insets.right) else width, height)
        }
    }

    override fun actionConfig(menu: Menu): ItemConfig<ActionItemInfo> =
        DelegateConfig(menu, delegate.actionConfig(menu))

    override fun promptConfig(menu: Menu): ItemConfig<ActionItemInfo> =
        DelegateConfig(menu, delegate.promptConfig(menu))

    override fun separatorConfig(menu: Menu): SeparatorConfig = delegate.separatorConfig(menu)

    override fun subMenuConfig(menu: Menu): SubMenuConfig {
        val delegateConfig = delegate.subMenuConfig(menu)

        return object : ItemConfig<SubMenuInfo> by DelegateConfig(menu, delegateConfig), SubMenuConfig {
            override val showDelay: Measure<Time>
                get() = delegateConfig.showDelay
        }
    }
}