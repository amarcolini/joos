import com.griffinrobotics.lib.command.*
import com.griffinrobotics.lib.hardware.gamepad.Button
import com.griffinrobotics.lib.util.NanoClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommandTest {
    private lateinit var scheduler: CommandScheduler

    @BeforeEach
    fun init() {
        scheduler = CommandScheduler()
    }

    @Test
    fun testCommand() {
//        println("   **testCommand**")
        val command = RangeCommand("test", 0, 10, true)
        scheduler.schedule(command)
        assert(command.isScheduled())
        assert(scheduler.isScheduled(command))
        assert(command.isInitialized)
        while (!command.isFinished()) scheduler.update()
        scheduler.update()
        assert(command.hasEnded)
        assert(!command.isScheduled())
        assert(!scheduler.isScheduled(command))
        assert(command.num == command.end)
    }

    @Test
    fun testComponent() {
//        println("   **testComponent**")
        val component = DummyComponent("test")
        scheduler.register(component)
        scheduler.update()
        assert(component.updateCount() == 1)
        scheduler.unregister(component)
        scheduler.update()
        assert(component.updateCount() == 1)
    }

    @Test
    fun testCancel() {
//        println("   **testCancel**")
        val command = RangeCommand("test", 0, 10, false)
        scheduler.schedule(command)
        assert(command.isScheduled())
        assert(scheduler.isScheduled(command))
        assert(command.isInitialized)
        scheduler.update()
        scheduler.update()
        command.cancel()
        scheduler.update()
        assert(command.hasEnded)
        assert(!command.isScheduled())
        assert(!scheduler.isScheduled(command))
        assert(command.num == 2)
    }

    @Test
    fun testInterrupt() {
//        println("   **testInterrupt**")
        val component = DummyComponent("Bobby")
        val command = RangeCommand("test", 0, 10, true, setOf(component))
        scheduler.schedule(command)
        scheduler.update()
        val command2 = RangeCommand("teSt3", 0, 10, true, setOf(component))
        scheduler.schedule(command2)
        assert(command.num == 1)
        assert(!command.isScheduled())
        assert(!scheduler.isScheduled(command))
        assert(command.hasEnded && command.wasInterrupted)
        assert(command2.isInitialized && command2.isScheduled())
        scheduler.update()
        assert(command2.num == 1)

        assert(component.updateCount() == 0 && component.name != "Billy")
    }

    @Test
    fun testUninterruptible() {
//        println("   **testUninterruptible**")
        val component = DummyComponent("Bobby")
        val command = RangeCommand("test", 0, 10, false, setOf(component))
        scheduler.schedule(command)
        scheduler.update()
        val command2 = RangeCommand("teSt3", 0, 10, true, setOf(component))
        assert(!scheduler.schedule(command2))
        assert(command.num == 1)
        assert(command.isScheduled())
        assert(scheduler.isScheduled(command))
        assert(!command.hasEnded && !command.wasInterrupted)
        assert(!command2.isInitialized && !command2.isScheduled())
        scheduler.update()
        assert(command2.num != 1 && command.num == 2)

        assert(component.updateCount() == 0 && component.name != "Billy")
    }

    @Test
    fun testSequential() {
//        println("   **testSequential**")
        val cmd1 = RangeCommand("#1", 0, 5, true)
        val cmd2 = RangeCommand("#2", 1, 7, false)
        val group = cmd1 then cmd2

        scheduler.schedule(group)
        assert(group.isScheduled())
        assert(!group.isFinished())
        assert(cmd1.isInitialized)
        assert(!cmd2.isInitialized)
        scheduler.update()
        assert(cmd1.num == 1)
        assert(cmd2.num == 1)
        while (!cmd1.isFinished()) scheduler.update()
        assert(!group.isFinished())
        assert(cmd1.num == 5)
        assert(!cmd1.wasInterrupted && cmd1.hasEnded)
        assert(cmd2.isInitialized)
        assert(cmd2.num == 1)
        while (!group.isFinished()) scheduler.update()
        assert(cmd2.isFinished() && cmd2.hasEnded && !cmd2.wasInterrupted)
    }

    @Test
    fun testWait() {
//        println("   **testWait**")
        val cmd = WaitCommand(1.0)
        val clock = NanoClock.system()
        val start = clock.seconds()
        cmd.run()
        assert(clock.seconds() - start - 1.0 < 0.0001)
    }

    @Test
    fun testParallel() {
//        println("   **testParallel**")
        val a1 = RangeCommand("#A1", 0, 5)
        val a2 = RangeCommand("#A2", 0, 6)
        val cmd = a1 and a2
        cmd.init()
        assert(a1.isInitialized && a2.isInitialized)
        while (!a1.isFinished()) cmd.execute()
        assert(a1.hasEnded && !a2.hasEnded)
        assert(!cmd.isFinished())
        cmd.end(true)
    }

    @Test
    fun testPretty() {
        val cmd = InstantCommand { println("first") }
            .then { println("second") }
            .then(
                Command.of { println("A1") } and Command.of { println { "A2" } }
            )
            .wait(1.0)
            .then { println("So cool!!") }
        cmd.run()
    }

    @Test
    fun testCondition() {
        val btn1 = Button()
        val btn2 = Button()
        var runCount = 0
        val cmd = Command.of { runCount++ }

        scheduler.map(btn1::justActivated, cmd)
        scheduler.map(btn2::isActive, cmd)

        repeat(20) {
            scheduler.update()
        }

        assert(runCount == 0)
        btn1.toggle()

        scheduler.update()

        assert(runCount == 1)
        btn1.state = btn1.state
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
//        println("Initializing $name at $start")
    }

    override fun execute() {
        num++
//        println("Running $name. Currently at $num.")
    }

    override fun end(interrupted: Boolean) {
        hasEnded = true
        wasInterrupted = interrupted
//        println("Ending $name. $interrupted")
    }

    override fun isFinished() = num >= end
}

class DummyComponent(val name: String) : Component {
    private var updateCount = 0

    override fun update(scheduler: CommandScheduler) {
        updateCount++
//        println("Updating component $name: $updateCount")
    }

    fun updateCount() = updateCount
}