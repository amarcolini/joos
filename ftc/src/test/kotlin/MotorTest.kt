import com.amarcolini.joos.control.DCMotorFeedforward
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.epsilonEquals
import mock.DummyClock
import mock.DummyMotor
import org.junit.Before
import org.junit.Test
import org.knowm.xchart.QuickChart
import org.knowm.xchart.style.theme.MatlabTheme
import kotlin.math.PI
import kotlin.math.abs

class MotorTest {
    @Before
    fun reset() {
        DummyClock.setTime(0.0)
        DummyClock.disableRealtime()
    }

    @Test
    fun testGroupReversal() {
        val motor1 = Motor(DummyMotor(1.0, 1.0), 1.0, 1.0)
        val motor2 = Motor(DummyMotor(1.0, 1.0), 1.0, 1.0)
        val group = MotorGroup(
            motor1,
            motor2.reversed()
        )
        assert(!motor1.reversed && motor2.reversed)
        group.reversed = true
        assert(motor1.reversed && !motor2.reversed)
        group.reversed()
        assert(!motor1.reversed && motor2.reversed)
    }

    @Test
    fun testVelocity() {
        val rpm = 69.0
        val tpr = 420.0
        val internal = DummyMotor(rpm, tpr)
        val motor = Motor(internal, rpm, tpr)
        motor.power = 1.0
        DummyClock.step(0.1)
        internal.update()
        DummyClock.step(10.0)
        internal.update()
        assert(motor.velocity epsilonEquals rpm*tpr/60)
    }

    @Test
    fun testOffset() {
        val internal = DummyMotor(100.0, 100.0, 1.0, 1.0, 0.0)
        val motor = Motor(internal, 100.0, 100.0)
        var seconds = 0.0
        val dt = 0.01
        val xData = ArrayList<Double>()
        val yData = ArrayList<Double>()
        motor.power = 1.0
        do {
            internal.update(seconds)
            xData.add(seconds)
            yData.add(motor.velocity)
            seconds += dt
        } while (seconds <= 1)
        val chart = QuickChart.getChart("motor", "time", "motor speed", "motor", xData, yData)
        chart.styler.isLegendVisible = false
        chart.styler.theme = MatlabTheme()
        GraphUtil.saveGraph("motor_test", chart)
    }

    @Test
    fun testPID() {
        val internal = DummyMotor(100.0, 100.0, 1.0, 1.0, 0.0, DummyClock)
        val motor = Motor(internal, 100.0, 100.0, DummyClock)
        motor.veloCoefficients = PIDCoefficients(0.008, 0.0016, 0.000003)
        motor.runMode = Motor.RunMode.RUN_USING_ENCODER
        val xData = ArrayList<Double>()
        val yData = ArrayList<Double>()
        motor.setRPM(50.0)
        println(motor.runMode)
        do {
            DummyClock.step(0.01)
            internal.update()
            motor.update()
            xData.add(DummyClock.seconds())
            yData.add(motor.velocity)
        } while (DummyClock.seconds() <= 1)
        val chart = QuickChart.getChart("motor", "time", "motor speed", "motor", xData, yData)
        chart.styler.isLegendVisible = false
        chart.styler.theme = MatlabTheme()
        GraphUtil.saveGraph("pid_test", chart)
        assert(abs(motor.velocity - 50.0) <= 3.0)
    }

    @Test
    fun testFeedforward() {
        val internal = DummyMotor(100.0, 100.0, 0.5, 1.0, 0.0)
        val motor = Motor(internal, 100.0, 100.0, DummyClock)
        motor.runMode = Motor.RunMode.RUN_WITHOUT_ENCODER
        motor.feedforward =
            DCMotorFeedforward(1.25 / motor.maxTPS)
        val xData = ArrayList<Double>()
        val yData = ArrayList<Double>()
        motor.setRPM(40.0)
        do {
            internal.update()
            motor.update()
            xData.add(DummyClock.seconds())
            yData.add(motor.velocity)
            DummyClock.step(0.01)
        } while (DummyClock.seconds() <= 1)
        val chart = QuickChart.getChart("motor", "time", "motor speed", "motor", xData, yData)
        chart.styler.isLegendVisible = false
        chart.styler.theme = MatlabTheme()
        GraphUtil.saveGraph("feedforward_test", chart)
        assert(abs(motor.velocity - 40.0) < 5.0)
    }
}