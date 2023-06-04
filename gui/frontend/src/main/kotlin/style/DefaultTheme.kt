package style

import field.SplineKnot
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.View
import io.nacular.doodle.theme.Theme

class DefaultTheme : Theme {

    override fun install(display: Display, all: Sequence<View>) = all.forEach {
        when (it) {
            is SplineKnot -> it.behavior = object : Behavior<SplineKnot> {
            }
        }
    }
}