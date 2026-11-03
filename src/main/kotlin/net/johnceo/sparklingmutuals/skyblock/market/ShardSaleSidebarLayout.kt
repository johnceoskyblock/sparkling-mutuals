package net.johnceo.sparklingmutuals.skyblock.market

internal data class ShardSidebarBounds(val x: Int,val y: Int,val width: Int,val height: Int,val scale: Float,val rows: Int) {
    fun clampScroll(scroll: Int,count: Int)=scroll.coerceIn(0,(count-rows).coerceAtLeast(0))
    fun contains(mx: Double,my: Double)=mx>=x && mx<x+width && my>=y && my<y+height
    fun rowAt(mx: Double,my: Double): Int? {
        if(!contains(mx,my))return null
        val local=(my-y)/scale-ShardSaleSidebarLayout.HEADER
        if(local<0 || local>=rows*ShardSaleSidebarLayout.ROW)return null
        return (local/ShardSaleSidebarLayout.ROW).toInt()
    }
}
internal object ShardSaleSidebarLayout {
    const val WIDTH=236
    const val HEADER=30
    const val FOOTER=30
    const val ROW=14
    fun bounds(screenWidth: Int,screenHeight: Int,inventoryRight: Int,rowCount: Int): ShardSidebarBounds? {
        if(screenWidth<=0 || screenHeight<=0 || inventoryRight<0 || inventoryRight>screenWidth)return null
        val x=inventoryRight+6
        val available=screenWidth-x-4
        var scale=minOf(1f,available/WIDTH.toFloat())
        if(kotlin.math.ceil(WIDTH*scale.toDouble())>available)scale=Math.nextDown(scale)
        if(scale<.75f)return null
        val capacity=kotlin.math.floor(((screenHeight-8)/scale-HEADER-FOOTER)/ROW).toInt().coerceAtMost(12)
        if(capacity<1)return null
        val rows=minOf(rowCount.coerceAtLeast(1),capacity)
        val height=kotlin.math.ceil((HEADER+FOOTER+rows*ROW)*scale.toDouble()).toInt()
        val width=kotlin.math.ceil(WIDTH*scale.toDouble()).toInt()
        return ShardSidebarBounds(x,(screenHeight-height)/2,width,height,scale,rows)
    }
}
