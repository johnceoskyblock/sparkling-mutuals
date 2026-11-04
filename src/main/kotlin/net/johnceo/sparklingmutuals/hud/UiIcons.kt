package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.client.gui.GuiGraphicsExtractor

/** SafariUtils pixel sprites (MIT). No font-specific Unicode or texture-pack dependency. */
object UiIcons {
    enum class Kind { BIRD,BERRY,WORM,SEED_BAG,INCENSE,GEM,MOUNTAIN,SNOWFLAKE,RAVAGER,SKULL,LEAF,NEST,WALL,MOUND,COIN,EYE,CHECK,CROSS }
    private val sprites=mapOf(
        Kind.BIRD to arrayOf(".....ss....","....s##ss..","....wwwd#s.","...hhw###oo",".wwww####s.","smww####s..","mmmmms##s..",".mmmms#s...","..mm..ss..."),
        Kind.BERRY to arrayOf("....bb....","...#b#....","..#hh###..","..hh#####.",".########s",".########s",".#######ss","..######s.","...####s.."),
        Kind.WORM to arrayOf("..........","........h.",".......#hs",".......mms","......h#s.","......sss.","....#mm...",".m#hsss...","..sss....."),
        Kind.SEED_BAG to arrayOf("..........","...#ggg#..","...shhh##.","...hh####.","..s#######",".s########",".s###dd###",".s##d####s","..s#####s."),
        Kind.INCENSE to arrayOf(".....t....","....tc....","...tc.....","....c.....","..tcdhd...","..d###ds..",".d#####ds.",".d#####ds.","..ddddds.."),
        Kind.GEM to arrayOf("..........","...hhhh...","..hh##hh..",".hh####hs.",".m######s.",".m######s.",".mm####ss.","..mm##ss..","...msss..."),
        Kind.MOUNTAIN to arrayOf("....l......","...loo..l..","..looosloo.",".looogloogs","looogggggss","oogggggssss","ogggggsssss","sssdddsssss",".sssssssss."),
        Kind.SNOWFLAKE to arrayOf(".....g.....","..l..g..l..","...l.g.l...",".l..ggg..l.","..gggdggg..",".l..ggg..l.","...l.g.l...","..l..g..l..",".....g....."),
        Kind.RAVAGER to arrayOf("c.........c","tc.hhhhh.ct",".tchggghct.","..sgggggs..","..sddddds..","..seggges..","..sgmnmgs..","...smnms...","....sss...."),
        Kind.SKULL to arrayOf("..lllll....",".lgggggl...","lgggggggl..","lgpdgdpgl..","lgggggggl..",".lggdggl...","..gdddg....","..s.s.s....","..sssss...."),
        Kind.LEAF to arrayOf(".....l.....","....lgl....","...lgggl...","..lggvggs..","..lggvggs..","..lggvggs..","...lgvgs...","....vs.....","....s......"),
        Kind.NEST to arrayOf("..hhhhhh..",".h######s.","h########s","h##dddd##s","h########s","h##dddd##s",".h######s.","..ssssss.."),
        Kind.WALL to arrayOf("hhhhhhhhhh","##s###s###","ssssssssss","s###s###s#","ssssssssss","##s###s###","ssssssssss","s###s###s#"),
        Kind.MOUND to arrayOf("..........","....hh....","...h##s...","..h####s..",".h##d###s.","h####d###s","ssssssssss"),
        Kind.COIN to arrayOf("...hhhh...","..h####s..",".h##hh##s.",".h###h##s.",".h##hh##s.","..h####s..","...ssss..."),
        Kind.EYE to arrayOf("..........","...hhhh...",".hh####ss.","h###dd###s",".hh####ss.","...ssss..."),
        Kind.CHECK to arrayOf("........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##....."),
        Kind.CROSS to arrayOf("##.....##", ".##...##.", "..##.##..", "...###...", "..##.##..", ".##...##.", "##.....##")
    )
    fun forLabel(label: String): Kind? {
        if (SafariRoster.named(label) != null) return null
        val text=label.lowercase()
        return when {
            text.startsWith("forest") -> Kind.LEAF
            text.startsWith("cavern") -> Kind.MOUNTAIN
            text.startsWith("icy") -> Kind.SNOWFLAKE
            text.startsWith("haunted") -> Kind.SKULL
            "nest" in text -> Kind.NEST
            "wall" in text -> Kind.WALL
            "mound" in text -> Kind.MOUND
            "coin" in text -> Kind.COIN
            "gazer" in text -> Kind.EYE
            "feed in feeder" in text -> Kind.SEED_BAG
            "yogi berry" in text -> Kind.BERRY
            "wriggleworm" in text -> Kind.WORM
            "bag of seeds" in text -> Kind.SEED_BAG
            "bird food" in text -> Kind.SEED_BAG
            "incense" in text -> Kind.INCENSE
            "gem" in text -> Kind.GEM
            "all caught" in text || "all checked" in text -> Kind.CHECK
            else -> null
        }
    }
    private fun shade(c: Int,f: Float)=(c and 0xFF000000.toInt()) or
        ((((c shr 16 and 255)*f).toInt().coerceIn(0,255)) shl 16) or
        ((((c shr 8 and 255)*f).toInt().coerceIn(0,255)) shl 8) or
        (((c and 255)*f).toInt().coerceIn(0,255))
    fun draw(g: GuiGraphicsExtractor,x: Int,y: Int,kind: Kind,color: Int) {
        sprites.getValue(kind).forEachIndexed { row,line ->
            line.forEachIndexed { col,token ->
                val pixel=when(kind) {
                    Kind.MOUNTAIN -> when(token) { 'l'->0xFFFFD09A.toInt();'o'->0xFFEF8A42.toInt();'g'->0xFFA85F3F.toInt();'s'->0xFF5A3C32.toInt();'d'->0xFF2F211E.toInt();else->0 }
                    Kind.RAVAGER -> when (token) {
                        'c' -> 0xFFADC6C4.toInt(); 't' -> 0xFF5F7979.toInt()
                        'h' -> 0xFF8C8780.toInt(); 'g' -> 0xFF6A655E.toInt()
                        's' -> 0xFF403D39.toInt(); 'd' -> 0xFF262523.toInt()
                        'e' -> 0xFFC7E5A8.toInt(); 'm' -> 0xFF514C46.toInt()
                        'n' -> 0xFF2E2D28.toInt(); else -> 0
                    }
                    Kind.SKULL -> when(token) { 'l'->0xFFD6C3DF.toInt();'g'->0xFF8B7895.toInt();'s'->0xFF483752.toInt();'d'->0xFF1C1325.toInt();'p'->0xFF45155F.toInt();else->0 }
                    Kind.SNOWFLAKE -> when(token) { 'l'->0xFFB9F3FF.toInt();'g'->0xFF65CFFF.toInt();'d'->0xFF318CCF.toInt();else->0 }
                    Kind.LEAF -> when(token) { 'l'->0xFFA9F05E.toInt();'g'->0xFF43C83F.toInt();'v'->0xFF279D36.toInt();'s'->0xFF176F2D.toInt();else->0 }
                    else -> when(token) { '#'->color;'s'->shade(color,.48f);'h'->shade(color,1.3f);'m'->shade(color,.72f);'w'->shade(color,1.18f);'d'->shade(color,.28f);'o'->0xFFFFC857.toInt();'b'->0xFF58A346.toInt();'g'->0xFF9B8A56.toInt();'c'->0xFF98BCC5.toInt();'t'->0xFF6B8591.toInt();else->0 }
                }
                if(pixel!=0) g.fill(x+col,y+row,x+col+1,y+row+1,pixel)
            }
        }
    }
}
