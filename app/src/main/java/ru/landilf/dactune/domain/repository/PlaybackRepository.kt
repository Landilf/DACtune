package ru.landilf.dactune.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.landilf.dactune.domain.model.KeepAliveSettings
import ru.landilf.dactune.domain.model.PlaybackState
import ru.landilf.dactune.domain.model.StopReason

interface PlaybackRepository {
    val state: StateFlow<PlaybackState>
    fun start(settings: KeepAliveSettings)
    fun stop(reason: StopReason = StopReason.USER)
}
