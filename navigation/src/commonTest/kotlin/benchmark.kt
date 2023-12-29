package utils.benchmark

import com.amarcolini.joos.serialization.format
import com.amarcolini.joos.util.NanoClock

fun benchmark(
    tests: Map<String, () -> Any?>,
    times: Int = 100_000_000,
    warmup: Int = 20
): Map<String, Double> {
    val clock = NanoClock.system
    return tests.mapValues { (_, test) ->
        repeat(warmup) { test() }
        val startTime = clock.seconds()
        repeat(times) { test() }
        clock.seconds() - startTime
    }
}

fun benchmark(
    times: Int = 100_000_000,
    warmup: Int = 20,
    vararg tests: Pair<String, () -> Any?>
) = benchmark(tests.toMap(), times, warmup)

fun benchmark(
    times: Int = 100_000_000,
    vararg tests: Pair<String, () -> Any?>
) = benchmark(tests.toMap(), times)

fun logBenchmark(
    tests: Map<String, () -> Any?>,
    times: Int = 100_000_000,
    warmup: Int = 20,
    logger: (String) -> Unit = ::println
) = benchmark(tests, times, warmup).forEach { (name, time) ->
    logger("$name: avg ${(time / times * 1000).format(6)} ms")
}

fun logBenchmark(
    vararg tests: Pair<String, () -> Any?>
) = logBenchmark(tests.toMap())

fun logBenchmark(
    times: Int,
    warmup: Int,
    logger: (String) -> Unit = ::println,
    vararg tests: Pair<String, () -> Any?>
) = logBenchmark(tests.toMap(), times, warmup, logger)

fun logBenchmark(
    times: Int,
    warmup: Int,
    vararg tests: Pair<String, () -> Any?>
) = logBenchmark(tests.toMap(), times, warmup)