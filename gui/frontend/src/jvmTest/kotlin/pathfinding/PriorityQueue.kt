package pathfinding

class PriorityQueue<E>(private val comparator: Comparator<in E>) {
    companion object {
        fun <E : Comparable<E>> fromComparable() = PriorityQueue<E> { a, b ->
            a.compareTo(b)
        }
    }

    private val storage: MutableList<E> = mutableListOf()

    fun insert(element: E): Boolean {
        storage.add(element)
        var i = storage.lastIndex
        while (i > 0) {
            val j = (i - 1) / 2
            val a = storage[j]
            val b = storage[i]
            if (comparator.compare(a, b) <= 0) break
            storage[i] = a
            storage[j] = b
            i = j
        }
        return true
    }

    operator fun plusAssign(element: E) {
        insert(element)
    }

    fun remove(element: E): Boolean {
        val index = storage.binarySearch(element, comparator)
        if (index < 0) return false
        if (storage[index] === element) {
            storage.removeAt(index)
            return true
        }
        var i = index - 1
        while (i >= 0 && storage[index].let {
                if (it === element) return true
                else comparator.compare(element, it) == 0
            }) i--
        i = index + 1
        while (i <= storage.lastIndex && storage[index].let {
                if (it === element) return true
                else comparator.compare(element, it) == 0
            }) i++
        return false
    }

    fun top(): E? = storage.firstOrNull()

    fun clear() {
        storage.clear()
    }

    @Throws(NoSuchElementException::class)
    fun pop(): E {
        val first = storage.first()
        val last = storage.removeLast()
        if (storage.isNotEmpty()) {
            storage[0] = last
            var i = 0
            while (2 * i + 1 < storage.lastIndex) {
                val j = if (2 * i + 2 < storage.lastIndex) {
                    if (comparator.compare(storage[2 * i + 1], storage[2 * i + 2]) < 0) 2 * i + 1 else 2 * i + 2
                } else 2 * i + 1
                if (comparator.compare(storage[i], storage[j]) <= 0) break
                storage[i] = storage[j].also { storage[j] = storage[i] }
                i = j
            }
        }
        return first
    }
}