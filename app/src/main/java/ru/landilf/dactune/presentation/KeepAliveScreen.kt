package ru.landilf.dactune.presentation

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import ru.landilf.dactune.R
import ru.landilf.dactune.domain.model.*

/** Views only render state and emit actions; no storage, timers or audio ownership here. */
class KeepAliveScreen(
    private val context: Context,
    private val onAction: (KeepAliveAction) -> Unit,
    private val onShowInfo: () -> Unit
) {
    private var rendering = false
    private var state = KeepAliveUiState()
    private val root = ResponsiveContentLayout(context)
    val view = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(Toolbar(context).apply {
            title = context.getString(R.string.app_name)
            minimumHeight = dp(56)
            elevation = dp(4).toFloat()
            menu.add(context.getString(R.string.about_app)).apply {
                setIcon(R.drawable.ic_info)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                setOnMenuItemClickListener {
                    onShowInfo()
                    true
                }
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        addView(ResponsiveScrollView(context, root),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }
    private val adaptive = SwitchCompat(context)
    private val signals = Spinner(context).apply { minimumHeight = dp(48) }
    private val level = SeekBar(context)
    private val decrease = Button(context)
    private val increase = Button(context)
    private val levelText = TextView(context).apply { textSize = 16f }
    private val levelLabel = text(R.string.label_level)
    private val lowerLevel = SeekBar(context)
    private val lowerDecrease = Button(context)
    private val lowerIncrease = Button(context)
    private val lowerLevelText = TextView(context).apply {
        textSize = 16f
    }
    private val lowerLevelGroup = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    private val adaptiveLevelText = TextView(context).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        textSize = 16f
        setPadding(0, dp(4), 0, dp(8))
    }
    private val signalGraph = SignalLevelGraphView(context)
    private val signalGraphGroup = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(R.string.signal_graph_title))
        addView(signalGraph, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(180)
        ))
        setPadding(0, dp(4), 0, dp(8))
    }
    private val timer = Spinner(context).apply { minimumHeight = dp(48) }
    private val countdown = TextView(context)
    private val toggle = Button(context)
    private val status = TextView(context)

    init {
        adaptive.text = context.getString(R.string.label_adaptive_mode)
        adaptive.minimumHeight = dp(48)
        adaptive.setOnCheckedChangeListener { _, checked ->
            if (!rendering) onAction(KeepAliveAction.SetAdaptive(checked))
        }
        root.settingsPanel.addView(adaptive)
        signals.adapter = adapter(KeepAliveSignal.entries.map { context.getString(it.labelRes()) })
        signals.onItemSelectedListener = selection { index ->
            val signal = KeepAliveSignal.entries[index]
            if (signal != state.settings.signal) onAction(KeepAliveAction.SelectSignal(signal))
        }
        addGroup(R.string.label_signal, signals)
        level.max = KeepAliveSettings.LEVEL_STEP_COUNT
        level.minimumHeight = dp(48)
        level.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !rendering) onAction(KeepAliveAction.SetLevel(progress))
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        configureArrow(
            decrease, R.string.button_decrease_level,
            R.string.button_decrease_level_description, -1
        )
        configureArrow(
            increase, R.string.button_increase_level,
            R.string.button_increase_level_description, 1
        )
        val controls = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(
                decrease,
                LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            addView(level, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(
                increase,
                LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
        root.settingsPanel.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(levelHeader(levelLabel, levelText))
            addView(controls)
            root.registerSection(this)
        })
        createLowerLevelControls()
        root.settingsPanel.addView(lowerLevelGroup)
        root.registerSection(lowerLevelGroup)
        root.playbackPanel.addView(adaptiveLevelText)
        root.playbackPanel.addView(signalGraphGroup)
        root.configureGraph(signalGraph)
        root.registerSection(signalGraphGroup)
        timer.adapter = adapter(KeepAliveSettings.TIMER_OPTIONS.map { minutes ->
            when (minutes) {
                KeepAliveSettings.UNTIL_APP_CLOSED -> context.getString(R.string.timer_until_closed)
                else -> context.resources.getQuantityString(R.plurals.timer_minutes, minutes, minutes)
            }
        })
        timer.onItemSelectedListener = selection { index ->
            val minutes = KeepAliveSettings.TIMER_OPTIONS[index]
            if (minutes != state.settings.timerMinutes) onAction(KeepAliveAction.SetTimer(minutes))
        }
        addGroup(R.string.label_timer, timer, root.playbackPanel)
        countdown.gravity = Gravity.CENTER_HORIZONTAL
        root.playbackPanel.addView(countdown)
        toggle.setOnClickListener { onAction(KeepAliveAction.TogglePlayback) }
        root.playbackPanel.addView(toggle)
        root.playbackPanel.addView(status)
    }

    fun render(state: KeepAliveUiState) {
        rendering = true
        this.state = state
        adaptive.isChecked = state.settings.adaptiveEnabled
        signals.setSelection(KeepAliveSignal.entries.indexOf(state.settings.signal))
        level.progress = KeepAliveSettings.progressFromLevel(state.settings.levelDbfs)
        levelText.text = context.getString(R.string.level_dbfs, state.settings.levelDbfs)
        levelLabel.setText(
            if (state.settings.adaptiveEnabled) R.string.label_upper_level else R.string.label_level
        )
        lowerLevelGroup.visibility = if (state.settings.adaptiveEnabled) View.VISIBLE else View.GONE
        lowerLevel.max = KeepAliveSettings.lowerProgressFromLevel(state.settings.levelDbfs)
        lowerLevel.progress = KeepAliveSettings.lowerProgressFromLevel(state.settings.lowerLevelDbfs)
        lowerLevelText.text = context.getString(R.string.level_dbfs, state.settings.lowerLevelDbfs)
        val adaptiveRunning = state.playback.phase == PlaybackPhase.RUNNING &&
            state.playback.settings?.adaptiveEnabled == true
        adaptiveLevelText.visibility = if (adaptiveRunning) View.VISIBLE else View.GONE
        adaptiveLevelText.text = state.playback.signalLevelDbfs?.let { currentLevel ->
            context.getString(R.string.adaptive_level_current, currentLevel)
        } ?: context.getString(R.string.adaptive_level_pending)
        signalGraphGroup.visibility = if (state.settings.adaptiveEnabled) View.VISIBLE else View.GONE
        val graphSettings = state.playback.settings ?: state.settings
        signalGraph.render(state.playback.signalHistory,
            graphSettings.levelDbfs, graphSettings.lowerLevelDbfs)
        timer.setSelection(KeepAliveSettings.TIMER_OPTIONS.indexOf(state.settings.timerMinutes))
        listOf(adaptive, signals, level, decrease, increase, timer,
            lowerLevel, lowerDecrease, lowerIncrease).forEach {
            it.isEnabled = state.controlsEnabled
        }
        toggle.isEnabled = !state.requestingPermission
        toggle.setText(
            if (state.playback.active) R.string.button_stop_keep_alive
            else R.string.button_start_keep_alive
        )
        countdown.text = state.playback.remainingSeconds?.let {
            context.getString(R.string.timer_remaining, (it / 60).toInt(), (it % 60).toInt())
        } ?: ""
        countdown.visibility = if (state.playback.remainingSeconds != null) View.VISIBLE else View.GONE
        status.text = statusText(state)
        rendering = false
    }

    private fun statusText(state: KeepAliveUiState): String {
        state.messageRes?.let { return context.getString(it) }
        val playback = state.playback
        playback.error?.let { return context.getString(R.string.status_start_failed, it) }
        if (playback.active) {
            val settings = playback.settings ?: state.settings
            val res = if (settings.adaptiveEnabled) {
                if (playback.phase == PlaybackPhase.STARTING) R.string.status_adaptive_starting
                else R.string.status_adaptive_running
            } else R.string.status_running
            return context.getString(
                res,
                context.getString(settings.signal.labelRes()),
                settings.levelDbfs
            )
        }
        return context.getString(
            when (playback.stopReason) {
                StopReason.TIMER -> R.string.status_timer_finished
                StopReason.CAPTURE_ENDED -> R.string.status_capture_ended
                StopReason.NONE -> R.string.status_ready
                else -> R.string.status_stopped
            }
        )
    }

    private fun createLowerLevelControls() {
        lowerLevel.minimumHeight = dp(48)
        lowerLevel.contentDescription = context.getString(R.string.label_lower_level)
        lowerLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !rendering) onAction(KeepAliveAction.SetLowerLevel(progress))
            }

            override fun onStartTrackingTouch(bar: SeekBar?) = Unit

            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        configureArrow(lowerDecrease, R.string.button_decrease_level,
            R.string.button_decrease_lower_level_description, -1, lower = true)
        configureArrow(lowerIncrease, R.string.button_increase_level,
            R.string.button_increase_lower_level_description, 1, lower = true)
        lowerLevelGroup.addView(levelHeader(text(R.string.label_lower_level), lowerLevelText))
        lowerLevelGroup.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(lowerDecrease,
                LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(lowerLevel,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(lowerIncrease,
                LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    private fun configureArrow(
        button: Button,
        text: Int,
        description: Int,
        step: Int,
        lower: Boolean = false
    ) {
        button.setText(text)
        button.contentDescription = context.getString(description)
        button.minWidth = 0
        button.minimumWidth = 0
        button.setPadding(0, 0, 0, 0)
        button.setOnClickListener {
            onAction(
                if (lower) KeepAliveAction.StepLowerLevel(step) else KeepAliveAction.StepLevel(step)
            )
        }
    }

    private fun text(res: Int, size: Float = 16f) = TextView(context).apply {
        setText(res)
        textSize = size
        setPadding(0, 0, 0, dp(4))
    }

    private fun levelHeader(label: TextView, value: TextView): LinearLayout =
        LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            value.setPadding(dp(8), 0, 0, 0)
            addView(value)
        }

    private fun addGroup(
        label: Int,
        control: View,
        panel: LinearLayout = root.settingsPanel
    ) {
        panel.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(label))
            addView(control)
            root.registerSection(this)
        })
    }

    private fun adapter(items: List<String>) =
        ArrayAdapter(context, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun selection(selected: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!rendering) selected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}

private fun KeepAliveSignal.labelRes(): Int = when (this) {
    KeepAliveSignal.TONE_18_KHZ -> R.string.signal_tone_18khz
    KeepAliveSignal.WHITE_NOISE -> R.string.signal_white_noise
}
