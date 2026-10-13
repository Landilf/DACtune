package ru.landilf.dactune.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.withClip
import ru.landilf.dactune.R
import ru.landilf.dactune.domain.engine.SignalLevelHistory
import ru.landilf.dactune.domain.model.SignalLevelSample

/** Displays the digital keep-alive envelope. History is owned by the session, not this View. */
class SignalLevelGraphView(context: Context) : View(context) {
    val minimumReadableHeightPx: Int
        get() = (labelPaint.fontSpacing * 2 + dp(76f)).toInt()

    private var samples: List<SignalLevelSample> = emptyList()
    private var upperDbfs = -40.0
    private var lowerDbfs = -80.0
    private val path = Path()
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(android.R.attr.textColorSecondary, Color.GRAY)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 11f, resources.displayMetrics
        )
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelPaint.color
        alpha = 50
        strokeWidth = dp(1f)
    }
    private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelPaint.color
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
    }
    private val signalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(androidx.appcompat.R.attr.colorAccent, Color.BLUE)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    init {
        contentDescription = context.getString(R.string.signal_graph_description)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun render(history: List<SignalLevelSample>, upper: Double, lower: Double) {
        if (history == samples && upper == upperDbfs && lower == lowerDbfs) return
        samples = history
        upperDbfs = upper
        lowerDbfs = lower
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = labelPaint.measureText(context.getString(R.string.level_dbfs, -100.0)) + dp(8f)
        val right = width - dp(6f)
        val top = dp(8f)
        val bottom = height - labelPaint.fontSpacing * 2 - dp(12f)
        if (right <= left || bottom <= top) return

        val minDbfs = lowerDbfs - 5.0
        val maxDbfs = upperDbfs + 5.0
        fun y(level: Double): Float =
            bottom - ((level.coerceIn(minDbfs, maxDbfs) - minDbfs) /
                (maxDbfs - minDbfs)).toFloat() * (bottom - top)

        val ticks = if (upperDbfs == lowerDbfs) listOf(upperDbfs)
            else listOf(upperDbfs, (upperDbfs + lowerDbfs) / 2, lowerDbfs).distinct()
        for (tick in ticks) {
            val tickY = y(tick)
            canvas.drawText(context.getString(R.string.level_dbfs, tick), 0f,
                tickY - (labelPaint.ascent() + labelPaint.descent()) / 2, labelPaint)
            canvas.drawLine(left, tickY, right, tickY, gridPaint)
        }
        for (index in 0..2) {
            val x = left + (right - left) * index / 2
            canvas.drawLine(x, top, x, bottom, gridPaint)
            val label = if (index == 2) context.getString(R.string.signal_graph_now)
                else context.getString(R.string.signal_graph_seconds_ago, 30 - index * 15)
            val labelX = (x - labelPaint.measureText(label) / 2)
                .coerceIn(0f, (width - labelPaint.measureText(label)).coerceAtLeast(0f))
            canvas.drawText(label, labelX, bottom + labelPaint.fontSpacing, labelPaint)
        }

        canvas.drawLine(left, y(upperDbfs), right, y(upperDbfs), boundaryPaint)
        canvas.drawLine(left, y(lowerDbfs), right, y(lowerDbfs), boundaryPaint)

        if (samples.isEmpty()) {
            val label = context.getString(R.string.signal_graph_empty)
            canvas.drawText(label, left + (right - left - labelPaint.measureText(label)) / 2,
                (top + bottom) / 2, labelPaint)
        } else {
            val endMs = samples.last().elapsedMs
            val startMs = endMs - SignalLevelHistory.WINDOW_MS
            canvas.withClip(left, top, right, bottom) {
                path.reset()
                samples.forEachIndexed { index, sample ->
                    val x = left + ((sample.elapsedMs - startMs).toDouble() /
                        SignalLevelHistory.WINDOW_MS).toFloat() * (right - left)
                    if (index == 0) path.moveTo(x, y(sample.levelDbfs))
                    else path.lineTo(x, y(sample.levelDbfs))
                }
                drawPath(path, signalPaint)
                drawCircle(right, y(samples.last().levelDbfs), dp(2f), signalPaint)
            }
        }
        val legend = context.getString(R.string.signal_graph_legend)
        canvas.drawText(legend, 0f, height - dp(3f), labelPaint)
    }

    private fun themeColor(attribute: Int, fallback: Int): Int {
        val attributes = context.obtainStyledAttributes(intArrayOf(attribute))
        return try {
            attributes.getColor(0, fallback)
        } finally {
            attributes.recycle()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
