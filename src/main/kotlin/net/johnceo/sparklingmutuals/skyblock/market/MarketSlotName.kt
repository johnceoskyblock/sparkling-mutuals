package net.johnceo.sparklingmutuals.skyblock.market

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.util.StringDecomposer
import java.util.Optional

internal object MarketSlotName {
    fun capture(component: Component): String {
        val plain=component.string
        val visible=StringBuilder()
        var count=0
        var bold=true
        component.visit(FormattedText.StyledContentConsumer<Boolean> {style,text ->
            StringDecomposer.iterateFormatted(text,style) {_,effective,codePoint ->
                if(count<4 && !effective.isBold)bold=false
                count++
                visible.appendCodePoint(codePoint)
                true
            }
            Optional.empty()
        },Style.EMPTY)
        if(!visible.startsWith("SELL "))return plain
        if(!bold)return visible.toString()
        // Retain ordinary legacy labels; mixed/structured labels need one canonical marker.
        return if(Regex("^(?:§[0-9a-fk-or])*§lSELL .+$").matches(plain))plain else "§l$visible"
    }
}
