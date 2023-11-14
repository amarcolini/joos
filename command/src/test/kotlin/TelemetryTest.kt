import com.amarcolini.joos.dashboard.SuperTelemetry
import org.junit.Before
import org.junit.Test

class TelemetryTest {
    private val telemetry = SuperTelemetry

    @Before
    fun init() {
        telemetry.reset()
    }

    private fun printTelemetry() {
        telemetry.lines.forEach { println(it.composed()) }
        println("------------------")
    }

    @Test
    fun testBasic() {
        telemetry.addData("Number", 1)
        telemetry.addData("String", "hello!")
        telemetry.addData("format", "%.2f, %.2f", 1.234, 3.456)
        telemetry.addLine("multiple items: ")
            .addData("item1", "yo")
            .addData("item2", "what up")
        telemetry.captionValueSeparator = " - "
        printTelemetry()
        telemetry.clear()
        assert(telemetry.lines.isEmpty())
    }
}