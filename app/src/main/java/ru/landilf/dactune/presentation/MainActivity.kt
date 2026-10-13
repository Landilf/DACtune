package ru.landilf.dactune.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.landilf.dactune.DACtuneApplication
import ru.landilf.dactune.R

class MainActivity : AppCompatActivity() {
    private val container
        get() = (application as DACtuneApplication).container

    private lateinit var viewModel: KeepAliveViewModel
    private lateinit var screen: KeepAliveScreen

    private val recordPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                requestProjection()
            } else {
                viewModel.permissionFinished(R.string.status_record_audio_denied)
            }
        }

    private val projectionPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            container.androidPlaybackRepository.startAdaptive(
                container.settingsRepository.settings.value,
                result.resultCode,
                data
            )
            viewModel.permissionFinished()
        } else {
            viewModel.permissionFinished(R.string.status_capture_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(KeepAliveViewModel::class.java))
                return KeepAliveViewModel(
                    container.settingsRepository,
                    container.playbackRepository
                ) as T
            }

        })[KeepAliveViewModel::class.java]
        screen = KeepAliveScreen(this, viewModel::onAction)
        setContentView(screen.view)
        screen.render(viewModel.uiState.value)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(screen::render)
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            KeepAliveEvent.RequestPlaybackCapture -> requestCapturePermission()
                        }
                    }
                }
            }
        }
    }

    private fun requestCapturePermission() {
        if (Build.VERSION.SDK_INT < 29) {
            viewModel.permissionFinished(R.string.status_adaptive_unsupported)
        } else if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            recordPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            requestProjection()
        }
    }

    private fun requestProjection() {
        projectionPermission.launch(
            getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()
        )
    }
}
