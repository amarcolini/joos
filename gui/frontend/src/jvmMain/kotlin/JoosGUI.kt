import com.amarcolini.joos.serialization.SerializableTrajectory
import field.DraggableTrajectory
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Modules
import io.nacular.doodle.application.application
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import util.TrajectoryMetadata

object JoosGUI {
    var trajectory: SerializableTrajectory? = null

    @JvmStatic
    fun launch() {
        trajectory?.let {
            DraggableTrajectory.trajectory = TrajectoryMetadata.fromTrajectory(
                it
            )
            DraggableTrajectory.initializePathEditing()
        }
        application(
            modules = listOf(
                Modules.ImageModule,
                Modules.PointerModule,
                Modules.FocusModule,
//            AccessibilityModule,
                Modules.PopupModule,
                AnimationModule,
                Modules.ModalModule,
                Modules.MenuFactoryModule,
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

    private val AnimationModule = DI.Module(name = "AnimationModule") {
        bindProvider<Animator> { AnimatorImpl(timer = instance(), animationScheduler = instance()) }
    }
}