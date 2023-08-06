import animation.ScrubBar
import field.Field
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Application
import io.nacular.doodle.application.Modules.Companion.AccessibilityModule
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.ModalModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.Modules.Companion.PopupModule
import io.nacular.doodle.application.application
import io.nacular.doodle.controls.PopupManager
import io.nacular.doodle.controls.modal.ModalManager
import io.nacular.doodle.controls.popupmenu.MenuFactory
import io.nacular.doodle.controls.popupmenu.MenuFactoryImpl
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.Layout.Companion.simpleLayout
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.TextMetrics
import io.nacular.doodle.focus.FocusManager
import io.nacular.doodle.geometry.PathMetrics
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.geometry.impl.PathMetricsImpl
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.theme.Theme
import io.nacular.doodle.theme.ThemeManager
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import io.nacular.doodle.theme.plus
import io.nacular.doodle.utils.fastSetOf
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.kodein.di.*
import settings.Settings
import style.DefaultTheme
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
    pathMetrics: PathMetrics,
    popupManager: PopupManager,
    modalManager: ModalManager,
    themeManager: ThemeManager,
    basicTheme: BasicTheme,
    nativeTheme: NativeTheme,
) : Application {
    companion object {
        lateinit var focusManager: FocusManager
            private set
        lateinit var scheduler: Scheduler
            private set
        lateinit var popupManager: PopupManager
            private set
        lateinit var modalManager: ModalManager
            private set
        lateinit var animate: Animator
            private set
        lateinit var textMetrics: TextMetrics
            private set
        lateinit var pathMetrics: PathMetrics
            private set
        lateinit var appScope: CoroutineScope
            private set
        lateinit var menus: MenuFactory
            private set
        lateinit var imageLoader: ImageLoader
            private set
        private lateinit var themeManager: ThemeManager
        private lateinit var display: Display

        const val fieldImageKey = "fieldImageURL"
        const val trajectoryKey = "currentTrajectory"

        fun fakeTheme(view: View) {
            themeManager.selected?.install(display, sequenceOf(view))
        }

        fun parseURL(url: String) = when(url.lowercase()) {
            "meow" -> "https://i.natgeofe.com/n/548467d8-c5f1-4551-9f58-6817a8d2c45e/NationalGeographic_2572187_square.jpg"
            else -> url
        }
    }

    init {
        Companion.appScope = MainScope()
        Companion.focusManager = focusManager
        Companion.scheduler = scheduler
        Companion.popupManager = popupManager
        Companion.modalManager = modalManager
        Companion.animate = animator
        Companion.textMetrics = textMetrics
        Companion.pathMetrics = pathMetrics
        Companion.menus = MenuFactoryImpl(popupManager, scheduler, focusManager)
        Companion.themeManager = themeManager
        Companion.display = display
        Companion.imageLoader = imageLoader
        themeManager.selected = nativeTheme + basicTheme + DefaultTheme(basicTheme.config)

        appScope.launch {
            val fallback = "./background/Generic.png"
            val url =
                window.localStorage.getItem(fieldImageKey)?.let { parseURL(it) } ?: fallback
            Field.backgrounds["Generic"] = imageLoader.load(url) ?: imageLoader.load(fallback) ?: run {
                println("Unable to load default image!")
                null
            }
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
        appScope.cancel()
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
            ModalModule,
            BasicTheme.BasicTheme,
            BasicTheme.basicButtonBehavior(),
            BasicTheme.basicDropdownBehavior(),
            BasicTheme.basicLabelBehavior(),
            BasicTheme.basicMenuBehavior(),
            NativeTheme.NativeTheme,
            NativeTheme.nativeTextFieldBehavior(),
            DI.Module("pathmetrics") {
                bind<PathMetrics>() with singleton {
                    PathMetricsImpl(instance())
                }
            }
//            KeyboardModule,
        )
    ) {
        GUIApp(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }
}