package top.kkoishi.stg

class Heap<T> : Iterable<T> {

    override fun iterator(): Iterator<T> {
        TODO("Not yet implemented")
    }

    private fun left(index: Int): Int = 2 * index + 1

    private fun right(index: Int): Int = 2 * index + 2

    private fun parent(index: Int): Int = (index - 1) / 2
}