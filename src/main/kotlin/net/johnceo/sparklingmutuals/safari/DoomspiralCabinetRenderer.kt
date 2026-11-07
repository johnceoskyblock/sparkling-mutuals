package net.johnceo.sparklingmutuals.safari

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Stateless native counterpart of the approved cabinet preview; never handles input or packets. */
internal object DoomspiralCabinetRenderer {
    private val icon = Identifier.fromNamespaceAndPath("sparkling-mutuals", "textures/gui/doomspiral_lottery.png")
    private const val CYAN = 0x67D9DB
    private const val GOLD = 0xFFD87A

    fun render(g: GuiGraphicsExtractor, width: Int, height: Int, frame: DoomspiralCabinet.Frame) {
        val scale = DoomspiralCabinet.scale(width, height)
        if (scale <= 0f) return
        g.fill(0, 0, width, height, 0xFF09141B.toInt())
        g.pose().pushMatrix()
        try {
            // Shift slightly left to balance the complete cabinet with its projecting lever.
            g.pose().translate(width / 2f - 10 * scale, height / 2f)
            g.pose().scale(scale, scale)
            val accent = if (frame.jackpot) GOLD else CYAN
            val border = if (frame.jackpot) 0xD9B864 else 0x39616B
            panel(g, -108, -119, 108, 119, 0x07151B, border)
            panel(g, -104, -115, 104, 115, 0x11272E, 0x25434D)
            g.outline(-101, -112, 202, 224, color(border, 120))
            panel(g, -96, -105, 96, -66, if (frame.jackpot) 0x27332B else 0x0B1E26, border)
            frame.label?.let { label ->
                val font = Minecraft.getInstance().font
                g.text(font, label, -font.width(label) / 2, -89, frame.labelColor, true)
            }
            lights(g, frame, accent)
            panel(g, -96, -50, 96, 45, if (frame.jackpot) 0x84713E else 0x25434D, border)
            panel(g, -91, -45, 91, 40, 0x07151B, 0x07151B)
            repeat(3) { index -> reel(g, (index - 1) * 62, frame.offset(index), frame, index, border) }
            g.fill(-95, -4, 95, -3, color(accent, 45))
            // Centre-line arrows.
            repeat(4) { n ->
                g.fill(-101 + n, -7 + n, -100 + n, 1 - n, color(accent))
                g.fill(100 - n, -7 + n, 101 - n, 1 - n, color(accent))
            }
            panel(g, -96, 54, 96, 84, 0x18343E, border)
            panel(g, -12, 61, 12, 77, 0x254F5B, 0x67A9B1)
            g.fill(-8, 64, 8, 66, color(0x9AC4CA, 150))
            panel(g, -83, 93, 83, 108, 0x07151B, border)
            g.fill(-79, 104, 79, 106, color(0x25434D))
            g.fill(-107, 115, 107, 119, color(border))
            lever(g, frame.elapsed)
            if (frame.jackpot) celebration(g, frame)
        } finally {
            g.pose().popMatrix()
        }
    }

    private fun panel(g: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int, fill: Int, edge: Int) {
        g.fill(left, top, right, bottom, color(fill))
        g.outline(left, top, right - left, bottom - top, color(edge))
        g.outline(left + 2, top + 2, right - left - 4, bottom - top - 4, color(edge, 95))
    }

    private fun lights(g: GuiGraphicsExtractor, frame: DoomspiralCabinet.Frame, accent: Int) {
        repeat(19) { index ->
            val brightness = (.5f + .5f * sin(frame.elapsed * 7f - index * .8f))
            val x = -89 + index * 10
            g.fill(x - 2, -59, x + 3, -54, color(accent, (25 + 35 * brightness).toInt()))
            g.fill(x - 1, -58, x + 2, -55, color(accent, (110 + 145 * brightness).toInt()))
        }
        val pulse = (.5f + .5f * sin(frame.elapsed * 4f))
        g.outline(-98, -52, 196, 99, color(accent, (40 + 65 * pulse).toInt()))
    }

    private fun lever(g: GuiGraphicsExtractor, elapsed: Float) {
        panel(g, 108, 23, 119, 49, 0x193943, 0x52808A)
        // One decorative pull on appearance; no actual screen control is installed.
        val pull = if (elapsed < .5f) sin(elapsed / .5f * PI).toFloat() else 0f
        val top = -31 + (pull * 28).toInt()
        repeat(61) { n ->
            val t = n / 60f
            val x = 113 + (t * 17).toInt()
            val y = 35 + ((top - 35) * t).toInt()
            g.fill(x - 2, y, x + 3, y + 3, color(0x39616B))
            g.fill(x, y, x + 2, y + 3, color(0x9AC4CA))
        }
        g.fill(121, top - 8, 140, top + 10, color(0x942F3A))
        g.fill(123, top - 6, 138, top + 8, color(0xFF525D))
        g.fill(124, top - 5, 137, top - 3, color(0xFF9199))
        g.fill(124, top - 3, 126, top + 6, color(0xFF9199))
    }

    // Keep the existing 56-unit reel pitch and artwork. The extra window height reveals neighbours.
    private fun reel(g: GuiGraphicsExtractor, x: Int, offset: Float, frame: DoomspiralCabinet.Frame, index: Int, border: Int) {
        g.fill(x - 28, -43, x + 28, 39, color(0x07151B))
        g.enableScissor(x - 27, -42, x + 27, 38)
        try {
            val base = floor(offset).toInt()
            for (row in -1..2) {
                val y = -4 - ((offset - base - row) * 56).toInt()
                if ((base + row) % 6 == 0) {
                    val phase = frame.elapsed * 3f + index
                    val bob = (sin(phase) * 2f).toInt()
                    g.blit(RenderPipelines.GUI_TEXTURED, icon, x - 19, y - 25 + bob,
                        0f, 0f, 38, 50, 230, 307, 230, 307, -1)
                    repeat(6) { particle ->
                        val angle = phase + particle * PI / 3
                        val alpha = (155 + 100 * sin(angle * 2)).toInt()
                        spark(g, x + (cos(angle) * 21).toInt(), y + (sin(angle) * 20).toInt(),
                            color(if (particle % 2 == 0) 0xFFC960 else 0xFFE6A4, alpha))
                    }
                } else {
                    val font = Minecraft.getInstance().font
                    g.pose().pushMatrix()
                    g.pose().translate(x.toFloat(), y.toFloat())
                    g.pose().scale(2.8f, 2.8f)
                    g.text(font, "✕", -font.width("✕") / 2, -4, 0xFFFF525D.toInt(), true)
                    g.pose().popMatrix()
                }
            }
            repeat(6) { band ->
                val alpha = 150 - band * 25
                g.fill(x - 27, -42 + band * 2, x + 27, -40 + band * 2, color(0x07151B, alpha))
                g.fill(x - 27, 36 - band * 2, x + 27, 38 - band * 2, color(0x07151B, alpha))
            }
        } finally {
            g.disableScissor()
        }
        g.outline(x - 28, -43, 56, 82, color(border))
    }

    private fun celebration(g: GuiGraphicsExtractor, frame: DoomspiralCabinet.Frame) {
        // Bounded, deterministic particles: no retained emitters or random work each frame.
        val progress = (frame.revealAge / 5f).coerceIn(0f, 1f)
        repeat(3) { wave ->
            val radius = 121 + ((progress + wave / 3f) % 1f * 12).toInt()
            g.outline(-radius, -radius, radius * 2, radius * 2, color(GOLD, 35 + wave * 15))
        }
        repeat(48) { index ->
            val angle = index * PI * 2 / 48 + progress * .3
            val radius = 42 + progress * 88 + (index % 4) * 5
            val x = (cos(angle) * radius).toInt()
            val y = (sin(angle) * radius * .93).toInt()
            val alpha = (240 - progress * 100).toInt()
            spark(g, x, y, color(if (index % 3 == 0) 0xFFF1BC else GOLD, alpha))
        }
        // Gold confetti across the reels, changing with server progress rather than client wall time.
        repeat(24) { index ->
            val x = ((index * 53 % 187) - 93)
            val y = (((index * 29 + (progress * 160).toInt()) % 180) - 85)
            g.fill(x, y, x + 2, y + 4, color(if (index % 2 == 0) GOLD else 0xFFF1BC, 200))
        }
    }

    private fun spark(g: GuiGraphicsExtractor, x: Int, y: Int, color: Int) {
        g.fill(x - 2, y, x + 3, y + 1, color)
        g.fill(x, y - 2, x + 1, y + 3, color)
    }

    private fun color(rgb: Int, alpha: Int = 255) = (alpha.coerceIn(0, 255) shl 24) or rgb
}
