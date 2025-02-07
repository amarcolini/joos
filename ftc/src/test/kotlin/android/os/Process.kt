package android.os

class Process {
    companion object {
        @JvmStatic
        fun myTid() = Thread.currentThread().id.toInt()
    }
}