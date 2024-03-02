import com.amarcolini.joos.geometry.Angle;
import com.amarcolini.joos.geometry.Pose2d;
import com.amarcolini.joos.geometry.Vector2d;
import com.amarcolini.joos.serialization.LinePiece;
import com.amarcolini.joos.serialization.SerializableTrajectory;
import com.amarcolini.joos.serialization.StartPiece;

import java.util.Arrays;

public class MainTest {
    public static void main(String[] args) {
        JoosGUI.INSTANCE.setTrajectory(
                new SerializableTrajectory(
                        new StartPiece(new Pose2d(30.0, 30.0, Angle.deg(10.0))),
                        Arrays.asList(
                                new LinePiece(new Vector2d(40.0, 60.0))
                        )
                )
        );
        JoosGUI.INSTANCE.launch();
    }
}