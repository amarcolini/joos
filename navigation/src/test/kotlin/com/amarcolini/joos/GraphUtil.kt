package com.amarcolini.joos

import com.amarcolini.joos.path.ParametricCurve
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.util.DoubleProgression
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.QuickChart
import org.knowm.xchart.XYChart
import org.knowm.xchart.style.theme.MatlabTheme
import java.io.File
import java.nio.file.Paths

const val GRAPH_DIR = "./graphs/"
const val GRAPH_DPI = 300

object GraphUtil {

    fun saveGraph(name: String, graph: XYChart) {
        val file = File(Paths.get(GRAPH_DIR, name).toString())
        file.parentFile.mkdirs()

        BitmapEncoder.saveBitmapWithDPI(
            graph,
            file.absolutePath,
            BitmapEncoder.BitmapFormat.PNG,
            GRAPH_DPI
        )
    }

    fun saveCurve(name: String, curve: ParametricCurve, resolution: Int = 1000) {
        val displacementData = DoubleProgression.fromClosedInterval(
            0.0,
            curve.length(),
            resolution
        )
        val points = displacementData.map { curve[it] }
        val xData = points.map { it.x }.toDoubleArray()
        val yData = points.map { it.y }.toDoubleArray()

        val graph = QuickChart.getChart(name, "x", "y", name, xData, yData)
        graph.styler.isLegendVisible = false
        graph.styler.theme = MatlabTheme()

        saveGraph(name, graph)
    }

    fun saveProfile(name: String, profile: MotionProfile, resolution: Int = 1000) {
        val temporalData = DoubleProgression.fromClosedInterval(
            0.0,
            profile.duration(),
            resolution
        )
        val points = temporalData.map { profile[it] }
        val xData = points.map { it.x }.toDoubleArray()
        val yData = points.map { it.v }.toDoubleArray()

        val graph = QuickChart.getChart(name, "x", "v", name, xData, yData)
        graph.styler.isLegendVisible = false
        graph.styler.theme = MatlabTheme()

        saveGraph(name, graph)
    }
}