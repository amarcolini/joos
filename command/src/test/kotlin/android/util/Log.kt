package android.util

class Log {
    companion object {
        const val ASSERT = 7
        const val DEBUG = 3
        const val ERROR = 6
        const val INFO = 4
        const val VERBOSE = 2
        const val WARN = 5

        private val priorityMap = mapOf(
            ASSERT to "ASSERT",
            DEBUG to "DEBUG",
            ERROR to "ERROR",
            INFO to "INFO",
            VERBOSE to "VERBOSE",
            WARN to "WARN"
        )

        @JvmStatic
        fun i(tag: String?, msg: String): Int = println(INFO, tag, msg)

        @JvmStatic
        fun println(priority: Int, tag: String?, msg: String): Int {
            println("[${priorityMap[priority]}]: ${tag?.plus(": ") ?: ""}$msg")
            return 0
        }
    }
}