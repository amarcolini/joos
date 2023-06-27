package utils.benchmark

import com.amarcolini.joos.util.NanoClock

fun benchmark(
    tests: Map<String, () -> Any?>,
    times: Int = 100_000_000
): Map<String, Double> {
    val clock = NanoClock.system()
    return tests.mapValues { (_, test) ->
        val startTime = clock.seconds()
        repeat(times) { test() }
        clock.seconds() - startTime
    }
}

fun logBenchmark(
    tests: Map<String, () -> Any?>,
    times: Int = 100_000_000,
    logger: (String) -> Unit = ::println
) = benchmark(tests, times).forEach { (name, time) -> logger("$name: $time") }

fun logBenchmark(
    vararg tests: Pair<String, () -> Any?>
) = logBenchmark(tests.toMap())

fun logBenchmark(
    times: Int,
    logger: (String) -> Unit,
    vararg tests: Pair<String, () -> Any?>
) = logBenchmark(tests.toMap(), times, logger)