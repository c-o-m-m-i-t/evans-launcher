package com.evans.launcher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val FRAME_MS = 40L   // raise to 66 if the TV stutters

/** Animated background: drifting glows, faint grid, scanline sweep, floating particles, vignette. */
class TechView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val t0 = System.nanoTime()
    private val dots = List(46) { floatArrayOf(rnd(), rnd(), 0.3f + rnd() * 0.7f) }

    private fun rnd() = Math.random().toFloat()
    private fun wrap(v: Float) = ((v % 1f) + 1f) % 1f

    private fun glow(cv: Canvas, x: Float, y: Float, r: Float, rgb: Int, a: Int) {
        p.shader = RadialGradient(x, y, r, intArrayOf((a shl 24) or rgb, rgb), null, Shader.TileMode.CLAMP)
        cv.drawCircle(x, y, r, p)
        p.shader = null
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val t = (System.nanoTime() - t0) / 1e9f
        cv.drawColor(0xFF05080F.toInt())

        glow(cv, w * (0.14f + 0.05f * sin(t * 0.20f)), h * 0.88f, w * 0.55f, 0x00E5FF, 80)
        glow(cv, w * (0.92f + 0.04f * cos(t * 0.15f)), h * 0.04f, w * 0.50f, 0x7C4DFF, 70)

        p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = 0x0EFFFFFF
        val s = h / 18f
        var x = (t * 5f) % s
        while (x < w) { cv.drawLine(x, 0f, x, h, p); x += s }
        var y = (t * 5f) % s
        while (y < h) { cv.drawLine(0f, y, w, y, p); y += s }

        p.style = Paint.Style.FILL
        for (d in dots) {
            val px = wrap(d[0] + t * 0.004f * d[2]) * w
            val py = wrap(d[1] - t * 0.012f * d[2]) * h
            p.color = ((20 + 60 * d[2]).toInt() shl 24) or 0x00E5FF
            cv.drawCircle(px, py, 1.5f + 2f * d[2], p)
        }

        val sy = wrap(t * 0.07f) * h
        p.shader = LinearGradient(0f, sy - 70f, 0f, sy + 70f,
            intArrayOf(0x0000E5FF, 0x1A00E5FF, 0x0000E5FF), null, Shader.TileMode.CLAMP)
        cv.drawRect(0f, sy - 70f, w, sy + 70f, p)

        p.shader = RadialGradient(w / 2, h / 2, max(w, h) * 0.75f,
            intArrayOf(0x00000000, 0xDD000000.toInt()), floatArrayOf(0.45f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null

        postInvalidateDelayed(FRAME_MS)
    }
}

/** One launcher tile: app icon or glyph, label, index tag, neon focus ring. */
class Tile(
    c: Context,
    private val label: String,
    private val tag: String,
    private val icon: Drawable?,
    private val glyph: String?,
    private val go: () -> Unit
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isFocusable = true
        isClickable = true
        setOnClickListener { go() }
        setOnFocusChangeListener { _, f ->
            animate().scaleX(if (f) 1.1f else 1f).scaleY(if (f) 1.1f else 1f).setDuration(160).start()
            translationZ = if (f) 20f else 0f
            invalidate()
        }
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val f = isFocused
        val r = w * 0.08f
        val rect = RectF(6f, 6f, w - 6f, h - 6f)

        p.style = Paint.Style.FILL; p.clearShadowLayer()
        p.color = if (f) 0xF0101B2D.toInt() else 0x99101826.toInt()
        cv.drawRoundRect(rect, r, r, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = if (f) 4f else 1.5f
        p.color = if (f) 0xFF00E5FF.toInt() else 0x4400E5FF
        if (f) p.setShadowLayer(30f, 0f, 0f, 0xFF00E5FF.toInt())
        cv.drawRoundRect(rect, r, r, p)
        p.clearShadowLayer()

        val s = (h * 0.42f).toInt(); val l = ((w - s) / 2).toInt(); val top = (h * 0.14f).toInt()
        icon?.apply { setBounds(l, top, l + s, top + s); draw(cv) }

        p.style = Paint.Style.FILL; p.typeface = Typeface.MONOSPACE; p.textAlign = Paint.Align.CENTER
        glyph?.let {
            p.textSize = h * 0.26f; p.color = 0xFF00E5FF.toInt()
            cv.drawText(it, w / 2, top + s * 0.8f, p)
        }
        p.textSize = w * 0.095f
        p.color = if (f) 0xFFFFFFFF.toInt() else 0xFFB8C7D9.toInt()
        p.isFakeBoldText = f
        cv.drawText(label, w / 2, h * 0.82f, p)

        p.isFakeBoldText = false; p.textAlign = Paint.Align.LEFT
        p.textSize = w * 0.07f; p.color = 0x9900E5FF.toInt()
        cv.drawText(tag, w * 0.09f, h * 0.10f, p)
    }
}
