package ru.landilf.dactune.presentation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlin.math.max

/** Fits the chart into the actual viewport, with scrolling as an accessibility fallback. */
class ResponsiveContentLayout(context: Context) : LinearLayout(context) {
    val settingsPanel = panel()
    val playbackPanel = panel()
    private var viewportHeight = 0
    private var graph: View? = null
    private val sections = mutableListOf<View>()

    init {
        orientation = VERTICAL
        addView(settingsPanel)
        addView(playbackPanel)
    }

    fun setViewportHeight(height: Int) {
        if (viewportHeight == height) return
        viewportHeight = height
        // The parent is already measuring. Re-measure this child without a late layout request.
        forceLayout()
    }

    fun configureGraph(view: View) {
        graph = view
    }

    fun registerSection(view: View) {
        sections.add(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val twoColumns = width >= dp(600) && viewportHeight > 0 && width > viewportHeight
        val compact = viewportHeight in 1 until dp(720)
        orientation = if (twoColumns) HORIZONTAL else VERTICAL
        val horizontal = dp(if (compact) 12 else 20)
        val vertical = dp(if (compact) 4 else 8)
        setPadding(horizontal, vertical, horizontal, vertical)
        configurePanel(settingsPanel, twoColumns, if (twoColumns) dp(12) else 0)
        configurePanel(playbackPanel, twoColumns, 0)
        val gap = dp(if (compact) 2 else 8)
        sections.forEach { it.setPadding(it.paddingLeft, gap, it.paddingRight, gap) }

        val chart = graph
        val minimumGraphHeight = max(dp(96),
            (chart as? SignalLevelGraphView)?.minimumReadableHeightPx ?: dp(96))
        val preferredGraphHeight = max(dp(180), minimumGraphHeight)
        chart?.let { setGraphHeight(it, preferredGraphHeight) }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (chart != null && chart.visibility != GONE &&
            chart.parent is View && (chart.parent as View).visibility != GONE &&
            viewportHeight > 0
        ) {
            val requiredHeight = if (twoColumns) {
                playbackPanel.measuredHeight
            } else {
                settingsPanel.measuredHeight + playbackPanel.measuredHeight
            }
            val overflow = requiredHeight + paddingTop + paddingBottom - viewportHeight
            if (overflow > 0) {
                val height = (preferredGraphHeight - overflow).coerceAtLeast(minimumGraphHeight)
                setGraphHeight(chart, height)
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    private fun setGraphHeight(chart: View, height: Int) {
        if (chart.layoutParams.height == height) return
        chart.layoutParams.height = height
        // Invalidate measurement caches up to this container without scheduling a layout loop.
        var child: View? = chart
        while (child != null && child !== this) {
            child.forceLayout()
            child = child.parent as? View
        }
    }

    private fun configurePanel(panel: LinearLayout, twoColumns: Boolean, endMargin: Int) {
        val params = panel.layoutParams as LayoutParams
        val desiredWidth = if (twoColumns) 0 else ViewGroup.LayoutParams.MATCH_PARENT
        val desiredWeight = if (twoColumns) 1f else 0f
        if (params.width != desiredWidth || params.weight != desiredWeight ||
            params.marginEnd != endMargin
        ) {
            params.width = desiredWidth
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.weight = desiredWeight
            params.marginEnd = endMargin
            panel.layoutParams = params
        }
    }

    private fun panel() = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
