package ru.landilf.dactune.presentation

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import ru.landilf.dactune.R
import ru.landilf.dactune.domain.model.*

/** Views only render state and emit actions; no storage, timers or audio ownership here. */
class KeepAliveScreen(
    private val context: Context,
    private val onAction: (KeepAliveAction) -> Unit
) {
    private var rendering = false
    private var state = KeepAliveUiState()
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(40))
    }
    val view = ScrollView(context).apply { addView(root) }
    private val adaptive = SwitchCompat(context)
    private val signals = Spinner(context)
    private val level = SeekBar(context)
    private val decrease = Button(context)
    private val increase = Button(context)
    private val levelText = TextView(context)
    private val timer = Spinner(context)
    private val countdown = TextView(context)
    private val toggle = Button(context)
    private val status = TextView(context)

    init {
        root.addView(text(R.string.title, 24f).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(text(R.string.description))
        adaptive.text = context.getString(R.string.label_adaptive_mode)
        adaptive.setOnCheckedChangeListener { _, checked ->
            if (!rendering) onAction(KeepAliveAction.SetAdaptive(checked))
        }
        root.addView(adaptive)
        root.addView(text(R.string.adaptive_description, 14f))
        signals.adapter = adapter(KeepAliveSignal.entries.map { context.getString(it.labelRes()) })
        signals.onItemSelectedListener = selection { index ->
            val signal = KeepAliveSignal.entries[index]
            if (signal != state.settings.signal) onAction(KeepAliveAction.SelectSignal(signal))
        }
        addGroup(R.string.label_signal, signals)
        level.max = KeepAliveSettings.LEVEL_STEP_COUNT
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
        addGroup(R.string.label_level, controls)
        levelText.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(levelText)
        timer.adapter = adapter(KeepAliveSettings.TIMER_OPTIONS.map { minutes ->
            if (minutes == 0) context.getString(R.string.timer_off)
            else context.resources.getQuantityString(R.plurals.timer_minutes, minutes, minutes)
        })
        timer.onItemSelectedListener = selection { index ->
            val minutes = KeepAliveSettings.TIMER_OPTIONS[index]
            if (minutes != state.settings.timerMinutes) onAction(KeepAliveAction.SetTimer(minutes))
        }
        addGroup(R.string.label_timer, timer)
        countdown.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(countdown)
        toggle.setOnClickListener { onAction(KeepAliveAction.TogglePlayback) }
        root.addView(toggle)
        root.addView(status)
    }

    fun render(state: KeepAliveUiState) {
        rendering = true
        this.state = state
        adaptive.isChecked = state.settings.adaptiveEnabled
        signals.setSelection(KeepAliveSignal.entries.indexOf(state.settings.signal))
        level.progress = KeepAliveSettings.progressFromLevel(state.settings.levelDbfs)
        levelText.text = context.getString(R.string.level_dbfs, state.settings.levelDbfs)
        timer.setSelection(KeepAliveSettings.TIMER_OPTIONS.indexOf(state.settings.timerMinutes))
        listOf(adaptive, signals, level, decrease, increase, timer).forEach {
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

    private fun configureArrow(button: Button, text: Int, description: Int, step: Int) {
        button.setText(text)
        button.contentDescription = context.getString(description)
        button.minWidth = 0
        button.minimumWidth = 0
        button.setPadding(0, 0, 0, 0)
        button.setOnClickListener { onAction(KeepAliveAction.StepLevel(step)) }
    }

    private fun text(res: Int, size: Float = 16f) = TextView(context).apply {
        setText(res)
        textSize = size
        setPadding(0, 0, 0, dp(16))
    }

    private fun addGroup(label: Int, control: View) {
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(label))
            addView(control)
            setPadding(0, dp(8), 0, dp(12))
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
