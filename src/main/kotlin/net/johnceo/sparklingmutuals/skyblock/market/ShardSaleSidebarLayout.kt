package net.johnceo.sparklingmutuals.skyblock.market

internal data class ShardSidebarBounds(val x: Int,val y: Int,val width: Int,val height: Int,val scale: Float,val rows: Int,
    val header: Int=ShardSaleSidebarLayout.HEADER) {
    fun clampScroll(scroll: Int,count: Int)=scroll.coerceIn(0,(count-rows).coerceAtLeast(0))
    fun contains(mx: Double,my: Double)=mx>=x && mx<x+width && my>=y && my<y+height
    fun rowAt(mx: Double,my: Double): Int? {
        if(!contains(mx,my))return null
        val local=(my-y)/scale-header
        if(local<0 || local>=rows*ShardSaleSidebarLayout.ROW)return null
        return (local/ShardSaleSidebarLayout.ROW).toInt()
    }
}
internal object ShardSaleSidebarLayout {
    fun leftBounds(screenWidth: Int,screenHeight: Int,inventoryLeft: Int,rowCount: Int): ShardSidebarBounds? {
        if(inventoryLeft !in 0..screenWidth)return null
        return bounds(screenWidth,screenHeight,screenWidth-inventoryLeft+24,rowCount)?.let {
            it.copy(x=screenWidth-it.x-it.width)
        }
    }
    const val WIDTH=236
    const val HEADER=17
    const val FOOTER=6
    const val ROW=11
    const val BOX_HEADER=31
    fun boxBounds(screenWidth: Int,screenHeight: Int,inventoryRight: Int,rowCount: Int): ShardSidebarBounds?=
        boundsWithHeader(screenWidth,screenHeight,inventoryRight,rowCount,BOX_HEADER)
    fun bounds(screenWidth: Int,screenHeight: Int,inventoryRight: Int,rowCount: Int): ShardSidebarBounds? {
        return boundsWithHeader(screenWidth,screenHeight,inventoryRight,rowCount,HEADER)
    }
    private fun boundsWithHeader(screenWidth: Int,screenHeight: Int,inventoryRight: Int,rowCount: Int,header: Int): ShardSidebarBounds? {
        if(screenWidth<=0 || screenHeight<=0 || inventoryRight<0 || inventoryRight>screenWidth)return null
        val x=inventoryRight+6
        val available=screenWidth-x-4
        var scale=minOf(1f,available/WIDTH.toFloat())
        if(kotlin.math.ceil(WIDTH*scale.toDouble())>available)scale=Math.nextDown(scale)
        if(scale<.75f)return null
        val capacity=kotlin.math.floor(((screenHeight-8)/scale-header-FOOTER)/ROW).toInt().coerceAtMost(12)
        if(capacity<1)return null
        val rows=minOf(rowCount.coerceAtLeast(1),capacity)
        val height=kotlin.math.ceil((header+FOOTER+rows*ROW)*scale.toDouble()).toInt()
        val width=kotlin.math.ceil(WIDTH*scale.toDouble()).toInt()
        return ShardSidebarBounds(x,(screenHeight-height)/2,width,height,scale,rows,header)
    }
}
