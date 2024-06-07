package com.amarcolini.joos.command.builders

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.CommandGroup
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.SequentialCommand
import com.amarcolini.joos.util.NanoClock

/**
 * A command that runs commands in sequence while scheduling other commands to run in parallel.
 *
 * @param sequentialCommand the commands to run sequentially
 * @param markers the commands to be scheduled in parallel.
 * All commands will be scheduled by the time the sequential commands finish.
 */
class MarkerCommand @JvmOverloads constructor(
    override var isInterruptable: Boolean,
    private val sequentialCommand: SequentialCommand,
    private val markers: List<Marker>,
    private val clock: NanoClock = NanoClock.system
) : CommandGroup(sequentialCommand.commands + markers.map { it.command }) {
    constructor(sequentialCommand: SequentialCommand, markers: List<Marker>) : this(
        sequentialCommand.isInterruptable,
        sequentialCommand,
        markers
    )

    constructor(commands: List<Command>, markers: List<Marker>) : this(
        SequentialCommand(*commands.toTypedArray()), markers
    )

    abstract class Marker(@JvmField val command: Command) {
        /**
         * Returns whether [command] should be scheduled based on the currently running command and the time.
         *
         * @param totalTime the time since the [MarkerCommand] has started running
         * @param relativeTime the time since the [currentCommand] has started running
         */
        abstract fun shouldSchedule(
            commandIndex: Int,
            currentCommand: Command,
            totalTime: Double,
            relativeTime: Double
        ): Boolean
    }

    private val currentMarkers = mutableListOf<Marker>()
    private var start: Double = 0.0
    private var relativeStart: Double = 0.0
    private var currentIndex = -1
    private var sequentialDone = false

    private fun updateIndex() {
        if (sequentialCommand.index != currentIndex) {
            relativeStart = clock.seconds()
            currentIndex = sequentialCommand.index
        }
        val currentCommand = sequentialCommand.commands[currentIndex]
        val totalTime = clock.seconds() - start
        val relativeTime = clock.seconds() - relativeStart
        currentMarkers.removeIf { marker ->
            val shouldSchedule =
                marker.shouldSchedule(currentIndex, currentCommand, totalTime, relativeTime) || sequentialDone
            if (shouldSchedule) scheduleQueue += marker.command
            shouldSchedule
        }
    }

    override fun add(command: Command): CommandGroup = MarkerCommand(
        isInterruptable, sequentialCommand.add(command), markers
    )

    override val requirements: Set<Component> by lazy {
        (markers.flatMap { it.command.requirements } + sequentialCommand.requirements).toSet()
    }

    override fun internalInit() {
        sequentialDone = false
        currentMarkers.clear()
        currentMarkers.addAll(markers)
        sequentialCommand.init()
        start = clock.seconds()
        updateIndex()
    }

    override fun isFinished(): Boolean {
        if (!sequentialDone) {
            sequentialCommand.execute()
            sequentialDone = sequentialCommand.isFinished()
        }
        updateIndex()
        return sequentialDone && scheduledCommands.isEmpty() && currentMarkers.isEmpty()
    }
}