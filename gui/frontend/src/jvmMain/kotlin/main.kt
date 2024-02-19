import com.amarcolini.joos.util.addOne
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Application
//import io.nacular.doodle.application.Modules.Companion.AccessibilityModule
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.MenuFactoryModule
import io.nacular.doodle.application.Modules.Companion.ModalModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.Modules.Companion.PopupModule
import io.nacular.doodle.application.application
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import org.kodein.di.*


fun main() {
    application(
        modules = listOf(
            ImageModule,
            PointerModule,
            FocusModule,
//            AccessibilityModule,
            PopupModule,
            AnimationModule,
            ModalModule,
            MenuFactoryModule,
            BasicTheme.BasicTheme,
            BasicTheme.basicButtonBehavior(),
            BasicTheme.basicDropdownBehavior(),
            BasicTheme.basicLabelBehavior(),
            BasicTheme.basicMenuBehavior(),
            NativeTheme.NativeTheme,
            NativeTheme.nativeTextFieldBehavior(),
//            DI.Module("pathmetrics") {
//                bind<PathMetrics>() with singleton {
//                    PathMetricsImpl(instance())
//                }
//            }
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
//            instance(),
            instance(),
            instance()
        )
    }
}

val AnimationModule = DI.Module(name = "AnimationModule") {
    bindProvider<Animator> { AnimatorImpl(timer = instance(), animationScheduler = instance()) }
}