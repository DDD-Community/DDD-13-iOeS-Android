package com.pickflow.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.network.api.ArchiveApi
import com.pickflow.android.core.network.api.AuthApi
import com.pickflow.android.core.network.api.BoardApi
import com.pickflow.android.core.network.api.BookmarkApi
import com.pickflow.android.core.network.api.MySpotApi
import com.pickflow.android.core.network.api.MySpotAlarmApi
import com.pickflow.android.core.network.api.SpotReportApi
import com.pickflow.android.core.network.api.RefreshApi
import com.pickflow.android.core.network.api.SpotApi
import com.pickflow.android.core.network.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        auth: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(auth)
        .addInterceptor(logging)
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.PICKFLOW_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideSpotApi(retrofit: Retrofit): SpotApi = retrofit.create(SpotApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideArchiveApi(retrofit: Retrofit): ArchiveApi = retrofit.create(ArchiveApi::class.java)

    @Provides
    @Singleton
    fun provideBookmarkApi(retrofit: Retrofit): BookmarkApi = retrofit.create(BookmarkApi::class.java)

    @Provides
    @Singleton
    fun provideMySpotApi(retrofit: Retrofit): MySpotApi = retrofit.create(MySpotApi::class.java)

    @Provides
    @Singleton
    fun provideBoardApi(retrofit: Retrofit): BoardApi = retrofit.create(BoardApi::class.java)

    @Provides
    @Singleton
    fun provideSpotReportApi(retrofit: Retrofit): SpotReportApi = retrofit.create(SpotReportApi::class.java)

    @Provides
    @Singleton
    fun provideMySpotAlarmApi(retrofit: Retrofit): MySpotAlarmApi = retrofit.create(MySpotAlarmApi::class.java)

    @Provides
    @Singleton
    fun provideAppVersionApi(retrofit: Retrofit): com.pickflow.android.core.network.api.AppVersionApi =
        retrofit.create(com.pickflow.android.core.network.api.AppVersionApi::class.java)

    // --- Refresh 전용 (AuthInterceptor / Authenticator 미부착 OkHttpClient) ---

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttp(logging: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshRetrofit(
        @Named("refresh") client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.PICKFLOW_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideRefreshApi(@Named("refresh") retrofit: Retrofit): RefreshApi =
        retrofit.create(RefreshApi::class.java)

    // --- Kakao Local API 전용 (별도 base URL, AuthInterceptor 미부착) ---
    // iOS `AddressService` 가 직접 호출하는 https://dapi.kakao.com/ 정합.

    @Provides
    @Singleton
    @Named("kakaoLocal")
    fun provideKakaoLocalRetrofit(
        @Named("refresh") client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideKakaoLocalApi(
        @Named("kakaoLocal") retrofit: Retrofit,
    ): com.pickflow.android.core.network.api.KakaoLocalApi =
        retrofit.create(com.pickflow.android.core.network.api.KakaoLocalApi::class.java)

    @Provides
    @Named("kakaoRestApiKey")
    fun provideKakaoRestApiKey(): String = BuildConfig.KAKAO_REST_API_KEY
}
