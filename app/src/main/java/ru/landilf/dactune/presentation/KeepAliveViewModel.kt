package ru.landilf.dactune.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.landilf.dactune.domain.model.KeepAliveSettings
import ru.landilf.dactune.domain.repository.PlaybackRepository
import ru.landilf.dactune.domain.repository.SettingsRepository
import ru.landilf.dactune.domain.usecase.UpdateKeepAliveSettingsUseCase

class KeepAliveViewModel(
    private val settingsRepository: SettingsRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {
    private val updateSettings = UpdateKeepAliveSettingsUseCase(settingsRepository)
    private val requesting = MutableStateFlow(false)
    private val message = MutableStateFlow<Int?>(null)
    private val mutableEvents = MutableSharedFlow<KeepAliveEvent>()
    val events = mutableEvents.asSharedFlow()
    val uiState = combine(
        settingsRepository.settings,
        playbackRepository.state,
        requesting,
        message
    ) { settings, playback, requestingPermission, messageRes ->
        KeepAliveUiState(settings, playback, requestingPermission, messageRes)
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        KeepAliveUiState(settingsRepository.settings.value, playbackRepository.state.value)
    )

    fun onAction(action: KeepAliveAction) {
        if (action == KeepAliveAction.TogglePlayback) {
            message.value = null
            if (playbackRepository.state.value.active) playbackRepository.stop()
            else if (!requesting.value) {
                if (settingsRepository.settings.value.adaptiveEnabled) {
                    requesting.value = true
                    viewModelScope.launch { mutableEvents.emit(KeepAliveEvent.RequestPlaybackCapture) }
                } else playbackRepository.start(settingsRepository.settings.value)
            }
            return
        }
        if (playbackRepository.state.value.active || requesting.value) return
        updateSettings { settings ->
            when (action) {
                is KeepAliveAction.SelectSignal -> settings.copy(signal = action.signal)
                is KeepAliveAction.SetLevel -> settings.copy(
                    levelDbfs = KeepAliveSettings.levelFromProgress(action.progress)
                )

                is KeepAliveAction.StepLevel -> settings.copy(
                    levelDbfs = settings.levelDbfs + action.steps * KeepAliveSettings.LEVEL_STEP_DB
                )

                is KeepAliveAction.SetTimer -> settings.copy(timerMinutes = action.minutes)
                is KeepAliveAction.SetAdaptive -> settings.copy(adaptiveEnabled = action.enabled)
                KeepAliveAction.TogglePlayback -> settings
            }
        }
    }

    fun permissionFinished(messageRes: Int? = null) {
        requesting.value = false
        message.value = messageRes
    }
}
