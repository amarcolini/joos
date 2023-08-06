package style

import GUIApp
import io.nacular.doodle.controls.popupmenu.Menu
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.theme.Theme
import io.nacular.doodle.theme.basic.BasicMenuBehavior
import io.nacular.doodle.theme.basic.BasicTheme
import util.BetterBasicMenuBehavior
import util.NumberField

class DefaultTheme(var basicThemeConfig: BasicTheme.BasicThemeConfig) : Theme {

    override fun install(display: Display, all: Sequence<View>) = all.forEach { view ->
        when (view) {
//            is PathKnot -> it.behavior = object : Behavior<PathKnot> {
//            }
            is Menu -> if (view.parent != null && view.parent != display) {
                val config = basicThemeConfig.run {
                    BasicMenuBehavior.Config(
                        menuFillPaint = this.backgroundColor.paint,
                        itemTextPaint = this.foregroundColor.paint,
                        itemDisabledTextPaint = this.disabledPaintMapper(this.foregroundColor.paint),
                        subMenuIconPaint = this.foregroundColor.paint,
                        itemHighlightPaint = this.selectionColor.paint,
                        itemTextSelectedPaint = Color.White.paint,
                        subMenuIconSelectedPaint = Color.White.paint,
                        separatorPaint = this.darkBackgroundColor.paint,
                    )
                }
                view.behavior = BetterBasicMenuBehavior(
                    removeDropShadows = true, customWidth = true,
                    config
                )
            }
        }
    }
}