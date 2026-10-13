package ru.landilf.dactune.presentation

import ru.landilf.dactune.domain.model.KeepAliveSignal

sealed interface KeepAliveAction {
    data class SelectSignal(val signal: KeepAliveSignal) : KeepAliveAction
    data class SetLevel(val progress: Int) : KeepAliveAction
    data class StepLevel(val steps: Int) : KeepAliveAction
    data class SetTimer(val minutes: Int) : KeepAliveAction
    data class SetAdaptive(val enabled: Boolean) : KeepAliveAction
    data object TogglePlayback : KeepAliveAction
}
