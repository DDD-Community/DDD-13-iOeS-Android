package com.pickflow.android.app.di

import com.pickflow.android.core.services.impl.DefaultArchiveService
import com.pickflow.android.core.services.impl.DefaultAuthService
import com.pickflow.android.core.services.impl.DefaultBoardService
import com.pickflow.android.core.services.impl.DefaultBookmarkService
import com.pickflow.android.core.services.impl.DefaultMySpotAlarmService
import com.pickflow.android.core.services.impl.DefaultMySpotService
import com.pickflow.android.core.services.impl.DefaultSpotReportService
import com.pickflow.android.core.services.impl.DefaultSocialLoginService
import com.pickflow.android.core.services.impl.DefaultUserService
import com.pickflow.android.core.services.impl.DataStoreOnboardingCompletionStore
import com.pickflow.android.core.services.impl.EncryptedTokenStore
import com.pickflow.android.core.services.impl.DefaultSpotListService
import com.pickflow.android.core.services.impl.FirebaseAnalyticsLogger
import com.pickflow.android.core.services.impl.AndroidExternalAppLauncher
import com.pickflow.android.core.services.impl.AndroidShareIntentService
import com.pickflow.android.core.services.impl.RealKakaoAuthProvider
import com.pickflow.android.core.services.impl.DefaultAddressService
import com.pickflow.android.core.services.impl.RealAppleAuthProvider
import com.pickflow.android.core.services.impl.DefaultSpotMapService
import com.pickflow.android.core.services.impl.DefaultLocationService
import com.pickflow.android.core.services.impl.DefaultSpotService
import com.pickflow.android.core.services.protocols.AppleAuthProvider
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.BoardService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotAlarmService
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.SpotReportService
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotMapService
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SocialLoginService
import com.pickflow.android.core.services.protocols.TokenStore
import com.pickflow.android.core.services.protocols.UserService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    abstract fun bindUserService(impl: DefaultUserService): UserService

    @Binds
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    abstract fun bindKakaoAuthProvider(impl: RealKakaoAuthProvider): KakaoAuthProvider

    @Binds
    abstract fun bindAppleAuthProvider(impl: RealAppleAuthProvider): AppleAuthProvider

    @Binds
    abstract fun bindSocialLoginService(impl: DefaultSocialLoginService): SocialLoginService

    @Binds
    abstract fun bindAuthService(impl: DefaultAuthService): AuthService

    @Binds
    abstract fun bindArchiveService(impl: DefaultArchiveService): ArchiveService

    @Binds
    abstract fun bindOnboardingCompletionStore(
        impl: DataStoreOnboardingCompletionStore
    ): OnboardingCompletionStore

    @Binds
    abstract fun bindSpotListService(impl: DefaultSpotListService): SpotListService

    @Binds
    abstract fun bindBookmarkService(impl: DefaultBookmarkService): BookmarkService

    @Binds
    abstract fun bindMySpotService(impl: DefaultMySpotService): MySpotService

    @Binds
    abstract fun bindBoardService(impl: DefaultBoardService): BoardService

    @Binds
    abstract fun bindSpotReportService(impl: DefaultSpotReportService): SpotReportService

    @Binds
    abstract fun bindMySpotAlarmService(impl: DefaultMySpotAlarmService): MySpotAlarmService

    @Binds
    abstract fun bindLocationService(impl: DefaultLocationService): LocationService

    @Binds
    abstract fun bindSpotService(impl: DefaultSpotService): SpotService

    @Binds
    abstract fun bindSpotMapService(impl: DefaultSpotMapService): SpotMapService

    @Binds
    abstract fun bindAddressService(impl: DefaultAddressService): AddressService

    @Binds
    abstract fun bindShareIntentService(impl: AndroidShareIntentService): ShareIntentService

    @Binds
    abstract fun bindExternalAppLauncher(impl: AndroidExternalAppLauncher): ExternalAppLauncher

    @Binds
    abstract fun bindAnalyticsLogger(impl: FirebaseAnalyticsLogger): AnalyticsLogger

    @Binds
    abstract fun bindAppVersionService(
        impl: com.pickflow.android.core.services.impl.DefaultAppVersionService,
    ): com.pickflow.android.core.services.protocols.AppVersionService
}
