import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.serialization.LinePiece
import com.amarcolini.joos.serialization.SerializableTrajectory
import com.amarcolini.joos.serialization.SplinePiece
import com.amarcolini.joos.serialization.StartPiece
import com.amarcolini.joos.util.deg
import kotlin.test.Test

class GUITest {
    @Test
    fun testGUI()  {
        JoosGUI.trajectory = SerializableTrajectory(
            StartPiece(Pose2d(10.0, 10.0, 45.deg)),
            mutableListOf(
                LinePiece(Vector2d(20.0, 20.0)),
                SplinePiece(Vector2d(-40.0, 30.0), 45.deg)
            )
        )
        JoosGUI.launch()
        try {
            while (GUIApp.activeState != GUIApp.State.Shutdown) {
                Thread.sleep(1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}