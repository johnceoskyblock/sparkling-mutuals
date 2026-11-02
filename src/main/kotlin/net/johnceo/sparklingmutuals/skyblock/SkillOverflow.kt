/* Adapted from SkyHanni SkillUtil and SkillTooltip, LGPL-2.1. See licenses/SkyHanni-Skills-NOTICE.txt. */
package net.johnceo.sparklingmutuals.skyblock

import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

internal data class OverflowLevel(val level: Int,val current: Long,val required: Long,val overflow: Long) {
    val fraction get()=current.toDouble()/required.coerceAtLeast(1)
}

/** Shared normal-skill curve through 60, then SkyHanni's extrapolated overflow curve. */
internal object SkillOverflowMath {
    private val metadata by lazy {
        SkillOverflowMath::class.java.getResourceAsStream("/assets/sparkling-mutuals/valuation/skill-levels.json")!!
            .bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
    }
    private val costs by lazy { metadata.getAsJsonObject("COMBAT").getAsJsonArray("costs").map { it.asLong } }
    fun cap(skill: String): Int?=metadata.getAsJsonObject(skill.uppercase(Locale.ROOT))?.get("maxLevel")?.asInt
    fun totalFor(level: Int)=costs.take(level.coerceIn(0,60)).sum()
    fun calculate(total: Long,cap: Int): OverflowLevel {
        var current=total.coerceAtLeast(0);var level=0
        while(level<60 && current>=costs[level]) { current-=costs[level];level++ }
        var required=if(level<60) costs[level] else 7_600_000L
        var slope=600_000L
        while(level>=60 && current>=required) {
            current-=required;level++
            // Saturation protects against corrupt lore or an unreachable virtual level.
            required=if(required>Long.MAX_VALUE-slope) Long.MAX_VALUE else required+slope
            if(level%10==0) slope=if(slope>Long.MAX_VALUE/2) Long.MAX_VALUE else slope*2
        }
        return OverflowLevel(level,current,required,(total.coerceAtLeast(0)-totalFor(cap)).coerceAtLeast(0))
    }
    fun number(raw: String): Long?=runCatching {
        val match=Regex("([0-9][0-9,]*(?:\\.[0-9]+)?)([kmbt]?)",RegexOption.IGNORE_CASE).matchEntire(raw.trim()) ?: return null
        val multiplier=when(match.groupValues[2].lowercase(Locale.ROOT)) { "k" -> 3;"m" -> 6;"b" -> 9;"t" -> 12;else -> 0 }
        BigDecimal(match.groupValues[1].replace(",","")).movePointRight(multiplier).setScale(0,RoundingMode.DOWN).longValueExact()
    }.getOrNull()
    fun roman(level: Int): String {
        if(level>3999) return level.toString()
        var remaining=level
        return buildString {
            for((value,label) in listOf(1000 to "M",900 to "CM",500 to "D",400 to "CD",100 to "C",90 to "XC",50 to "L",40 to "XL",10 to "X",9 to "IX",5 to "V",4 to "IV",1 to "I"))
                while(remaining>=value) { append(label);remaining-=value }
        }
    }
    fun menuLevel(raw: String): Int? {
        raw.toIntOrNull()?.let { return it.takeIf { value -> value in 1..60 } }
        val digits=mapOf('I' to 1,'V' to 5,'X' to 10,'L' to 50,'C' to 100,'D' to 500,'M' to 1000)
        val values=raw.map { digits[it] ?: return null }
        val level=values.mapIndexed { index,value -> if(value<(values.getOrNull(index+1) ?: 0)) -value else value }.sum()
        return level.takeIf { it in 1..60 && roman(it)==raw }
    }
}

internal data class OverflowTooltipPlan(val skill: String,val progressIndex: Int,val xpIndex: Int,val value: OverflowLevel,val roman: Boolean) {
    fun level(value: Int)=if(roman) SkillOverflowMath.roman(value) else value.toString()
    val title get()="$skill ${level(value.level)}"
}

internal object OverflowTooltipParsing {
    fun plan(name: String,lines: List<String>): OverflowTooltipPlan? {
        if(lines.any { it.contains("OVERFLOW XP",true) }) return null // Another mod already supplied it.
        val match=Regex("([A-Za-z]+) ([IVXLCDM]+|[0-9]+)").matchEntire(SafariRules.strip(name)) ?: return null
        val skill=match.groupValues[1]
        SkillOverflowMath.cap(skill) ?: return null // Exclude Social, Runecrafting and unrelated menus.
        // The maxed menu gives the actual cap, including personal unlocks and newer server caps.
        val cap=SkillOverflowMath.menuLevel(match.groupValues[2]) ?: return null
        val plain=lines.map(SafariRules::strip)
        val max=plain.indexOfFirst { it.trim().equals("Max Skill level reached!",true) }
        if(max<0) return null
        val xpIndex=(max+1..minOf(plain.lastIndex,max+4)).firstOrNull { index ->
            val text=plain[index].trim().removePrefix("Total XP: ").removeSuffix(" XP").trim()
            SkillOverflowMath.number(text)!=null
        } ?: return null
        val total=SkillOverflowMath.number(plain[xpIndex].trim().removePrefix("Total XP: ").removeSuffix(" XP").trim()) ?: return null
        if(total<SkillOverflowMath.totalFor(cap)) return null
        return OverflowTooltipPlan(skill,max,xpIndex,SkillOverflowMath.calculate(total,cap),match.groupValues[2].first().isLetter())
    }
}

/** Menu-local transformation only: never edits server item stacks, skill stats, or inventories. */
object SkillOverflow {
    fun register() {
        ItemTooltipCallback.EVENT.register { stack,_,_,lines ->
            val client=Minecraft.getInstance()
            if(!ConfigManager.skillOverflow || !SkyblockSidebar.inSkyblock(client)) return@register
            val screen=client.smScreen as? AbstractContainerScreen<*> ?: return@register
            if(SafariRules.strip(screen.title.string)!="Your Skills") return@register
            val plan=OverflowTooltipParsing.plan(stack.hoverName.string,lines.map { it.string }) ?: return@register
            if(lines.isEmpty()) return@register
            lines[0]=Component.literal(plan.title).setStyle(lines[0].style)
            lines[plan.progressIndex]=Component.literal("Progress to Level ${plan.level(plan.value.level+1)}: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT,"%.1f%%",plan.value.fraction*100)).withStyle(ChatFormatting.YELLOW))
            val filled=(plan.value.fraction*20).toInt().coerceIn(0,20)
            fun count(value: Long)=String.format(Locale.ROOT,"%,d",value)
            lines[plan.xpIndex]=Component.literal("▏".repeat(filled)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("▏".repeat(20-filled)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" ${count(plan.value.current)}/${count(plan.value.required)}").withStyle(ChatFormatting.YELLOW))
            lines.addAll(plan.xpIndex+1,listOf(Component.empty(),
                Component.literal("OVERFLOW XP: ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(count(plan.value.overflow)).withStyle(ChatFormatting.GRAY))))
        }
    }
}
