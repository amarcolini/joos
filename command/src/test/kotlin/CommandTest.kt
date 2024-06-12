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

private const val logOutput: Boolean = true

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
    fun testProperInterrupting() {
        if (logOutput) println("   **testProperInterrupting**")
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
        if (logOutput) println("   **testUninterruptible**")
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
    fun testSequentialGroups() {
        if (logOutput) println("   **testSequentialGroups**")
        val component = DummyComponent("common")
        val cmd1 = RangeCommand("#1", 0, 5, true, setOf(component))
        val cmd2 = RangeCommand("#2", 1, 7, false, setOf(component))
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
    fun testWaitCommands() {
        if (logOutput) println("   **testWaitCommands**")
        val cmd = WaitCommand(1.0)
        val clock = NanoClock.system
        val start = clock.seconds()
        cmd.runBlocking()
        //Accurate to a thousandth of a second
        assert(abs(clock.seconds() - start - 1.0) < 0.001)
    }

    @Test
    fun testParallelGroups() {
        if (logOutput) println("   **testParallelGroups**")
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
    fun testRaceGroups() {
        if (logOutput) println("   **testRace**")
        val a1 = RangeCommand("#A1", 0, 5)
        val a2 = RangeCommand("#A2", 0, 6)
        val cmd = a1 race a2
        cmd.init()
        assert(a1.isInitialized && a2.isInitialized)
        while (!a1.isFinished()) cmd.execute()
        assert(a1.hasEnded && !a2.hasEnded)
        assert(a1.isFinished() && !a2.isFinished() && cmd.isFinished())
        cmd.end(true)
    }

    @Test
    fun testIfPrettyLooking() {
        if (logOutput) println("   **testIfPrettyLooking**")
        val cmd = Command.of { if (logOutput) println("first") }
            .then { if (logOutput) println("second") }
            .then(
                Command.of { if (logOutput) println("A1") } and { if (logOutput) println("A2") }
            )
            .then { if (logOutput) println("third") }
            .wait(1.0)
            .then { if (logOutput) println("So cool!!") }
        cmd.runBlocking()
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
    fun testSelectCommand() {
        if (logOutput) println("   **testSelectCommand**")
        var number = 0
        if (logOutput) println("number is $number.")
        val cmd = Command.select {
            val newNumber = number + 1
            Command.of {
                number = newNumber
                if (logOutput) println("setting number to $newNumber.")
            }
        }
        cmd.runBlocking()
        assert(number == 1)
    }

    @Test
    fun testTimeCommand() {
        if (logOutput) println("   **testTimeCommand**")
        var time = 0.0
        var compounded = 0.0
        scheduler.schedule(TimeCommand({ t, dt ->
            compounded += dt
            t > 3
        }, object : NanoClock {
            override fun seconds() = time
        }))
        repeat(40) {
            scheduler.update()
            time += 0.1
        }
        if (logOutput) println(compounded)
        assert(compounded epsilonEquals 3.0)
    }

    @Test
    fun testCoroutineCommand() {
        var count = -1
        val command = CoroutineCommand {
            count = 0
            yield(false)
            do {
                count++
                yield(false)
            } while (count < 5)
            yield(true)
        }
        assert(count == -1)
        scheduler.schedule(command)
        assert(count == 0)
        scheduler.update()
        assert(count == 1)
        while (!command.isFinished()) scheduler.update()
        assert(count == 5)
    }

    @OptIn(CommandScope.Unsafe::class)
    @Test
    fun testCoroutineAsync() {
        var count = -1
        var secondCount = -1
        var hasCancelled = false
        val command = CoroutineCommand {
            count = 0
            val background = async {
                do {
                    secondCount++
                } while ((secondCount < 5).also { yield(!it) })
            }
            async {
                while (true) yield(false)
            }.ifInterrupted {
                hasCancelled = true
            }
            yield(false)
            count++
            yield(false)
            await(background)
            count++
        }
        assert(count == -1 && secondCount == -1)
        scheduler.schedule(command)
        assert(count == 0 && secondCount == 0)
        scheduler.update()
        assert(count == 1 && secondCount == 1)
        scheduler.update()
        assert(count == 1 && secondCount == 2)
        repeat(3) { scheduler.update() }
        assert(count == 1 && secondCount == 5)
        scheduler.update()
        assert(count == 2 && secondCount == 5)
        scheduler.update()
        assert(command.isFinished() && hasCancelled)
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