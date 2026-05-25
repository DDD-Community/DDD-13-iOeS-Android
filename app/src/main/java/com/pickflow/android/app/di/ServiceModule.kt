package com.pickflow.android.app.di

import com.pickflow.android.core.services.impl.DefaultArchiveService
import com.pickflow.android.core.services.impl.DefaultAuthService
import com.pickflow.android.core.services.impl.DefaultBoardService
import com.pickflow.android.core.services.impl.DefaultBookmarkService
import com.pickflow.android.core.services.impl.DefaultMySpotService
import com.pickflow.android.core.services.impl.DefaultSocialLoginService
import com.pickflow.android.core.services.impl.DefaultUserService
import com.pickflow.android.core.services.impl.GridClusteringService
import com.pickflow.android.core.services.impl.InMemoryOnboardingCompletionStore
import com.pickflow.android.core.services.impl.InMemoryTokenStore
import com.pickflow.android.core.services.impl.DefaultSpotListService
import com.pickflow.android.core.services.impl.NoopAnalyticsLogger
import com.pickflow.android.core.services.impl.NoopExternalAppLauncher
import com.pickflow.android.core.services.impl.NoopShareIntentService
import com.pickflow.android.core.services.impl.RealKakaoAuthProvider
import com.pickflow.android.core.services.impl.StubAddressService
import com.pickflow.android.core.services.impl.StubAppleAuthProvider
import com.pickflow.android.core.services.impl.DefaultSpotMapService
import com.pickflow.android.core.services.impl.StubLocationService
import com.pickflow.android.core.services.impl.DefaultSpotService
import com.pickflow.android.core.services.protocols.AppleAuthProvider
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ClusteringService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.BoardService
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotService
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
    abstract fun bindTokenStore(impl: InMemoryTokenStore): TokenStore

    @Binds
    abstract fun bindKakaoAuthProvider(impl: RealKakaoAuthProvider): KakaoAuthProvider

    @Binds
    abstract fun bindAppleAuthProvider(impl: StubAppleAuthProvider): AppleAuthProvider

    @Binds
    abstract fun bindSocialLoginService(impl: DefaultSocialLoginService): SocialLoginService

    @Binds
    abstract fun bindAuthService(impl: DefaultAuthService): AuthService

    @Binds
    abstract fun bindArchiveService(impl: DefaultArchiveService): ArchiveService

    @Binds
    abstract fun bindOnboardingCompletionStore(
        impl: InMemoryOnboardingCompletionStore
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
    abstract fun bindLocationService(impl: StubLocationService): LocationService

    @Binds
    abstract fun bindSpotService(impl: DefaultSpotService): SpotService

    @Binds
    abstract fun bindSpotMapService(impl: DefaultSpotMapService): SpotMapService

    @Binds
    abstract fun bindAddressService(impl: StubAddressService): AddressService

    @Binds
    abstract fun bindShareIntentService(impl: NoopShareIntentService): ShareIntentService

    @Binds
    abstract fun bindExternalAppLauncher(impl: NoopExternalAppLauncher): ExternalAppLauncher

    @Binds
    abstract fun bindClusteringService(impl: GridClusteringService): ClusteringService

    @Binds
    abstract fun bindAnalyticsLogger(impl: NoopAnalyticsLogger): AnalyticsLogger
}
