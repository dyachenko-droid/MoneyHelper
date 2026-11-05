package ru.denpom.finance.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.denpom.finance.app.data.launch.StartupIntegrations
import ru.denpom.finance.app.widget.AppInitializer
import javax.inject.Inject

@HiltAndroidApp
class MoneyApp : Application() {
    @Inject lateinit var initializer: AppInitializer
    @Inject lateinit var integrations: StartupIntegrations

    override fun onCreate() {
        super.onCreate()
        initializer.start()
        integrations.start()
    }
}
