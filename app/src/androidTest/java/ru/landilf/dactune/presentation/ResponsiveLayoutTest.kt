package ru.landilf.dactune.presentation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutTest {
    @Test fun firstMeasureExcludesAppBarAndSystemPadding() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fixture = Fixture()
            fixture.measure(800)
            fixture.assertFitted()
            assertTrue(fixture.graph.measuredHeight < fixture.dp(180))
        }
    }

    @Test fun resizingAndChangingAppBarHeightRecalculateTheViewport() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fixture = Fixture()
            fixture.measure(900)
            assertEquals(fixture.dp(180), fixture.graph.measuredHeight)
            fixture.measure(800)
            fixture.assertFitted()
            val previous = fixture.graph.measuredHeight
            fixture.appBar.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, fixture.dp(64)
            )
            fixture.measure(800)
            fixture.assertFitted()
            assertTrue(fixture.graph.measuredHeight < previous)
        }
    }

    private class Fixture {
        private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        private val content = ResponsiveContentLayout(context)
        val graph = View(context)
        val appBar = View(context)
        private val scroll = ResponsiveScrollView(context, content)
        private val parent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(24))
            addView(appBar, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)
            ))
            addView(scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            ))
        }

        init {
            content.settingsPanel.addView(View(context), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(580)
            ))
            content.playbackPanel.addView(graph, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(180)
            ))
            content.configureGraph(graph)
        }

        fun measure(heightDp: Int) {
            parent.measure(
                View.MeasureSpec.makeMeasureSpec(dp(360), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(heightDp), View.MeasureSpec.EXACTLY)
            )
            parent.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
        }

        fun assertFitted() {
            val viewport = parent.measuredHeight - parent.paddingTop - parent.paddingBottom -
                appBar.measuredHeight
            assertEquals(viewport, scroll.measuredHeight)
            val occupied = content.settingsPanel.measuredHeight + content.playbackPanel.measuredHeight +
                content.paddingTop + content.paddingBottom
            assertEquals(viewport, occupied)
            assertEquals(viewport, content.measuredHeight)
        }

        fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    }
}
