package ru.landilf.dactune

import android.app.Application
import ru.landilf.dactune.di.AppContainer

class DACtuneApplication : Application() {
    val container by lazy { AppContainer(this) }
}
