import field.Field
import io.nacular.doodle.application.Application
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.KeyboardModule
import io.nacular.doodle.application.Modules.Companion.AccessibilityModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.Modules.Companion.PopupModule
import io.nacular.doodle.application.application
import io.nacular.doodle.controls.LazyPhoto
import io.nacular.doodle.controls.PopupManager
import io.nacular.doodle.controls.document.Document
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.Layout
import io.nacular.doodle.focus.FocusManager
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.scheduler.Scheduler
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.kodein.di.instance

class GUIApp(
    display: Display,
    imageLoader: ImageLoader,
    focusManager: FocusManager,
    scheduler: Scheduler,
    popupManager: PopupManager
) : Application {
    companion object {
        lateinit var focusManager: FocusManager
            private set
        lateinit var scheduler: Scheduler
            private set
        lateinit var popupManager: PopupManager
            private set
    }

    private val coroutineScope = MainScope()

    init {
        Companion.focusManager = focusManager
        Companion.scheduler = scheduler
        Companion.popupManager = popupManager
        coroutineScope.launch {
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

    override fun shutdown() {
        coroutineScope.cancel()
    }
}

fun main() {
    application(
        modules = listOf(
            ImageModule,
            PointerModule,
            FocusModule,
            AccessibilityModule,
            PopupModule,
//            KeyboardModule,
        )
    ) {
        GUIApp(instance(), instance(), instance(), instance(), instance())
    }
}