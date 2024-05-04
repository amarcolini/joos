package com.amarcolini.joos

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PositionPath
import com.amarcolini.joos.util.CircularGVF
import com.amarcolini.joos.util.DoubleProgression
import com.amarcolini.joos.util.PathGVF
import com.amarcolini.joos.util.VectorField
import org.jetbrains.letsPlot.geom.extras.arrow
import org.jetbrains.letsPlot.geom.geomPath
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomSegment
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleColorViridis
import kotlin.math.roundToInt

object LetsPlotUtil {
    fun plotVectorField(
        vf: VectorField,
        p1: Vector2d,
        p2: Vector2d,
        resolution: Double = 1.0
    ): Plot {
        val vectorData = arrayListOf(
            arrayListOf<Double>(),
            arrayListOf(),
            arrayListOf(),
            arrayListOf(),
            arrayListOf(),
        )
        val count = (p2 - p1) / resolution
        DoubleProgression.fromClosedInterval(p1.x, p2.x, count.x.roundToInt()).forEach { x ->
            DoubleProgression.fromClosedInterval(p1.y, p2.y, count.y.roundToInt()).forEach { y ->
                val source = Vector2d(x, y)
                val vector = vf[x, y]
                val length = vector.norm()
                vectorData[4].add(length)
                val dest = source + vector / length * 0.7
                vectorData[0].add(source.x)
                vectorData[1].add(source.y)
                vectorData[2].add(dest.x)
                vectorData[3].add(dest.y)
            }
        }

        val fig = letsPlot(
            mapOf(
                "x0" to vectorData[0],
                "y0" to vectorData[1],
                "x1" to vectorData[2],
                "y1" to vectorData[3],
                "color" to vectorData[4]
            )
        ) +
                geomSegment(
                    size = resolution - 0.4,
                    arrow = arrow(45, 1, "last", "closed")
                ) {
                    x = "x0"
                    y = "y0"
                    xend = "x1"
                    yend = "y1"
                    color = "color"
                } +
                scaleColorViridis(labels = listOf("color"))
        if (vf is PathGVF) {
            fig + createPathLayer(vf.path)
        }
        if (vf is CircularGVF) {
            fig + geomPoint(
                x = vf.center.x,
                y = vf.center.y,
                shape = 1,
                size = vf.radius,
                stroke = 1.0,
                color = "black"
            )
        }
        return fig
    }

    fun createPathLayer(color: Any?, points: List<Vector2d>): geomPath {
        return geomPath(
            data = mapOf("x" to points.map { it.x }, "y" to points.map { it.y }),
            color = color,
            size = 1.0
        ) { x = "x"; y = "y" }
    }

    fun createPathLayer(length: Double, color: Any?, points: (Double) -> Vector2d): geomPath {
        return createPathLayer(color, DoubleProgression.fromClosedInterval(0.0, length, 40)
            .map { points(it) })
    }

    fun createPathLayer(path: Path, color: Any? = "black"): geomPath =
        createPathLayer(path.length(), color) { path[it].vec() }

    fun createPathLayer(path: PositionPath, color: Any? = "black"): geomPath =
        createPathLayer(path.length(), color) { path[it] }
}