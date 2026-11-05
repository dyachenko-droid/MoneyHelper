package ru.denpom.finance.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.denpom.finance.app.widget.AppInitializer
import javax.inject.Inject

@HiltAndroidApp
class MoneyApp : Application() {
    @Inject lateinit var initializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        initializer.start()
    }
}
