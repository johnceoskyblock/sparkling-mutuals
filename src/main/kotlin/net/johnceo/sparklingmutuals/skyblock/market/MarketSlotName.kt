package net.johnceo.sparklingmutuals.skyblock.market

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import java.util.Optional

internal object MarketSlotName {
    fun capture(component: Component): String {
        val plain=component.string
        if(!plain.startsWith("SELL "))return plain
        var count=0
        var bold=true
        component.visit(FormattedText.StyledContentConsumer<Boolean> {style,text ->
            val take=minOf(text.length,4-count)
            if(take>0 && !style.isBold)bold=false
            count+=take
            if(count>=4)Optional.of(true)else Optional.empty()
        },Style.EMPTY)
        return if(bold && count==4)"§l$plain"else plain
    }
}
