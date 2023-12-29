package com.amarcolini.joos

import com.amarcolini.joos.util.DoubleProgression
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomABLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.letsPlot
import utils.benchmark.logBenchmark
import kotlin.test.Test

fun linearRegression1Optimized(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    val n = x.size
    require(n == y.size)
    var xsum = 0.0
    var ysum = 0.0
    var xysum = 0.0
    var x2sum = 0.0
    for (i in 0..<n) {
        val xi = x[i]
        val yi = y[i]
        xsum += xi
        ysum += yi
        xysum += xi * yi
        x2sum += xi * xi
    }
    val slope = (n * xysum - xsum * ysum) / (n * x2sum - xsum * xsum)
    val intercept = (ysum - slope * xsum) / n
    return slope to intercept
}

fun linearRegression1(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    require(x.size == y.size) { "x and y lists must have the same size!" }
    val xsum = x.sum()
    val ysum = y.sum()
    val xysum = x.zip(y).sumOf { it.first * it.second }
    val x2sum = x.sumOf { it * it }
    val slope = (x.size * xysum - xsum * ysum) / (x.size * x2sum - xsum * xsum)
    val intercept = (ysum - slope * xsum) / x.size
    return slope to intercept
}

fun linearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    require(x.size == y.size)
    val xmean = x.average()
    val ymean = y.average()
    val xdiff = x.map { it - xmean }
    val numerator = xdiff.zip(y).sumOf { it.first * (it.second - ymean) }
    val denominator = xdiff.sumOf { it * it }
    val slope = numerator / denominator
    val intercept = ymean - slope * xmean
    return slope to intercept
}

fun linearRegressionOptimized(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    require(x.size == y.size)
    val xmean = x.average()
    val ymean = y.average()
    val xdiff = x.map { it - xmean }
    val numerator = y.reduceIndexed { i, acc, d ->
        xdiff[i] * (d - ymean) + acc
    }
    val denominator = xdiff.sumOf { it * it }
    val slope = numerator / denominator
    val intercept = ymean - slope * xmean
    return slope to intercept
}

fun linearRegressionOptimized2(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    val n = x.size
    require(n == y.size)
    var xsum = 0.0
    var ysum = 0.0
    for (i in 0..<n) {
        xsum += x[i]
        ysum += y[i]
    }
    xsum /= n
    ysum /= n
    var numerator = 0.0
    var denominator = 0.0
    for (i in 0..<n) {
        val xdiff = x[i] - xsum
        numerator += xdiff * (y[i] - ysum)
        denominator += xdiff * xdiff
    }
    val slope = numerator / denominator
    val intercept = ysum - slope * xsum
    return slope to intercept
}

fun linearRegressionNoIntercept(x: List<Double>, y: List<Double>): Double {
    val numerator = x.zip(y).sumOf { it.first * it.second }
    val denominator = x.sumOf { it * it }
    return numerator / denominator
}

class RegressionTest {
    companion object {
        val xData = DoubleProgression(0.0, 1.0, 100).toList()
        val yData = xData.map { it * 2.0 + 5.0 + (Math.random() - 0.5) * 20.0 }

        private fun getData(): Pair<List<Double>, List<Double>> {
            val x = DoubleProgression(0.0, 1.0 + Math.random(), 10000).toList()
            val y = x.map { it * (2.0 + Math.random()) + 5.0 + (Math.random() - 0.5) * 20.0 }
            return x to y
        }
    }

    @Test
    fun testRegression1() {
        val (slope, intercept) = linearRegression1(xData, yData)

        val plot = letsPlot(
            mapOf("x" to xData, "y" to yData)
        ) {
            x = "x"; y = "y"
        } + geomPoint() + geomABLine(slope = slope, intercept = intercept)
        ggsave(plot, "regression1.png")
    }

    @Test
    fun testRegression() {
        val (slope, intercept) = linearRegression(xData, yData)

        val plot = letsPlot(
            mapOf("x" to xData, "y" to yData)
        ) {
            x = "x"; y = "y"
        } + geomPoint() + geomABLine(slope = slope, intercept = intercept)
        ggsave(plot, "regression.png")
    }

    @Test
    fun testNoIntercept() {
        val slope = linearRegressionNoIntercept(xData, yData)

        val plot = letsPlot(
            mapOf("x" to xData, "y" to yData)
        ) {
            x = "x"; y = "y"
        } + geomPoint() + geomABLine(slope = slope, intercept = null)
        ggsave(plot, "nointercept.png")
    }

    @Test
    fun benchmarkRegression() {
        logBenchmark(
            times = 10_000,
            warmup = 20,
            "regression1" to {
                val (x, y) = getData()
                linearRegression1(x, y)
            },
            "regression" to {
                val (x, y) = getData()
                linearRegression(x, y)
            },
            "regressionOptimized" to {
                val (x, y) = getData()
                linearRegressionOptimized(x, y)
            },
            "regressionOptimized2" to {
                val (x, y) = getData()
                linearRegressionOptimized(x, y)
            },
            "regression1Optimized" to {
                val (x, y) = getData()
                linearRegressionOptimized(x, y)
            },
        )
    }
}