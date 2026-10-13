package ru.landilf.dactune.domain.model

data class SignalLevelSample(
    val elapsedMs: Long,
    val levelDbfs: Double
)
