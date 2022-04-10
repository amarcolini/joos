package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.Global
import com.amarcolini.joos.gui.rendering.Renderer
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.trajectory.config.*
import com.amarcolini.joos.util.DoubleProgression
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TabPane
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File
import java.nio.file.Paths
import kotlin.math.max
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


internal class TrajectoryEditor() : View() {
    val renderer: Renderer = Renderer()

    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tab("Path") {
            listview(renderer.waypoints) {
                fitToParentHeight()
                isEditable = true
                cellFormat {
                    graphic = textflow {
                        it.error.addListener { _, _, newValue ->
                            togglePseudoClass(Theme.error.name, newValue != null)
                        }
                        togglePseudoClass(Theme.error.name, it.error.value != null)
                        text("!!") {
                            removeWhen {
                                it.error.isNull
                            }
                            addClass(Theme.errorText)
                        }
                        tooltipProperty().bind(it.error.select {
                            (if (it == null) null else tooltip(
                                it
                            )).toProperty()
                        })
                        text(it::class.simpleName?.replaceFirstChar {
                            it.lowercase()
                        }) {
                            addClass(Theme.valueText)
                        }
                        text("(")
                        val properties = it::class.declaredMemberProperties
                            .filter { it.visibility == KVisibility.PUBLIC }
                            .filterIsInstance<KMutableProperty<*>>()
                        properties.forEachIndexed { i, it ->
                            text("${it.name}=") {
                                addClass(Theme.propertyText)
                            }
                            val value = it.call(item)
                            val valueText = text(
                                when (value) {
                                    is Angle -> AngleStringConverter().toString(value)
                                    is Double -> DoubleStringConverter().toString(value)
                                    is Vector2d -> Vector2dStringConverter().toString(value)
                                    is Pose2d -> Pose2dStringConverter().toString(value)
                                    else -> null
                                }
                            ) {
                                removeWhen { editingProperty() }
                            }
                            textfield(value.toString()) {
                                textFormatter = when (value) {
                                    is Angle -> TextFormatter(AngleStringConverter(), value)
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
                                focusedProperty().onChange { focused ->
                                    if (!focused) {
                                        it.setter.call(item, textFormatter.value)
                                        valueText.text = it.call(item).toString()
                                        renderer.trajectory = WaypointTrajectory(
                                            renderer.waypoints,
                                            renderer.constraints.value
                                        )
//                                        commitEdit(item)
                                    }
                                }
                                action {
                                    it.setter.call(item, textFormatter.value)
                                    valueText.text = it.call(item).toString()
                                    renderer.trajectory = WaypointTrajectory(
                                        renderer.waypoints,
                                        renderer.constraints.value
                                    )
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
                            var index = renderer.waypoints.indexOf(it) + 1
                            var (lastPose, lastTangent) = renderer.waypoints.mapPose().last().second

                            fun update() {
                                index = renderer.waypoints.indexOf(it) + 1
                                val (newPose, newTangent) = renderer.waypoints.mapPose()
                                    .last().second
                                lastPose = newPose + Pose2d(0.01, 0.01)
                                lastTangent = newTangent
                            }
                            menu("_New") {
                                item("Wait").action {
                                    update()
                                    renderer.waypoints.add(index, Wait())
                                }
                                item("Turn").action {
                                    update()
                                    renderer.waypoints.add(index, Turn())
                                }
                                menu("Spline") {
                                    item("SplineTo").action {
                                        update()
                                        renderer.waypoints.add(
                                            index,
                                            SplineTo(lastPose.vec(), lastTangent)
                                        )
                                    }
                                    item("SplineToConstantHeading").action {
                                        update()
                                        renderer.waypoints.add(
                                            index,
                                            SplineToConstantHeading(lastPose.vec(), lastTangent)
                                        )
                                    }
                                    item("SplineToLinearHeading").action {
                                        update()
                                        renderer.waypoints.add(
                                            index,
                                            SplineToLinearHeading(lastPose, lastTangent)
                                        )
                                    }
                                    item("SplineToSplineHeading").action {
                                        update()
                                        renderer.waypoints.add(
                                            index,
                                            SplineToSplineHeading(lastPose, lastTangent)
                                        )
                                    }
                                }
                                menu("Line") {
                                    item("StrafeTo").action {
                                        update()
                                        renderer.waypoints.add(index, StrafeTo(lastPose.vec()))
                                    }
                                    item("Forward").action {
                                        update()
                                        renderer.waypoints.add(index, Forward())
                                    }
                                    item("Back").action {
                                        update()
                                        renderer.waypoints.add(index, Back())
                                    }
                                    item("StrafeLeft").action {
                                        update()
                                        renderer.waypoints.add(index, StrafeLeft())
                                    }
                                    item("StrafeRight").action {
                                        update()
                                        renderer.waypoints.add(index, StrafeRight())
                                    }
                                    item("LineTo").action {
                                        update()
                                        renderer.waypoints.add(index, LineTo(lastPose.vec()))
                                    }
                                    item("LineToConstantHeading").action {
                                        update()
                                        renderer.waypoints.add(
                                            index,
                                            LineToConstantHeading(lastPose.vec())
                                        )
                                    }
                                    item("LineToLinearHeading").action {
                                        update()
                                        renderer.waypoints.add(index, LineToLinearHeading(lastPose))
                                    }
                                    item("LineToSplineHeading").action {
                                        update()
                                        renderer.waypoints.add(index, LineToSplineHeading(lastPose))
                                    }
                                }
                            }
                            separator()
                            if (it !is Start) {
                                item("_Delete").action {
                                    renderer.waypoints.remove(it)
                                    if (renderer.waypoints.size > 1) {
                                        renderer.trajectory = WaypointTrajectory(
                                            renderer.waypoints,
                                            renderer.constraints.value
                                        )
                                    }
                                }
                            } else {
                                item("_Delete All").action {
                                    renderer.waypoints.removeIf { it !is Start }
                                    renderer.trajectory = WaypointTrajectory(
                                        renderer.waypoints,
                                        renderer.constraints.value
                                    )
                                }
                            }
                        }
                    }
                }
                setOnKeyPressed {
                    if ((it.code == KeyCode.BACK_SPACE || it.code == KeyCode.DELETE) && selectedItem !is Start && selectedItem != null) {
                        renderer.waypoints.remove(selectedItem)
                        if (renderer.waypoints.size > 1) {
                            renderer.trajectory = WaypointTrajectory(
                                renderer.waypoints,
                                renderer.constraints.value
                            )
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
                            if (renderer.waypoints.isNotEmpty()) {
                                val trajectory = renderer.trajectory.trajectory
                                if (trajectory != null) {
                                    data.clear()
                                    val progression =
                                        DoubleProgression.fromClosedInterval(
                                            0.0,
                                            trajectory.duration(),
                                            (trajectory.duration() / 0.1).toInt()
                                        )
                                    multiseries("ẋ", "ẏ", "ω") {
                                        progression.forEach {
                                            val result = trajectory.velocity(it)
                                            data(it, result.x, result.y, result.heading.radians)
                                        }
                                    }
                                }
                            }
                        }
                        renderer.trajectoryRenderer.trajectoryProperty.onChange {
                            if (!renderer.trajectoryRenderer.beingDragged)
                                update()
                        }
                        line {
                            stroke = Global.theme.value.text
                            Global.theme.onChange {
                                stroke = it?.text
                            }
                            renderer.robot.timeProperty.onChange {
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
                            renderer.waypoints.onChange {
                                isVisible = it.list.size > 1
                            }
                        }
                    }
                    form {
                        fieldset("Constraints", labelPosition = Orientation.VERTICAL) {
                            val type = SimpleObjectProperty(renderer.constraints.value.type)

                            combobox<TrajectoryConstraints.DriveType>(
                                type,
                                TrajectoryConstraints.DriveType.values().toList()
                            )

                            val maxWheelVel = SimpleDoubleProperty(312.0)
                            val trackWidth = SimpleDoubleProperty(1.0)
                            val wheelBase = SimpleDoubleProperty(1.0)
                            val lateralMultiplier = SimpleDoubleProperty(1.0)
                            val maxVel =
                                SimpleDoubleProperty(30.0)
                            val maxAccel =
                                SimpleDoubleProperty(30.0)
                            val maxAngVel =
                                SimpleObjectProperty(
                                    renderer.constraints.value.maxAngVel
                                )
                            val maxAngAccel =
                                SimpleObjectProperty(
                                    renderer.constraints.value.maxAngAccel,
                                )
                            val maxAngJerk =
                                SimpleObjectProperty(
                                    renderer.constraints.value.maxAngJerk,
                                )

                            renderer.constraints.onChange {
                                val current = renderer.constraints.value
                                type.set(current.type)
                                when (current) {
                                    is GenericConstraints -> {
                                        maxVel.set(current.maxVel)
                                        maxAccel.set(current.maxAccel)
                                    }
                                    is MecanumConstraints -> {
                                        maxVel.set(current.maxVel)
                                        maxAccel.set(current.maxAccel)
                                        maxWheelVel.set(current.maxWheelVel)
                                        trackWidth.set(current.trackWidth)
                                        lateralMultiplier.set(current.lateralMultiplier)
                                    }
                                    is SwerveConstraints -> {
                                        maxVel.set(current.maxVel)
                                        maxAccel.set(current.maxAccel)
                                        maxWheelVel.set(current.maxWheelVel)
                                        trackWidth.set(current.trackWidth)
                                    }
                                    is TankConstraints -> {
                                        maxVel.set(current.maxVel)
                                        maxAccel.set(current.maxAccel)
                                        maxWheelVel.set(current.maxWheelVel)
                                        trackWidth.set(current.trackWidth)
                                    }
                                }
                            }

                            fun update() {
                                renderer.constraints.set(
                                    when (type.value) {
                                        TrajectoryConstraints.DriveType.GENERIC, null -> GenericConstraints(
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value,
                                            maxAngAccel.value,
                                            maxAngJerk.value
                                        )
                                        TrajectoryConstraints.DriveType.MECANUM -> MecanumConstraints(
                                            maxWheelVel.value,
                                            trackWidth.value,
                                            wheelBase.value,
                                            lateralMultiplier.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value,
                                            maxAngAccel.value,
                                            maxAngJerk.value
                                        )
                                        TrajectoryConstraints.DriveType.SWERVE -> SwerveConstraints(
                                            maxWheelVel.value,
                                            trackWidth.value,
                                            wheelBase.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value,
                                            maxAngAccel.value,
                                            maxAngJerk.value
                                        )
                                        TrajectoryConstraints.DriveType.DIFF_SWERVE -> DiffSwerveConstraints(
                                            maxWheelVel.value,
                                            trackWidth.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value,
                                            maxAngAccel.value,
                                            maxAngJerk.value
                                        )
                                        TrajectoryConstraints.DriveType.TANK -> TankConstraints(
                                            maxWheelVel.value,
                                            trackWidth.value,
                                            maxVel.value,
                                            maxAccel.value,
                                            maxAngVel.value,
                                            maxAngAccel.value,
                                            maxAngJerk.value
                                        )
                                    }
                                )
                            }

                            type.onChange { update() }

                            textflow {
                                text("maxWheelVel: ").addClass(Theme.propertyText)
                                textfield(maxWheelVel, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(DoubleStringConverter(), maxWheelVel.value)
                                }
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it != TrajectoryConstraints.DriveType.GENERIC).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("trackWidth: ").addClass(Theme.propertyText)
                                textfield(trackWidth, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(DoubleStringConverter(), trackWidth.value)
                                }
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it != TrajectoryConstraints.DriveType.GENERIC).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("wheelBase: ").addClass(Theme.propertyText)
                                textfield(wheelBase, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(DoubleStringConverter(), wheelBase.value)
                                }
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
                                textfield(lateralMultiplier, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(
                                            DoubleStringConverter(),
                                            lateralMultiplier.value
                                        )
                                }
                                addClass(Theme.padding)
                                visibleWhen { type.select { (it == TrajectoryConstraints.DriveType.MECANUM).toProperty() } }
                                managedProperty().bind(visibleProperty())
                            }
                            textflow {
                                text("maxVel: ").addClass(Theme.propertyText)
                                textfield(maxVel, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(DoubleStringConverter(), maxVel.value)
                                }
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAccel: ").addClass(Theme.propertyText)
                                textfield(maxAccel, DoubleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(DoubleStringConverter(), maxAccel.value)
                                }
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngVel: ").addClass(Theme.propertyText)
                                textfield(maxAngVel, AngleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(AngleStringConverter(), maxAngVel.value)
                                }
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngAccel: ").addClass(Theme.propertyText)
                                textfield(maxAngAccel, AngleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(AngleStringConverter(), maxAngAccel.value)
                                }
                                addClass(Theme.padding)
                            }
                            textflow {
                                text("maxAngJerk: ").addClass(Theme.propertyText)
                                textfield(maxAngJerk, AngleStringConverter()) {
                                    action(::update)
                                    textFormatter =
                                        TextFormatter(AngleStringConverter(), maxAngJerk.value)
                                }
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
            File(Paths.get("").toAbsolutePath().toUri()), currentWindow
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
        TrajectoryConfigManager.saveConfig(
            renderer.waypoints.toConfig(renderer.constraints.value),
            file
        )
        alert(
            Alert.AlertType.INFORMATION,
            "Trajectory successfully saved as ${file.name}!",
            null,
            owner = currentWindow
        )
    }

    fun saveToJava() {
        val start = renderer.trajectory.waypoints[0] as Start
        var string = """new TrajectoryBuilder(
                        |   ${start.pose.toJava()},
                        |   ${start.tangent.toJava()},
                        |   ${renderer.constraints.value.toJava()}
                        |)
                    """.trimMargin()
        renderer.waypoints.filter { it !is Start }.forEach { waypoint ->
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
        val start = renderer.trajectory.waypoints[0] as Start
        var string = """TrajectoryBuilder(
                        |   ${start.pose.toKotlin()},
                        |   ${start.tangent.toKotlin()},
                        |   ${renderer.constraints.value.toKotlin()}
                        |)
                    """.trimMargin()
        renderer.waypoints.filter { it !is Start }.forEach { waypoint ->
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
        val file = trajectory ?: chooser.showOpenDialog(currentWindow) ?: return
        val config = try {
            TrajectoryConfigManager.loadConfig(file)
        } catch (e: Exception) {
            null
        }
        if (config != null) {
            renderer.waypoints.setAll(config.toWaypoints())
            renderer.constraints.set(config.constraints)
            renderer.trajectory = WaypointTrajectory(renderer.waypoints, config.constraints)
        } else alert(
            Alert.AlertType.ERROR,
            "Unable to load ${file.name}.", null,
            ButtonType.OK,
            title = "Error",
            owner = currentWindow
        )
    }
}