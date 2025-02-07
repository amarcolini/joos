import com.amarcolini.joos.command.CommandOpMode;

public class JavaOpMode extends CommandOpMode {
    @Register
    FakeRobot robot;

    @Override
    public void preInit() {
        robot.getMotor().setPower(1.0);
        schedule(() -> System.out.println(robot.getMotor().getPower()));
    }
}