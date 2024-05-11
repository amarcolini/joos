package pathfinding

class PriorityQueue<E>(private val comparator: Comparator<in E>) {
    companion object {
        fun <E : Comparable<E>> fromComparable() = PriorityQueue<E> { a, b ->
            a.compareTo(b)
        }
    }

    val size get() = storage.size

    inner class Item internal constructor(
        val element: E,
        internal var index: Int = 0,
    ) {
        internal var removed = false

        override fun toString(): String {
            return (element to index).toString()
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is PriorityQueue<*>.Item) return false
            return (other.element == element && other.index == index)
        }

        override fun hashCode(): Int {
            var result = element?.hashCode() ?: 0
            result = 31 * result + index
            return result
        }
    }

    private val storage: MutableList<Item> = mutableListOf()

    fun insert(element: E): Item {
        val item = Item(element)
        storage.add(item)
        var i = storage.lastIndex
        while (i > 0) {
            val j = (i - 1) / 2
            val a = storage[i]
            val b = storage[j]
            if (comparator.compare(b.element, a.element) <= 0) break
            storage[i] = b; storage[j] = a
            b.index = i; a.index = j
            i = j
        }
        return item
    }

    operator fun plusAssign(element: E) {
        insert(element)
    }

    fun remove(item: Item): Boolean = if (storage[item.index] == item) {
        item.removed = true
        true
    } else false

    fun getItem(element: E): Item? {
        val index = storage.binarySearch(Item(element), { a, b -> comparator.compare(a.element, b.element) })
        if (index < 0) return null
        storage[index].let {
            if (it.element == element) return it
        }
        var i = index - 1
        while (i >= 0 && storage[index].let {
                if (it == element) return it
                else comparator.compare(element, it.element) == 0
            }) i--
        i = index + 1
        while (i <= storage.lastIndex && storage[index].let {
                if (it == element) return it
                else comparator.compare(element, it.element) == 0
            }) i++
        return null
    }

    fun remove(element: E): Boolean = getItem(element)?.let { remove(it) } ?: false

    fun top(): Item? = storage.firstOrNull()

    fun clear() {
        storage.clear()
    }

    @Throws(NoSuchElementException::class)
    tailrec fun pop(): E {
        val first = storage.first()
        val last = storage.removeLast()
        if (storage.isNotEmpty()) {
            storage[0] = last
            var i = 0
            while (2 * i + 1 < storage.lastIndex) {
                val j = if (2 * i + 2 < storage.lastIndex) {
                    if (comparator.compare(
                            storage[2 * i + 1].element,
                            storage[2 * i + 2].element
                        ) < 0
                    ) 2 * i + 1 else 2 * i + 2
                } else 2 * i + 1
                val a = storage[i]
                val b = storage[j]
                if (comparator.compare(a.element, b.element) <= 0) break
                storage[i] = b; storage[j] = a
                a.index = j; b.index = i
                i = j
            }
        }
        return if (first.removed) pop() else first.element
    }
}