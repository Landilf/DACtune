package ru.landilf.dactune.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.landilf.dactune.domain.model.KeepAliveSettings

interface SettingsRepository {
    val settings: StateFlow<KeepAliveSettings>
    fun save(settings: KeepAliveSettings)
}
