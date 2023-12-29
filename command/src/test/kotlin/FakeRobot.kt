import com.amarcolini.joos.command.Robot
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.trajectory.constraints.GenericConstraints

class FakeRobot : Robot() {
    val motor: Motor = Motor(hMap, "my_motor", Motor.Type.GOBILDA_312)

    val constraints = GenericConstraints()

    override fun init() {
        motor.zeroPowerBehavior = Motor.ZeroPowerBehavior.BRAKE
    }
}