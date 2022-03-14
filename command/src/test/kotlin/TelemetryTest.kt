import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.command.SuperTelemetry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TelemetryTest {
    private lateinit var telemetry: SuperTelemetry

    @BeforeEach
    fun init() {
        telemetry = SuperTelemetry()
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