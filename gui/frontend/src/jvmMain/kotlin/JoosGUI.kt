import com.amarcolini.joos.serialization.SerializableTrajectory
import field.DraggableTrajectory
import field.Field
import field.FieldEntity
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Modules
import io.nacular.doodle.application.application
import io.nacular.doodle.controls.popupmenu.MenuFactoryImpl
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.geometry.PathMetrics
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindProvider
import org.kodein.di.instance
import settings.Settings
import util.TrajectoryMetadata

object JoosGUI {
    private val trajectories = arrayListOf<SerializableTrajectory>()

    fun addTrajectory(trajectory: SerializableTrajectory) {
        trajectories += trajectory
    }

    var customRender = { canvas: Canvas -> }
    private val renderLayer = object : FieldEntity() {
        override fun render(canvas: Canvas) {
            customRender(canvas)
        }
        init {
            minimumSize = Size(10.0, 10.0)
            size = minimumSize
            clipCanvasToBounds = false
            focusable = false
        }

        override fun contains(point: Point): Boolean = false
    }

    init {
        Field.children += renderLayer
    }

    @JvmStatic
    fun launch() {
        trajectories.map { DraggableTrajectory(TrajectoryMetadata.fromTrajectory(it)) }.forEach {
            Settings.trajectories += it
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
                Modules.PathModule,
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
                GUIApp.GUIConfig(),
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