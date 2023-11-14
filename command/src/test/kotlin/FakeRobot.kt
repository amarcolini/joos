import com.amarcolini.joos.command.Robot
import com.amarcolini.joos.hardware.Motor

class FakeRobot : Robot() {
    val motor: Motor = Motor(hMap, "my_motor", Motor.Type.GOBILDA_312)

    override fun init() {
        motor.zeroPowerBehavior = Motor.ZeroPowerBehavior.BRAKE
    }
}