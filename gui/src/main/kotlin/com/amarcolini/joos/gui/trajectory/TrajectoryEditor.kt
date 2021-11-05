package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.rendering.TrajectoryRenderer
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.trajectory.config.*
import com.amarcolini.joos.util.DoubleProgression
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.chart.NumberAxis
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File
import java.nio.file.Paths
import kotlin.math.max
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties


internal class TrajectoryEditor(val renderer: TrajectoryRenderer) : View() {
    private val start = Start()
    val constraints: SimpleObjectProperty<TrajectoryConstraints> = SimpleObjectProperty(
        GenericConfig(30.0, 30.0, Math.toRadians(180.0), Math.toRadians(180.0))
    )
    val waypoints: ObservableList<Waypoint> = listOf(
        start
    ).toObservable()

    init {
        waypoints.onChange {
            renderer.trajectory = it.list.toTrajectory(constraints.value).first
        }
        constraints.onChange {
            renderer.trajectory = waypoints.toTrajectory(constraints.value).first
        }
    }

    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tab("Path") {
            listview(waypoints) {
                fitToParentHeight()
                isEditable = true
                cellFormat {
                    graphic = textflow {
                        it.isBad.addListener { _, _, newValue ->
                            togglePseudoClass(Theme.error.name, newValue)
                        }
                        togglePseudoClass(Theme.error.name, it.isBad.value)
                        text("!!") {
                            removeWhen { !it.isBad }
                            addClass(Theme.errorText)
                        }
                        text(it::class.simpleName?.replaceFirstChar {
                            it.lowercase()
                        }) {
                            addClass(Theme.valueText)
                        }
                        text("(")
                        val properties = it::class.memberProperties
                            .filter { it.visibility == KVisibility.PUBLIC }
                            .filterIsInstance<KMutableProperty<*>>()
                        properties.forEachIndexed { i, it ->
                            text("${it.name}=") {
                                addClass(Theme.propertyText)
                            }
                            val value = it.call(item)
                            val valueText = text(value.toString()) {
                                removeWhen { editingProperty() }
                            }
                            textfield(value.toString()) {
                                textFormatter = when (value) {
                                    is Degree -> TextFormatter(DegreeStringConverter(), value)
                                    is Double -> TextFormatter(DoubleStringConverter(), value)
                                    is Vector2d -> TextFormatter(
                                        Vector2dStringConverter(),
                                        value
                                    )
                                    is Pose2d -> TextFormatter(
                                        Pose2dStringConverter(),
                                        value
                                    )
                                    else -> textFormatter
                                }
                                removeWhen { editingProperty().not() }
                                whenVisible { requestFocus() }
                                action {
                                    it.setter.call(item, textFormatter.value)
                                    valueText.text = it.call(item).toString()
                                    renderer.trajectory =
                                        waypoints.toTrajectory(constraints.value).first
                                    commitEdit(item)
                                }
                            }
                            if (i < properties.size - 1) {
                                text(", ")
                            }
                        }
                        text(")")
                        children.forEach {
                            it.addClass(Theme.editorText)
                        }
                        contextmenu {
                            var index = waypoints.indexOf(it) + 1
                            var lastPose =
                                if (waypoints.indexOf(it) > 1) waypoints.toTrajectory(constraints.value).first.end()
                                else start.pose
                            var lastTangent = Degree(lastPose.heading, false)
                            fun update() {
                                index = waypoints.indexOf(it) + 1
                                lastPose =
                                    if (index > 1) waypoints.toTrajectory(constraints.value).first.end()
                                    else start.pose
                                lastTangent = Degree(lastPose.heading, false)
                            }
                            menu("_New") {
                                item("Wait").action {
                                    update()
                                    waypoints.add(index, Wait())
                                }
                                item("Turn").action {
                                    update()
                                    waypoints.add(index, Turn())
                                }
                                menu("Spline") {
                                    item("SplineTo").action {
                                        update()
                                        waypoints.add(index, SplineTo(lastPose.vec(), lastTangent))
                                    }
                                    item("SplineToConstantHeading").action {
                                        update()
                                        waypoints.add(
                                            index,
                                            SplineToConstantHeading(lastPose.vec(), lastTangent)
                                        )
                                    }
                                    item("SplineToLinearHeading").action {
                                        update()
                                        waypoints.add(
                                            index,
                                            SplineToLinearHeading(lastPose, lastTangent)
                                        )
                                    }
                                    item("SplineToSplineHeading").action {
                                        update()
                                        waypoints.add(
                                            index,
                                            SplineToSplineHeading(lastPose, lastTangent)
                                        )
                                    }
                                }
                                menu("Line") {
                                    item("StrafeTo").action {
                                        update()
                                        waypoints.add(index, StrafeTo(lastPose.vec()))
                                    }
                                    item("Forward").action {
                                        update()
                                        waypoints.add(index, Forward())
                                    }
                                    item("Back").action {
                                        update()
                                        waypoints.add(index, Back())
                                    }
                                    item("StrafeLeft").action {
                                        update()
                                        waypoints.add(index, StrafeLeft())
                                    }
                                    item("StrafeRight").action {
                                        update()
                                        waypoints.add(index, StrafeRight())
                                    }
                                    item("LineTo").action {
                                        update()
                                        waypoints.add(index, LineTo(lastPose.vec()))
                                    }
                                    item("LineToConstantHeading").action {
                                        update()
                                        waypoints.add(index, LineToConstantHeading(lastPose.vec()))
                                    }
                                    item("LineToLinearHeading").action {
                                        update()
                                        waypoints.add(index, LineToLinearHeading(lastPose))
                                    }
                                    item("LineToSplineHeading").action {
                                        update()
                                        waypoints.add(index, LineToSplineHeading(lastPose))
                                    }
                                }
                            }
                            separator()
                            if (it !is Start) {
                                item("_Delete").action {
                                    waypoints.remove(it)
//                                    if (waypoints.size > 1) {
//                                        renderer.trajectory = waypoints.toTrajectory().first
//                                    }
                                }
                            } else {
                                item("_Delete All").action {
                                    waypoints.removeIf { it !is Start }
//                                    renderer.trajectory = waypoints.toTrajectory().first
                                }
                            }
                        }
                    }
                }
            }
        }
        tab("Profile") {
            scrollpane {
                vbox {
                    linechart("Profile Graph", NumberAxis(), NumberAxis()) {
                        createSymbols = false
                        legendSide = Side.TOP
                        fun update() {
                            data.clear()
                            if (waypoints.isNotEmpty()) {
                                val trajectory = waypoints.toTrajectory(constraints.value).first
                                val progression =
                                    DoubleProgression.fromClosedInterval(
                                        0.0,
                                        trajectory.duration(),
                                        1000
                                    )
                                multiseries("ẋ", "ẏ", "ω") {
                                    progression.forEach {
                                        val result = trajectory.velocity(it)
                                        data(it, result.x, result.y, result.heading)
                                    }
                                }
                            }
                        }
                        waypoints.onChange { update() }
                        constraints.onChange { update() }
                        line {
                            stroke = Color.TRANSPARENT
                            renderer.timeProperty.onChange {
                                stroke = renderer.theme.value.text
                                val xPos = this.parent.sceneToLocal(
                                    xAxis.localToScene(
                                        xAxis.getDisplayPosition(it),
                                        0.0
                                    )
                                ).x
                                val top = this.parent.sceneToLocal(
                                    yAxis.localToScene(
                                        0.0,
                                        yAxis.boundsInLocal.maxY
                                    )
                                ).y
                                val bottom = this.parent.sceneToLocal(
                                    yAxis.localToScene(
                                        0.0,
                                        yAxis.boundsInLocal.minY
                                    )
                                ).y
                                startX = xPos
                                endX = xPos
                                startY = top
                                endY = bottom
                            }
                            waypoints.onChange {
                                isVisible = it.list.size > 1
                            }
                        }
                    }
                    form {
                        fieldset("Constraints", labelPosition = Orientation.VERTICAL) {
                            val type = SimpleObjectProperty(constraints.value.type)

                            combobox<TrajectoryConstraints.DriveType>(
                                type,
                                TrajectoryConstraints.DriveType.values().toList()
                            )

                            val maxRPM = SimpleDoubleProperty(312.0)
                            val trackWidth = SimpleDoubleProperty(1.0)
                            val wheelBase = SimpleDoubleProperty(1.0)
                            val lateralMultiplier = SimpleDoubleProperty(1.0)
                            val maxVel =
                                SimpleDoubleProperty(30.0)
                            val maxAccel =
                                SimpleDoubleProperty(30.0)
                            val maxAngVel =
                                SimpleObjectProperty(Degree(constraints.value.maxAngVel, false))
                            val maxAngAccel =
                                SimpleObjectProperty(Degree(constraints.value.maxAngAccel, false))
                            val maxAngJerk =
                                SimpleObjectProperty(Degree(constraints.value.maxAngJerk, false))

                            constraints.onChange {
                                val current = constraints.value
                                type.set(current.type)
                                if (current !is GenericConfig) {
                                    maxRPM.set((current as MecanumConfig).maxRPM)
                                    trackWidth.set(current.trackWidth)
                                }
                                if (current is MecanumConfig) {
                                    wheelBase.set(current.wheelBase)
                                    lateralMultiplier.set(current.lateralMultiplier)
                                }
                                if (current is SwerveConfig) {
                                    wheelBase.set(current.wheelBase)
                                }
                            }

                            fun update() {
                                constraints.set(
                                    when (type.value) {
                                        TrajectoryConstraints.DriveType.GENERIC -> GenericConfig(
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value.radians,
                                            maxAngAccel.value.radians,
                                            maxAngJerk.value.radians
                                        )
                                        TrajectoryConstraints.DriveType.MECANUM -> MecanumConfig(
                                            maxRPM.value,
                                            trackWidth.value,
                                            wheelBase.value,
                                            lateralMultiplier.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value.radians,
                                            maxAngAccel.value.radians,
                                            maxAngJerk.value.radians
                                        )
                                        TrajectoryConstraints.DriveType.SWERVE -> SwerveConfig(
                                            maxRPM.value,
                                            trackWidth.value,
                                            wheelBase.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value.radians,
                                            maxAngAccel.value.radians,
                                            maxAngJerk.value.radians
                                        )
                                        TrajectoryConstraints.DriveType.TANK -> TankConfig(
                                            maxRPM.value,
                                            trackWidth.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value.radians,
                                            maxAngAccel.value.radians,
                                            maxAngJerk.value.radians
                                        )
                                    }
                                )
                            }

                            type.onChange { update() }

                            textflow {
                                text("maxRPM: ").addClass(Theme.propertyText)
                                textfield(maxRPM, DoubleStringConverter()).action(::update)
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it != TrajectoryConstraints.DriveType.GENERIC).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("trackWidth: ").addClass(Theme.propertyText)
                                textfield(trackWidth, DoubleStringConverter()).action(::update)
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it != TrajectoryConstraints.DriveType.GENERIC).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("wheelBase: ").addClass(Theme.propertyText)
                                textfield(wheelBase, DoubleStringConverter()).action(::update)
                                addClass(Theme.padding)
                                visibleWhen {
                                    type.select {
                                        (it == TrajectoryConstraints.DriveType.MECANUM
                                                || it == TrajectoryConstraints.DriveType.SWERVE).toProperty()
                                    }
                                }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("lateralMultiplier: ").addClass(Theme.propertyText)
                                textfield(
                                    lateralMultiplier,
                                    DoubleStringConverter()
                                ).action(::update)
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it == TrajectoryConstraints.DriveType.MECANUM).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("maxVel: ").addClass(Theme.propertyText)
                                textfield(maxVel, DoubleStringConverter()).action(::update)
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAccel: ").addClass(Theme.propertyText)
                                textfield(maxAccel, DoubleStringConverter()).action(::update)
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngVel: ").addClass(Theme.propertyText)
                                textfield(maxAngVel, DegreeStringConverter()).action(::update)
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngAccel: ").addClass(Theme.propertyText)
                                textfield(maxAngAccel, DegreeStringConverter()).action(::update)
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngJerk: ").addClass(Theme.propertyText)
                                textfield(maxAngJerk, DegreeStringConverter()).action(::update)
                                addClass(Theme.padding)
                            }
                        }
                    }
                }
                content.setOnScroll {
                    val diff = max(1.0, content.boundsInLocal.height - height)
                    vvalue += -it.deltaY / diff
                }
            }
        }
        contextmenu {
            menu("_Save") {
                item("To _File").action(::saveToFile)
                item("To _Java").action(::saveToJava)
                item("To _Kotlin").action(::saveToKotlin)
            }
            separator()
            menu("_Load") {
                item("From _File").action(::loadFile)
            }
        }
    }

    fun saveToFile() {
        val directory = chooseDirectory(
            "Choose Directory to Save Trajectory",
            File(Paths.get("").toAbsolutePath().toUri())
        ) ?: return
        val chooser = FileChooser()
        chooser.initialDirectory = directory
        chooser.extensionFilters += FileChooser.ExtensionFilter(
            "YAML files (*.yaml)",
            "*.yaml"
        )
        chooser.initialFileName = "untitled_trajectory.yaml"
        chooser.title = "Save Trajectory"
        val file = chooser.showSaveDialog(null)
        TrajectoryConfigManager.saveConfig(waypoints.toConfig(constraints.value), file)
        alert(
            Alert.AlertType.INFORMATION,
            "Trajectory successfully saved as ${file.name}!",
            null,
            owner = currentWindow
        )
    }

    fun saveToJava() {
        var string = """new TrajectoryBuilder(
                        |   ${start.pose.toJava()},
                        |   ${start.tangent.radians},
                        |   new ${constraints.value.toKotlin()}
                        |)
                    """.trimMargin()
        waypoints.filter { it !is Start }.forEach { waypoint ->
            string += "\n.${waypoint.toJava()}"
        }
        string += "\n.build();"
        clipboard.setContent {
            putString(string)
        }
        alert(
            Alert.AlertType.INFORMATION,
            "Trajectory copied to clipboard!",
            null,
            title = "Success",
            owner = currentWindow
        )
    }

    fun saveToKotlin() {
        var string = """TrajectoryBuilder(
                        |   ${start.pose.toKotlin()},
                        |   ${start.tangent.radians},
                        |   ${constraints.value.toKotlin()}
                        |)
                    """.trimMargin()
        waypoints.filter { it !is Start }.forEach { waypoint ->
            string += "\n.${waypoint.toKotlin()}"
        }
        string += "\n.build()"
        clipboard.setContent {
            putString(string)
        }
        alert(
            Alert.AlertType.INFORMATION,
            "Trajectory copied to clipboard!",
            null,
            title = "Success",
            owner = currentWindow
        )
    }

    fun loadFile(trajectory: File? = null) {
        val chooser = FileChooser()
        chooser.initialDirectory =
            File(Paths.get("").toAbsolutePath().toUri())
        chooser.extensionFilters += FileChooser.ExtensionFilter(
            "YAML files (*.yaml)",
            "*.yaml"
        )
        chooser.title = "Load Trajectory"
        val file = trajectory ?: chooser.showOpenDialog(null) ?: return
        val config = try {
            TrajectoryConfigManager.loadConfig(file)
        } catch (e: Exception) {
            null
        }
        if (config != null) {
            waypoints.clear()
            waypoints += config.toWaypoints()
            renderer.trajectory = config.toTrajectory()
        } else alert(
            Alert.AlertType.ERROR,
            "Unable to load ${file.name}.", null,
            ButtonType.OK,
            title = "Error",
            owner = currentWindow
        )
    }
}