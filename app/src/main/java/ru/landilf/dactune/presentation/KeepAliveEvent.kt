package ru.landilf.dactune.presentation

sealed interface KeepAliveEvent {
    data object RequestPlaybackCapture : KeepAliveEvent
}
