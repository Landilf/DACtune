package ru.landilf.dactune.presentation

import android.content.Context
import android.widget.ScrollView

/** Its height spec already excludes the sibling AppBar and the parent's system-bar padding. */
class ResponsiveScrollView(
    context: Context,
    private val content: ResponsiveContentLayout
) : ScrollView(context) {

    init {
        isFillViewport = true
        addView(content)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableHeight = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            0
        } else {
            (MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom).coerceAtLeast(0)
        }
        // Supply the viewport before ScrollView measures its child with an unbounded height.
        content.setViewportHeight(availableHeight)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
