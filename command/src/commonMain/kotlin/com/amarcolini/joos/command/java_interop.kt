package com.amarcolini.joos.command

fun interface Runnable {
    fun run()
}

fun interface CommandEnd {
    fun end(interrupted: Boolean)
}