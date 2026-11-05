package ru.denpom.finance.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.denpom.finance.app.data.launch.AppsFlyerAttributionRepository
import ru.denpom.finance.app.data.launch.FirebasePrivacyRepository
import ru.denpom.finance.app.data.launch.OneSignalNotificationRepository
import ru.denpom.finance.app.data.launch.SharedPreferencesLaunchRepository
import ru.denpom.finance.app.domain.launch.AcceptPrivacyUseCase
import ru.denpom.finance.app.domain.launch.AttributionRepository
import ru.denpom.finance.app.domain.launch.LaunchPreferences
import ru.denpom.finance.app.domain.launch.NotificationRepository
import ru.denpom.finance.app.domain.launch.PaidLinkComposer
import ru.denpom.finance.app.domain.launch.PrivacyLinkRepository
import ru.denpom.finance.app.domain.launch.RequestNotificationPermissionUseCase
import ru.denpom.finance.app.domain.launch.ResolveAttributionUseCase
import ru.denpom.finance.app.domain.launch.ResolveLaunchUseCase
import ru.denpom.finance.app.domain.launch.SyncNotificationIdentityUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LaunchBindings {
    @Binds abstract fun preferences(impl: SharedPreferencesLaunchRepository): LaunchPreferences
    @Binds abstract fun attribution(impl: AppsFlyerAttributionRepository): AttributionRepository
    @Binds abstract fun privacy(impl: FirebasePrivacyRepository): PrivacyLinkRepository
    @Binds abstract fun notifications(impl: OneSignalNotificationRepository): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object LaunchUseCasesModule {
    @Provides fun attribution(repository: AttributionRepository) = ResolveAttributionUseCase(repository)
    @Provides fun composer() = PaidLinkComposer()
    @Provides fun resolve(preferences: LaunchPreferences, attribution: ResolveAttributionUseCase,
        repository: AttributionRepository, privacy: PrivacyLinkRepository, composer: PaidLinkComposer) =
        ResolveLaunchUseCase(preferences, attribution, repository, privacy, composer)
    @Provides fun accept(preferences: LaunchPreferences) = AcceptPrivacyUseCase(preferences)
    @Provides fun notificationPermission(preferences: LaunchPreferences, notifications: NotificationRepository) =
        RequestNotificationPermissionUseCase(preferences, notifications)
    @Provides @Singleton fun identity(repository: AttributionRepository, notifications: NotificationRepository) =
        SyncNotificationIdentityUseCase(repository, notifications)
}
