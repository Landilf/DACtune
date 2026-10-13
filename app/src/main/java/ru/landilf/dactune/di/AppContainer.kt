package ru.landilf.dactune.di

import android.content.Context
import ru.landilf.dactune.data.audio.AndroidPlaybackRepository
import ru.landilf.dactune.data.storage.SharedPreferencesSettingsRepository
import ru.landilf.dactune.domain.repository.PlaybackRepository
import ru.landilf.dactune.domain.repository.SettingsRepository

/** Constructor injection with one composition root for this single-screen application. */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(context)
    val androidPlaybackRepository = AndroidPlaybackRepository(context)
    val playbackRepository: PlaybackRepository = androidPlaybackRepository
}
