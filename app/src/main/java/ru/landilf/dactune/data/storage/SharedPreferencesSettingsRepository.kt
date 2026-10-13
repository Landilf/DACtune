package ru.landilf.dactune.data.storage

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.landilf.dactune.domain.model.KeepAliveSettings
import ru.landilf.dactune.domain.model.KeepAliveSignal
import ru.landilf.dactune.domain.repository.SettingsRepository

/** Retains the original file and keys so existing installations keep their settings. */
class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences =
        context.getSharedPreferences("keep_alive_settings", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(load())
    override val settings = mutableSettings.asStateFlow()

    override fun save(settings: KeepAliveSettings) {
        val normalized = settings.normalized()
        preferences.edit {
            putString("signal", normalized.signal.name)
            putFloat("level_dbfs", normalized.levelDbfs.toFloat())
            putFloat("lower_level_dbfs", normalized.lowerLevelDbfs.toFloat())
            putInt("timer_minutes", normalized.timerMinutes)
            putBoolean("adaptive_enabled", normalized.adaptiveEnabled)
        }
        mutableSettings.value = normalized
    }

    private fun load(): KeepAliveSettings {
        val defaults = KeepAliveSettings()
        return KeepAliveSettings(
            signal = KeepAliveSignal.entries.firstOrNull {
                it.name == preferences.getString("signal", defaults.signal.name)
            } ?: defaults.signal,
            levelDbfs = preferences.getFloat("level_dbfs", defaults.levelDbfs.toFloat()).toDouble(),
            lowerLevelDbfs = preferences.getFloat(
                "lower_level_dbfs",
                preferences.getFloat("level_dbfs", defaults.levelDbfs.toFloat()) - 40f
            ).toDouble(),
            timerMinutes = preferences.getInt("timer_minutes", defaults.timerMinutes),
            adaptiveEnabled = preferences.getBoolean("adaptive_enabled", defaults.adaptiveEnabled)
        ).normalized()
    }
}
