import animation.ScrubBar
import field.Field
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Application
import io.nacular.doodle.application.Modules.Companion.AccessibilityModule
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.Modules.Companion.PopupModule
import io.nacular.doodle.application.application
import io.nacular.doodle.controls.PopupManager
import io.nacular.doodle.controls.panels.GridPanel
import io.nacular.doodle.controls.panels.SizingPolicy
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.Layout.Companion.simpleLayout
import io.nacular.doodle.core.View
import io.nacular.doodle.core.container
import io.nacular.doodle.drawing.TextMetrics
import io.nacular.doodle.focus.FocusManager
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.theme.Theme
import io.nacular.doodle.theme.ThemeManager
import io.nacular.doodle.theme.basic.BasicTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import settings.Settings
import util.BetterViewBuilder.Companion.viewBuilder
import util.FlexColumn
import util.FlexRow
import util.GROW

class GUIApp(
    display: Display,
    imageLoader: ImageLoader,
    focusManager: FocusManager,
    scheduler: Scheduler,
    animator: Animator,
    textMetrics: TextMetrics,
    popupManager: PopupManager,
    themeManager: ThemeManager,
    theme: Theme,
) : Application {
    companion object {
        lateinit var focusManager: FocusManager
            private set
        lateinit var scheduler: Scheduler
            private set
        lateinit var popupManager: PopupManager
            private set
        lateinit var animate: Animator
            private set
        lateinit var textMetrics: TextMetrics
            private set
    }

    private val coroutineScope = MainScope()

    init {
        Companion.focusManager = focusManager
        Companion.scheduler = scheduler
        Companion.popupManager = popupManager
        Companion.animate = animator
        Companion.textMetrics = textMetrics
        themeManager.selected = theme

        coroutineScope.launch {
            Field.backgrounds["Generic"] = imageLoader.load("/background/Generic.png")
        }
        display += viewBuilder {
            +Settings
            +viewBuilder {
                +Field
                +ScrubBar
                idealSize = GROW
                layout = FlexColumn()
            }
            layout = FlexRow()
        }
        display.layout = simpleLayout { container ->
            container.children.forEach {
                it.position = Point.Origin
                it.size = container.size
            }
        }
    }

    override fun shutdown() {
        coroutineScope.cancel()
    }
}

val AnimationModule = DI.Module(name = "AnimationModule") {
    bindProvider<Animator> { AnimatorImpl(timer = instance(), animationScheduler = instance()) }
}

fun main() {
    application(
        modules = listOf(
            ImageModule,
            PointerModule,
            FocusModule,
            AccessibilityModule,
            PopupModule,
            AnimationModule,
            BasicTheme.BasicTheme,
            BasicTheme.basicButtonBehavior(),
            BasicTheme.basicDropdownBehavior(),
            BasicTheme.basicLabelBehavior(),
//            KeyboardModule,
        )
    ) {
        GUIApp(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())
    }
}