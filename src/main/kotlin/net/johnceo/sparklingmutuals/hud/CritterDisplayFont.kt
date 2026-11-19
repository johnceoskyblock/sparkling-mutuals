package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence

/** A mod-owned font: canonical text stays in the models; glyphs and measurements use aliases. */
class CritterDisplayFont(provider: Provider): Font(provider) {
    private var depth=0
    private inline fun <T,R> visual(value: T,convert: (T)->T,draw: (T)->R): R {
        val shown=if(depth==0)convert(value) else value
        depth++
        try {return draw(shown)} finally {depth--}
    }
    override fun width(text: String): Int = visual(text,CritterNames::display) {super.width(it)}
    override fun width(text: FormattedText): Int = visual(text,CritterNames::formatted) {super.width(it)}
    override fun width(text: FormattedCharSequence): Int = visual(text,CritterNames::sequence) {super.width(it)}
    override fun split(text: FormattedText,width: Int): List<FormattedCharSequence> =
        visual(text,CritterNames::formatted) {super.split(it,width).map(CritterNames::mark)}
    override fun prepareText(text: String,x: Float,y: Float,color: Int,shadow: Boolean,background: Int): PreparedText =
        visual(text,CritterNames::display) {super.prepareText(it,x,y,color,shadow,background)}
    override fun prepareText(text: FormattedCharSequence,x: Float,y: Float,color: Int,shadow: Boolean,bidi: Boolean,background: Int): PreparedText =
        visual(text,CritterNames::sequence) {super.prepareText(it,x,y,color,shadow,bidi,background)}
}
