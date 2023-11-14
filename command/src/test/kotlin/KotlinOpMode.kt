import com.amarcolini.joos.command.CommandOpMode

class KotlinOpMode : CommandOpMode() {
    private val robot: FakeRobot by robot()

    override fun preInit() {
        schedule {
            robot.motor.power = 1.0
            println(robot.motor.power)
        }
    }
}