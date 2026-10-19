package net.johnceo.sparklingmutuals.safari.automation

enum class GemzieGem(val itemId:String,val label:String,val x:Int,val y:Int,val z:Int) {
    ORANGE("ORANGE_GEM","Orange Gem",-140,61,46),
    LIME("LIME_GEM","Lime Gem",-137,61,51),
    PURPLE("PURPLE_GEM","Purple Gem",-140,61,56)
}
data class GemzieStack(val itemId:String?,val name:String,val count:Int=1)
data class GemzieClick(val x:Int,val y:Int,val z:Int,val enabled:Boolean=true,val safari:Boolean=true,
                       val cavern:Boolean=true,val top:Boolean=true)

object GemzieSelection {
    private val formatting=Regex("§[0-9a-fk-or]",RegexOption.IGNORE_CASE)
    fun target(click:GemzieClick):GemzieGem? {
        if(!click.enabled || !click.safari || !click.cavern || !click.top) return null
        return GemzieGem.entries.firstOrNull { it.x==click.x && it.y==click.y && it.z==click.z }
    }
    fun matches(stack:GemzieStack,gem:GemzieGem):Boolean {
        if(stack.count<=0) return false
        // A present conflicting identity must never become a whole gem through a renamed label.
        return if(stack.itemId!=null) stack.itemId==gem.itemId
            else stack.name.replace(formatting,"").trim()==gem.label
    }
    fun slot(gem:GemzieGem,selected:Int,hotbar:List<GemzieStack>):Int? {
        if(selected in 0..8 && hotbar.getOrNull(selected)?.let { matches(it,gem) }==true) return selected
        return (0..8).firstOrNull { slot -> hotbar.getOrNull(slot)?.let { matches(it,gem) }==true }
    }
}
class GemzieUseAction {
    private var using=false
    fun handle(click:GemzieClick,selected:Int,hotbar:List<GemzieStack>,fresh:Boolean,
               select:(Int)->Unit,use:()->Unit,missing:(GemzieGem)->Unit):Boolean {
        val gem=GemzieSelection.target(click) ?: return false
        if(!fresh || using) return true
        val slot=GemzieSelection.slot(gem,selected,hotbar)
        if(slot==null) { missing(gem);return true }
        using=true
        try {
            if(slot!=selected) select(slot)
            use()
        } finally { using=false }
        return true
    }
}
