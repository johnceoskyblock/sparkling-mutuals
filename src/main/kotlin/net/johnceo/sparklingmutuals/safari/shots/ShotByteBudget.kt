package net.johnceo.sparklingmutuals.safari.shots

class ShotByteBudget(val maxBytes: Int = 4*1024*1024) : ShotBufferBudget {
    private var used = 0
    override val usedBytes get() = synchronized(this) { used }
    override fun tryReserve(bytes: Int): Boolean = synchronized(this) {
        require(bytes >= 0)
        if (bytes > maxBytes-used) false else { used+=bytes;true }
    }
    override fun release(bytes: Int) = synchronized(this) { require(bytes in 0..used);used-=bytes }
}
