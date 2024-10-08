import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.serialization.LinePiece
import com.amarcolini.joos.serialization.SerializableTrajectory
import com.amarcolini.joos.serialization.SplinePiece
import com.amarcolini.joos.serialization.StartPiece
import com.amarcolini.joos.util.deg
import field.DraggableTrajectory
import io.nacular.doodle.animation.Animator
import io.nacular.doodle.animation.AnimatorImpl
import io.nacular.doodle.application.Modules.Companion.AccessibilityModule
import io.nacular.doodle.application.Modules.Companion.FocusModule
import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.MenuFactoryModule
import io.nacular.doodle.application.Modules.Companion.ModalModule
import io.nacular.doodle.application.Modules.Companion.PathModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.Modules.Companion.PopupModule
import io.nacular.doodle.application.application
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import org.kodein.di.*
import settings.Settings
import util.TrajectoryMetadata


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
            MenuFactoryModule,
            PathModule,
            BasicTheme.BasicTheme,
            BasicTheme.basicButtonBehavior(),
            BasicTheme.basicSelectBoxBehavior(),
            BasicTheme.basicMutableSelectBoxBehavior(),
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
        val trajectories = mutableListOf<SerializableTrajectory>()
        trajectories += (
            SerializableTrajectory(
            StartPiece(Pose2d(0.0, 0.0, -90.deg)),
            mutableListOf(
                LinePiece(Vector2d(30.0, 20.0)),
                SplinePiece(Vector2d(40.0, 30.0), 90.deg)
            )
        )
        )
        trajectories += (
                SerializableTrajectory(
                    StartPiece(Pose2d(10.0, 10.0, 45.deg)),
                    mutableListOf(
                        LinePiece(Vector2d(20.0, 21.0)),
                        SplinePiece(Vector2d(-40.0, 30.0), 45.deg)
                    )
                )
                )
        trajectories.map { DraggableTrajectory(TrajectoryMetadata.fromTrajectory(it)) }.let {
            Settings.trajectories += it
        }
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
            instance(),
            instance(),
        )
    }
}

val AnimationModule = DI.Module(name = "AnimationModule") {
    bindProvider<Animator> { AnimatorImpl(timer = instance(), animationScheduler = instance()) }
}