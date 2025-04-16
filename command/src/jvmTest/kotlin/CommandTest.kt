import com.amarcolini.joos.command.*
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.epsilonEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs

private const val logOutput: Boolean = false

class CommandTest {
    fun runBlocking(command: Command) {
        command.init()
        do {
            command.execute()
        } while (!command.isFinished())
        command.end(false)
    }
    
    @Test
    fun testSequentialGroups() {
        if (logOutput) println("   **testSequentialGroups**")
        val component = DummyComponent("common")
        val cmd1 = RangeCommand("#1", 0, 5, true, setOf(component))
        val cmd2 = RangeCommand("#2", 1, 7, false, setOf(component))
        val group = cmd1 then cmd2
        
        group.init()
        assert(!group.isFinished())
        assert(cmd1.isInitialized)
        assert(!cmd2.isInitialized)
        group.execute()
        assert(cmd1.num == 1)
        assert(cmd2.num == 1)
        while (!cmd1.isFinished()) group.execute()
        assert(!group.isFinished())
        assert(cmd1.num == 5)
        assert(!cmd1.wasInterrupted && cmd1.hasEnded)
        group.execute()
        assert(cmd2.isInitialized)
        assert(cmd2.num == 2)
        while (!group.isFinished()) group.execute()
        assert(cmd2.isFinished() && cmd2.hasEnded && !cmd2.wasInterrupted)
    }

    @Test
    fun testWaitCommands() {
        if (logOutput) println("   **testWaitCommands**")
        val cmd = WaitCommand(1.0)
        val clock = NanoClock.system
        val start = clock.seconds()
        runBlocking(cmd)
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
        runBlocking(cmd)
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
        runBlocking(cmd)
        assert(number == 1)
    }

    @Test
    fun testTimeCommand() {
        if (logOutput) println("   **testTimeCommand**")
        var time = 0.0
        var compounded = 0.0
        val cmd = TimeCommand({ t, dt ->
            compounded += dt
            t > 3
        }, object : NanoClock {
            override fun seconds() = time
        })
        cmd.init()
        repeat(40) {
            cmd.execute()
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
        command.init()
        assert(count == 0)
        command.execute()
        assert(count == 1)
        while (!command.isFinished()) command.execute()
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
        command.init()
        assert(count == 0 && secondCount == 0)
        command.execute()
        assert(count == 1 && secondCount == 1)
        command.execute()
        assert(count == 1 && secondCount == 2)
        repeat(3) { command.execute() }
        assert(count == 1 && secondCount == 5)
        command.execute()
        assert(count == 2 && secondCount == 5)
        command.execute()
        assert(command.isFinished() && hasCancelled)
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