package swerveTest

import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.PIDSwerveModule
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.util.deg
import mock.DummyMotor
import org.junit.Test

class SwerveTest {
    @Test
    fun testMotorModuleFlipping() {
        var moduleOrientation = 0.deg
        val hardwareLock = object {}
        val motor = Motor(DummyMotor(hardwareLock = hardwareLock), Motor.Type.GOBILDA_MATRIX)
        val module = SampleSwerveModule(
            AngleSensor.from({ moduleOrientation }),
            motor
        )
        val remainingModules = List(3) {
            SampleSwerveModule(
                AngleSensor.from({ 0.deg }),
                Motor(DummyMotor(hardwareLock = hardwareLock), Motor.Type.GOBILDA_MATRIX)
            )
        }
        val drive = SampleSwerveDrive(
            module,
            remainingModules[0],
            remainingModules[1],
            remainingModules[2],
        )

        drive.apply {
            setDrivePower(Pose2d(1.0))
            repeat(3) {
                update()
            }
        }
        assert(motor.power == 1.0)

        moduleOrientation = 170.deg
        drive.apply {
            update()
        }
        assert(motor.power == -1.0)

        drive.apply {
            setDrivePower(Pose2d(1.0))
            update()
        }
        assert(motor.power == -1.0)

        moduleOrientation = (-170).deg
        drive.apply {
            setDrivePower(Pose2d(-1.0))
            update()
        }
        assert(motor.power == 1.0)
    }
}