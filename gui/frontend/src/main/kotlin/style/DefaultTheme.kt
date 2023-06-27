package style

import field.PathKnot
import field.SplineKnot
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.View
import io.nacular.doodle.theme.Theme

class DefaultTheme : Theme {

    override fun install(display: Display, all: Sequence<View>) = all.forEach {
        when (it) {
            is PathKnot -> it.behavior = object : Behavior<PathKnot> {
            }
        }
    }
}