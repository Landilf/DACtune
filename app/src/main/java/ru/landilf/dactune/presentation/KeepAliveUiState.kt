package ru.landilf.dactune.presentation

import ru.landilf.dactune.domain.model.KeepAliveSettings
import ru.landilf.dactune.domain.model.PlaybackState

data class KeepAliveUiState(
    val settings: KeepAliveSettings = KeepAliveSettings(),
    val playback: PlaybackState = PlaybackState(),
    val requestingPermission: Boolean = false,
    val messageRes: Int? = null
) {
    val controlsEnabled get() = !playback.active && !requestingPermission
}
