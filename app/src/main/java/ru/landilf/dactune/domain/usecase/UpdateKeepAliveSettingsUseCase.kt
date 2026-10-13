package ru.landilf.dactune.domain.usecase

import ru.landilf.dactune.domain.model.KeepAliveSettings
import ru.landilf.dactune.domain.repository.SettingsRepository

class UpdateKeepAliveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(transform: (KeepAliveSettings) -> KeepAliveSettings) {
        repository.save(transform(repository.settings.value).normalized())
    }
}
