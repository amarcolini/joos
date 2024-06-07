package com.amarcolini.joos

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.CommandGroup
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.builders.PathCommandBuilder
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicGVFFollower
import com.amarcolini.joos.followers.PathFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.hardware.drive.DrivePathFollower
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.util.deg
import org.junit.Test

class BuilderTest {
    @Test
    fun testBack() {
        val dummyComponent = Component.of {}
        val output = PathCommandBuilder(
            DummyDrive, Pose2d(-2 * 24.0 + 8.0, 3 * 24.0 - 9.0, (-90).deg)
        )
            .lineToSplineHeading(Pose2d(-49.0, 28.0, (0).deg))
            .afterDisp(0.0, (Command.empty().requires(dummyComponent)))
            .back(3.0)
//            .waitForAll()
            .afterDisp(0.0, Command.empty().requires(dummyComponent))
            .lineToSplineHeading(Pose2d(-50.0, 13.0, 0.deg))
            .build()

//        logDependencyTree(output.command)
    }

    fun logDependencyTree(command: Command, level: Int = 0) {
        var line = ""
        for (i in 0..<level) line += "  "
        if (command is CommandGroup) {
            command::class.simpleName?.let { println("$line$it (${command.commands.size})") }
            for (child in command.commands) {
                logDependencyTree(child, level + 1)
            }
        } else println("$line- ${command.requirements}")
    }

    object DummyDrive : DrivePathFollower {
        override val pathFollower: PathFollower = HolonomicGVFFollower(
            40.0, 40.0, 40.0, 180.deg, 180.deg, Pose2d(0.5, 0.5, 5.deg),
            1.0, 1.0, 1.0, 1.0, PIDCoefficients(1.0)
        )
        override var localizer: Localizer
            get() = throw Error("Not implemented")
            set(value) {}
        override val motors: MotorGroup
            get() = throw Error("Not implemented")

        override fun setDriveSignal(driveSignal: DriveSignal) {
        }

        override fun setDrivePower(drivePower: Pose2d) {
        }
    }
}