package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.ValueProvider

class BetterArrayProvider<T>(private val array: () -> Array<*>, private vararg val indices: Int) :
    ValueProvider<T?> {
    @Suppress("UNCHECKED_CAST")
    override fun get(): T? {
        return try {
            getArrayRecursive(array(), indices) as T
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }

    override fun set(value: T?) {
        try {
            setArrayRecursive(array(), value, indices)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        }
    }

    companion object {
        @Throws(ArrayIndexOutOfBoundsException::class, IllegalAccessException::class)
        fun getArrayRecursive(array: Array<*>, indices: IntArray): Any? {
            var currentArray = array
            for (i in indices.dropLast(1)) {
                currentArray = currentArray[i] as Array<*>
            }
            return currentArray[indices.last()]
        }

        @Suppress("UNCHECKED_CAST")
        fun setArrayRecursive(array: Array<*>, value: Any?, indices: IntArray) {
            var currentArray = array
            for (i in indices.dropLast(1)) {
                currentArray = currentArray[i] as Array<*>
            }
            (currentArray as Array<Any?>)[indices.last()] = value
        }
    }
}