package ru.landilf.dactune.domain.model

enum class PlaybackPhase { IDLE, STARTING, RUNNING }
enum class StopReason { NONE, USER, TIMER, CAPTURE_ENDED, ERROR }

data class PlaybackState(
    val phase: PlaybackPhase = PlaybackPhase.IDLE,
    val settings: KeepAliveSettings? = null,
    val remainingSeconds: Long? = null,
    val stopReason: StopReason = StopReason.NONE,
    val error: String? = null
) {
    val active: Boolean get() = phase != PlaybackPhase.IDLE
}
