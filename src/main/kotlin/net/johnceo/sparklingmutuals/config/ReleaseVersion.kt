package net.johnceo.sparklingmutuals.config

internal object ReleaseVersion {
    fun compareVersions(a: String, b: String): Int? {
        val pattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$")
        fun parts(value: String) = pattern.matchEntire(value.trim())?.groupValues?.drop(1)
        fun numeric(x: String, y: String): Int {
            val left = x.trimStart('0').ifEmpty { "0" }
            val right = y.trimStart('0').ifEmpty { "0" }
            return left.length.compareTo(right.length).takeIf { it != 0 } ?: left.compareTo(right)
        }
        val x = parts(a) ?: return null
        val y = parts(b) ?: return null
        for (i in 0..2) numeric(x[i], y[i]).takeIf { it != 0 }?.let { return it }
        if (x[3].isEmpty() || y[3].isEmpty()) {
            return when {
                x[3] == y[3] -> 0
                x[3].isEmpty() -> 1
                else -> -1
            }
        }
        val left = x[3].split('.')
        val right = y[3].split('.')
        for (i in 0 until minOf(left.size, right.size)) {
            val leftNumeric = left[i].all(Char::isDigit)
            val rightNumeric = right[i].all(Char::isDigit)
            val comparison = when {
                leftNumeric && rightNumeric -> numeric(left[i], right[i])
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left[i].compareTo(right[i])
            }
            if (comparison != 0) return comparison
        }
        return left.size.compareTo(right.size)
    }
}
