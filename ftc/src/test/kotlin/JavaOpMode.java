import com.amarcolini.joos.command.Command;
import com.amarcolini.joos.command.CommandOpMode;

public class JavaOpMode extends CommandOpMode {
    @Register
    FakeRobot robot;

    @Override
    public void preInit() {
        robot.getMotor().setPower(1.0);
        Command.of(() -> {

        });
        schedule(() -> System.out.println(robot.getMotor().getPower()));
    }
}