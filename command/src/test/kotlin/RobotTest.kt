import com.amarcolini.joos.command.Robot
import com.amarcolini.joos.command.RobotOpMode
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.junit.jupiter.api.Test

class DummyRobot(opmode: OpMode) : Robot(opmode) {
    val specialNumber = 42

    override fun init() {
        println("initializing")
    }

    override fun start() {
        println("starting")
    }
}

class TestOpMode : RobotOpMode<DummyRobot>() {
    @Test
    fun testRobot() {
        try {
            TestOpMode().init()
        } catch (e: Exception) {
            assert(e !is UninitializedPropertyAccessException)
        }
    }

    override fun preInit() {
        initialize<DummyRobot>()
        assert(robot.specialNumber == 42)
    }

    override fun preStart() {

    }
}