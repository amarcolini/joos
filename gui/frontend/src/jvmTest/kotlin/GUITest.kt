import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.serialization.LinePiece
import com.amarcolini.joos.serialization.SerializableTrajectory
import com.amarcolini.joos.serialization.SplinePiece
import com.amarcolini.joos.serialization.StartPiece
import com.amarcolini.joos.util.deg
import io.nacular.doodle.application.Modules
import io.nacular.doodle.application.application
import io.nacular.doodle.theme.basic.BasicTheme
import io.nacular.doodle.theme.native.NativeTheme
import org.kodein.di.instance
import kotlin.test.Test

class GUITest {
    @Test
    fun testGUI()  {
        JoosGUI.addTrajectory(SerializableTrajectory(
            StartPiece(Pose2d(0.0, 0.0, -90.deg)),
            mutableListOf(
                LinePiece(Vector2d(30.0, 20.0)),
                SplinePiece(Vector2d(40.0, 30.0), 90.deg)
            )
        ))
        JoosGUI.addTrajectory(SerializableTrajectory(
            StartPiece(Pose2d(10.0, 10.0, 45.deg)),
            mutableListOf(
                LinePiece(Vector2d(20.0, 21.0)),
                SplinePiece(Vector2d(-40.0, 30.0), 45.deg)
            )
        ))
        JoosGUI.launch()
        try {
            while (GUIApp.activeState != GUIApp.State.Shutdown) {
                Thread.sleep(500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}