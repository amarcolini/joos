import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import kotlin.test.Test

class PathFindingTest {
    @Test
    fun testPathFinding() {
        JoosGUI.customRender = {
            for (x in -60..60 step 5) {
                for (y in -60..60 step 5) {
                    it.circle(Circle(Point(x, y), 2.0), Color.Blue.paint)
                }
            }
        }
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