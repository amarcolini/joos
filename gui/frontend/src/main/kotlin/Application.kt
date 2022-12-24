import field.Field
import io.nacular.doodle.application.Application
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.KeyboardModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.application
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.plusAssign
import io.nacular.doodle.core.view
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.text
import io.nacular.doodle.focus.FocusManager
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.layout.HorizontalFlowLayout
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.utils.ObservableSet
import io.nacular.doodle.utils.PropertyObserver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.instance

@OptIn(DelicateCoroutinesApi::class)
class GUIApp(
    display: Display,
    imageLoader: ImageLoader,
    focusManager: FocusManager,
    scheduler: Scheduler
) : Application {
    companion object {
        lateinit var focusManager: FocusManager
            private set
        lateinit var scheduler: Scheduler
            private set
    }

    init {
        Companion.focusManager = focusManager
        Companion.scheduler = scheduler
        GlobalScope.launch {
            Field.backgrounds["Generic"] = imageLoader.load("/background/Generic.png")
        }

        Field.size = display.size
        display.layout = Layout.simpleLayout { container ->
            container.children.forEach {
                it.size = container.size
            }
        }
        display += Field
    }

    override fun shutdown() {}
}

fun main() {
    application(
        modules = listOf(
            ImageModule,
            PointerModule,
            FocusModule,
            KeyboardModule,
        )
    ) {
        GUIApp(instance(), instance(), instance(), instance())
    }
}