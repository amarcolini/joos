package util

import io.nacular.doodle.utils.ObservableList
import io.nacular.doodle.utils.ObservableSet

class ObservableMap<K, V>(
    private val base: MutableMap<K, V>, private val onUpdate: () -> Unit
) : MutableMap<K, V> by base {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = ObservableSet(base.entries).apply {
            changed += { _, _, _ ->
                onUpdate()
            }
        }

    override val keys: MutableSet<K>
        get() = ObservableSet(base.keys).apply {
            changed += { _, _, _ ->
                onUpdate()
            }
        }

    override val values: MutableCollection<V>
        get() = ObservableList(base.values.toList()).apply {
            changed += { _, _ ->
                onUpdate()
            }
        }

    override fun clear() {
        if (size > 0) {
            base.clear()
            onUpdate()
        }
    }

    override fun put(key: K, value: V): V? {
        val prev = base.put(key, value)
        if (prev !== value) onUpdate()
        return prev
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) put(key, value)
    }

    override fun remove(key: K): V? {
        if (containsKey(key)) {
            val prev = base.remove(key)
            onUpdate()
            return prev
        }
        return null
    }

    override fun equals(other: Any?): Boolean = base == other

    override fun hashCode(): Int = base.hashCode()

    override fun toString(): String = base.toString()
}