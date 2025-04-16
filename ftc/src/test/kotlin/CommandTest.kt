import com.amarcolini.joos.command.*
import com.amarcolini.joos.gamepad.Button
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.epsilonEquals
import com.qualcomm.robotcore.eventloop.opmode.MockOpModeManager
import mock.DummyMotor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

private const val logOutput: Boolean = false

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CommandTest {
    private val scheduler = CommandScheduler

    @Before
    fun reset() {
        scheduler.reset()
    }

    @Test
    fun testOpMode() {
        val opmode = JavaOpMode()
        val opmodemanager = MockOpModeManager(opmode)
        opmodemanager.hMap.dcMotor.put("my_motor", DummyMotor())
        opmodemanager.setUpOpMode()
        opmodemanager.initOpMode()
        Thread.sleep(100)
        opmodemanager.startOpMode()
        Thread.sleep(200)
        opmodemanager.stopOpMode()
    }

    @Test
    fun testCommandLifeCycle() {
        if (logOutput) println("   **testCommandLifeCycle**")
        val command = RangeCommand("test", 0, 10, true)
        val extraCommands = List(10) { i -> RangeCommand("extra #$i", 0, 0, true) }
        scheduler.schedule(command, *extraCommands.toTypedArray())
        assert(scheduler.isScheduled(command))
        assert(command.isInitialized)
        while (!command.isFinished()) scheduler.update()
        scheduler.update()
        assert(command.hasEnded)
        assert(!scheduler.isScheduled(command))
        assert(command.num == command.end)
    }

    @Test
    fun testConcurrentModification() {
        var result = false
        val cmd = Command.empty().runForever()
            .onEnd {
                result = true
            }
        repeat(3) {
            scheduler.schedule(Command.empty())
        }
        scheduler.schedule(Command.of {
            scheduler.schedule(Command.of {
                scheduler.cancel(cmd)
                scheduler.reset()
            })
        }, cmd)
        repeat(10) {
            scheduler.update()
        }
        assert(result)
    }

    @Test
    fun testComponentLifeCycle() {
        if (logOutput) println("   **testComponentLifeCycle**")
        val component = DummyComponent("test")
        scheduler.register(component)
        scheduler.update()
        assert(component.updateCount() == 1)
        scheduler.unregister(component)
        scheduler.update()
        assert(component.updateCount() == 1)
    }

    @Test
    fun testProperCancelling() {
        if (logOutput) println("   **testProperCancelling**")
        val command = RangeCommand("test", 0, 10, false)
        scheduler.schedule(command)
        assert(scheduler.isScheduled(command))
        assert(command.isInitialized)
        scheduler.update()
        scheduler.update()
        scheduler.cancel(command)
        scheduler.update()
        assert(command.hasEnded)
        assert(!scheduler.isScheduled(command))
        assert(command.num == 2)
    }

    @Test
    fun testProperInterrupting() {
        if (logOutput) println("   **testProperInterrupting**")
        val component = DummyComponent("Bobby")
        val command = RangeCommand("test", 0, 10, true, setOf(component))
        scheduler.schedule(command)
        scheduler.update()
        val command2 = RangeCommand("teSt3", 0, 10, true, setOf(component))
        scheduler.schedule(command2)
        assert(command.num == 1)
        assert(!scheduler.isScheduled(command))
        assert(command.hasEnded && command.wasInterrupted)
        assert(command2.isInitialized && scheduler.isScheduled(command2))
        scheduler.update()
        assert(command2.num == 1)

        assert(component.updateCount() == 0 && component.name != "Billy")
    }

    @Test
    fun testUninterruptible() {
        if (logOutput) println("   **testUninterruptible**")
        val component = DummyComponent("Bobby")
        val command = RangeCommand("test", 0, 10, false, setOf(component))
        scheduler.schedule(command)
        scheduler.update()
        val command2 = RangeCommand("teSt3", 0, 10, true, setOf(component))
        assert(!scheduler.schedule(command2))
        assert(command.num == 1)
        assert(scheduler.isScheduled(command))
        assert(!command.hasEnded && !command.wasInterrupted)
        assert(!command2.isInitialized && !scheduler.isScheduled(command2))
        scheduler.update()
        assert(command2.num != 1 && command.num == 2)

        assert(component.updateCount() == 0 && component.name != "Billy")
    }

    @Test
    fun testConditionMapping() {
        if (logOutput) println("   **testConditionMapping**")
        val btn1 = Button()
        val btn2 = Button()
        var runCount = 0
        val cmd = InstantCommand { runCount++ }

        scheduler.map(btn1::isJustActivated, cmd)
        scheduler.map(btn2::isActive, cmd)

        repeat(10) {
            scheduler.update()
        }

        assert(runCount == 0)
        btn1.toggle()

        scheduler.update()

        assert(runCount == 1)
        btn1.update(btn1.state)
        scheduler.update()
        assert(runCount == 1)
        btn2.toggle()
        scheduler.update()
        assert(runCount == 2)
        btn2.toggle()

        scheduler.map(btn1::isActive, cmd)

        repeat(2) {
            scheduler.update()
        }

        assert(runCount == 4)

        scheduler.unmap(cmd)
        scheduler.update()

        assert(runCount == 4)
    }

    @Test
    fun testSchedulePolicy() {
        val component = DummyComponent("Fred")
        val command1 = RangeCommand("c1", 0, 2, false, setOf(component))
        val command2 = RangeCommand("c2", 0, 2, true, setOf(component))

        scheduler.waitToScheduleCommands = false
        assert(scheduler.schedule(command1))
        assert(!scheduler.schedule(command2))
        while (scheduler.isScheduled(command1)) scheduler.update()
        scheduler.update()
        assert(!scheduler.isScheduled(command2))

        scheduler.waitToScheduleCommands = true
        assert(scheduler.schedule(command1))
        assert(!scheduler.schedule(command2))
        while (scheduler.isScheduled(command1)) scheduler.update()
        scheduler.update()
        assert(scheduler.isScheduled(command2) && command2.num == 1)
    }
}

class RangeCommand @JvmOverloads constructor(
    val name: String,
    val start: Int = 0,
    val end: Int,
    override val isInterruptable: Boolean = true,
    override val requirements: Set<Component> = HashSet()
) : Command() {
    var num: Int = start
    var isInitialized = false
    var wasInterrupted = false
    var hasEnded = false

    override fun init() {
        num = start
        isInitialized = true
        if (logOutput) println("Initializing $name at $start")
    }

    override fun execute() {
        num++
        if (logOutput) println("Running $name. Currently at $num.")
    }

    override fun end(interrupted: Boolean) {
        hasEnded = true
        wasInterrupted = interrupted
        if (logOutput) println("Ending $name. $interrupted")
    }

    override fun isFinished() = num >= end
}

class DummyComponent(val name: String) : Component {
    private var updateCount = 0

    override fun update() {
        updateCount++
        if (logOutput) println("Updating component $name: $updateCount")
    }

    fun updateCount() = updateCount
}