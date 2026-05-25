# Pickflow Android — PLAN.md

> iOS 저장소 `DDD-13-iOeS-iOS-develop/`의 `Pickflow` 앱을 Android로 1:1 포팅하기 위한 마스터 플랜.
> iOS `PLAN.md`와 섹션 번호를 의도적으로 맞춰서 양 플랫폼 규약을 동기화한다.

---

## §0 Overview

- **목적**: iOS `Pickflow` SwiftUI 앱을 Jetpack Compose 기반 Android 앱으로 포팅.
- **이식 정책**:
    - iOS의 아키텍처 규약(MVVM + Service Protocol DI, No Repository, No enum Action, 개별 State 노출)을 **그대로** 유지.
    - `docs/` 구조와 `CLAUDE.md`(= iOS `AGENTS.md`) 컨벤션도 그대로 가져온다.
    - SwiftUI 코드 예시는 Compose 등가로 재작성, 그 외 문서/티켓 흐름은 동일.
- **모듈화 전략**: 초기에는 **단일 `:app` 모듈 + 패키지 계층 분리**. iOS Pickflow가 단일 Tuist 타깃인 것과 대칭. Gradle 멀티모듈 분리는 빌드 시간이 문제가 되는 시점에 고려.
- **Min SDK**: API 26 (Android 8.0). Target/Compile SDK는 최신 Stable.

---

## §1 Tech Stack

| 영역 | iOS | Android |
|---|---|---|
| UI Framework | SwiftUI | **Jetpack Compose** (Material3) |
| 상태관리 | MVVM + `@Published` + `ObservableObject` | **MVVM + StateFlow + `ViewModel` (androidx.lifecycle)** |
| 비동기 | Swift Concurrency (`async`/`await`, `@MainActor`) | **Kotlin Coroutines + Flow** |
| DI | Swinject (`AppContainer`) | **Hilt** (`@HiltAndroidApp`, `@Module`, `@Binds`) |
| 모듈화 | Tuist 단일 타깃 + glob 소스 | **단일 `:app` 모듈 + 패키지 계층** (`core/common/feature`) |
| 네비게이션 | SwiftUI `NavigationStack` | **Navigation Compose** (`NavHost`) |
| 네트워킹 | Alamofire 5.10+ | **Retrofit + OkHttp + kotlinx-serialization** |
| 이미지 | Native | **Coil 2.x (`coil-compose`)** |
| 지도 | NaverMaps iOS 3.23+ | **Naver Map Android SDK** |
| Auth | Kakao 2.27 + AppleAuth | **Kakao Android SDK** + **Google Sign-In** (Apple은 추후 OAuth 웹) |
| Analytics | Firebase 12.12 | **Firebase Android BoM** |
| 토큰 저장 | Keychain | **EncryptedSharedPreferences** 또는 **DataStore + Tink** |
| 권한/위치 | CoreLocation | **FusedLocationProviderClient** + Accompanist Permissions |
| 테스팅 | XCTest + SnapshotTesting | **JUnit5 + Turbine + MockK + Paparazzi(스냅샷)** |
| 빌드 | Tuist + Xcode | **Gradle (Kotlin DSL) + `libs.versions.toml`** |
| Min OS | iOS 26 | **Android API 26 (Android 8.0)** |
| 폰트 | Pretendard | **Pretendard** (assets/fonts + `FontFamily`) |

---

## §2 디렉터리 / 패키지 구조

```
DDD-13-iOeS-Android/
├─ app/
│   ├─ build.gradle.kts
│   └─ src/main/java/com/pickflow/android/
│       ├─ app/
│       │   ├─ PickflowApplication.kt       # @HiltAndroidApp
│       │   ├─ MainActivity.kt              # setContent { PickflowApp() }
│       │   ├─ di/                          # Hilt @Module — iOS AppContainer 대응
│       │   └─ navigation/                  # NavHost, Route sealed class
│       ├─ core/
│       │   ├─ network/                     # Retrofit, Interceptor, ApiClient
│       │   ├─ services/
│       │   │   ├─ protocols/               # interface XxxService (iOS *ServiceProtocol 1:1)
│       │   │   └─ impl/                    # 구현체
│       │   ├─ auth/                        # KakaoAuthProvider, (Apple/Google) 등
│       │   ├─ storage/                     # TokenStore, OnboardingCompletionStore
│       │   └─ analytics/                   # AnalyticsLogger (Firebase)
│       ├─ common/
│       │   ├─ designsystem/                # Color, Typography, Theme, Components
│       │   └─ ui/                          # LoadState, ext, modifier
│       └─ feature/
│           ├─ login/                       # KAN-76
│           ├─ onboarding/                  # KAN-100, KAN-106
│           ├─ myprofile/                   # KAN-54
│           ├─ spotlist/                    # KAN-52
│           ├─ spotdetail/                  # KAN-51, KAN-84, KAN-99
│           ├─ spotsearch/
│           ├─ spotregistration/
│           └─ map/                         # KAN-82
├─ docs/                                    # iOS 구조 그대로 (KAN-XX/, phases/)
├─ CLAUDE.md                                # iOS AGENTS.md의 Android 버전
├─ PLAN.md                                  # 본 파일
├─ README.md
├─ build.gradle.kts                         # root
├─ settings.gradle.kts
└─ gradle/libs.versions.toml
```

각 `feature/<name>/` 내부 표준 레이아웃:

```
feature/login/
├─ LoginViewModel.kt
├─ LoginScreen.kt          # Composable 진입점
├─ components/             # 화면 전용 Composable
└─ model/                  # 화면 전용 UiState/Model
```

---

## §3 권장 패턴 (iOS PLAN.md §3 1:1 이식)

### 3.1 ViewModel 규약

- `@HiltViewModel class XxxViewModel @Inject constructor(...) : ViewModel()`
- **생성자 주입은 Service `interface` 만**. Activity/Context/Composable 의존 금지.
- **의존성 ≤ 5개**. 초과 시 Service 합성/분리 신호(iOS SpotDetail의 9 deps 안티패턴 재현 금지).
- `viewModelScope`만 사용. GlobalScope 금지.

### 3.2 State 규약

- **개별 `StateFlow` 노출**. 단일 거대 `data class UiState` 금지(iOS의 "개별 @Published"와 동일 정책).
- 변경 가능한 내부 상태는 `private val _xxx = MutableStateFlow(...)`, 외부는 `val xxx: StateFlow<...> = _xxx.asStateFlow()`.
- 비동기 결과는 `LoadState` sealed class로 표현:
    ```kotlin
    sealed class LoadState<out T> {
        data object Idle : LoadState<Nothing>()
        data object Loading : LoadState<Nothing>()
        data class Loaded<T>(val value: T) : LoadState<T>()
        data object Empty : LoadState<Nothing>()
        data class Failed(val error: Throwable) : LoadState<Nothing>()
    }
    ```

### 3.3 Action 규약

- **No enum/sealed `Action`**. ViewModel에 **`suspend fun` 또는 일반 `fun`을 직접** 노출.
- TCA Action / MVI Intent 도입 금지(iOS와 동일).
- View(Composable)는 ViewModel 함수를 람다로 전달받아 호출.

### 3.4 Service 규약 (No Repository)

- `core/services/protocols/`의 `interface`가 **단일 데이터 추상화**. Repository 레이어 두지 않음.
- 신규 Service 추가 = **3-step**:
    1. `core/services/protocols/XxxService.kt` 에 `interface` 작성.
    2. `core/services/impl/DefaultXxxService.kt` 에 구현.
    3. `app/di/ServiceModule.kt` 에 `@Binds` 또는 `@Provides` 등록.
- 헬퍼: 필요한 경우 `getXxxService()` 류는 두지 않고 **Hilt 주입으로만** 해결(Android에서는 service locator anti-pattern 회피가 더 깔끔).

### 3.5 DI Scope

- `@Singleton`: `NetworkClient`, `TokenStore`, `LocationProvider`, 각 `AuthProvider`, `AnalyticsLogger`
- 기본(default scope, 매 주입마다 새 인스턴스): 그 외 Service 구현체
- `@ViewModelScoped`: ViewModel 보조 의존성이 있을 때만

### 3.6 LoadState 사용

- 빈 응답(`emptyList()`)은 `Loaded(emptyList())`가 아닌 **`Empty`**로 매핑한다(iOS와 동일).
- Failed는 사용자 노출 메시지가 아닌 `Throwable` 보존, 화면 레이어에서 변환.

### 3.7 코드 스타일

- 4-space 인덴트, ktlint default.
- 파일명 = 최상위 public 심볼명.
- `companion object`로 상수/팩토리만, 비즈니스 로직 금지.

---

## §4 Known Issues / Migration 우선순위

### 우선순위 1 — 인증 흐름 닫기

1. **KAN-76 Login** (Kakao 우선, Apple은 추후, Google 추가 검토)
2. **KAN-100 / KAN-106 Onboarding** (`OnboardingCompletionStore` = DataStore)
3. **KAN-54 MyProfile** (회원 탈퇴 포함)

### 우선순위 2 — Spot 코어

4. **KAN-52 SpotList**: `SpotListService`는 iOS와 동일하게 **Mock 구현부터** 시작. BE API 합류 시 교체.
5. **SpotSearch** (주소 검색 + 위치 권한)

### 우선순위 3 — SpotDetail (선제 분할)

6. **KAN-51 / KAN-84 / KAN-99 SpotDetail**: iOS에서 9 deps로 DEBUG Factory에 격리된 안티패턴.
    - Android는 **처음부터 ≤5 deps**로 split: 예) `SpotDetailViewModel`(조회/북마크/공유) + `SpotDetailActionsViewModel`(외부 앱 런처/위치).
    - 또는 도메인 합성 Service(`SpotDetailFacadeService`)로 묶는다.

### 우선순위 4 — 지도 / 등록

7. **KAN-82 Map clustering** (Naver Map Android SDK clustering)
8. **SpotRegistration** (사진 업로드, Storage Access Framework)

### 보류 / 후순위

- **Apple Login**: Android에서는 Sign in with Apple JS 또는 OAuth 웹뷰 우회 필요. v1 제외.
- **HomeMap stub** (iOS도 stub): API 합류 후 진행.

---

## §5 docs/ 이식 정책

iOS `docs/`를 다음 규칙으로 재구성:

- `docs/KAN-XX/` 폴더 골격은 **유지**, 내부 문서는 **Android 컨텍스트로 재작성**(SwiftUI 예시 → Compose, Swinject → Hilt, Alamofire → Retrofit).
- `docs/phases/phase-a-viewmodel-tdd.md`, `phase-b-ui-tdd.md`, `phase-c-snapshot.md`는 **TDD 게이트 그대로 유지**, 도구만 치환:
    - Phase A: **JUnit5 + Turbine + MockK** (`viewModel.xxx.test { ... }`)
    - Phase B: **Compose UI Test** (`androidx.compose.ui.test`)
    - Phase C: **Paparazzi** 스냅샷
- 다이어그램(`docs/layer-architecture.png`, `docs/git-branch-strategy.png`)은 **그대로 복사**.
- 새 화면 티켓 도입 시 `screen-tdd-prompt` 스킬의 Android 버전을 사용.

---

## §6 CLAUDE.md / AGENTS.md 이식

iOS `AGENTS.md`를 베이스로 `DDD-13-iOeS-Android/CLAUDE.md` 작성. 필수 포함 항목:

- **커밋 컨벤션**: `[KAN-XX] 한국어 요약` 그대로.
- **브랜치 전략**: `develop` 베이스, `feature/KAN-XX` 워크트리(iOS와 동일).
- **인덴트**: 4-space (Kotlin 표준).
- **빌드 명령**:
    - iOS `tuist generate --no-open` → Android `./gradlew assembleDebug`
    - iOS `xcodebuild` 시뮬레이터 빌드 검증 → Android `./gradlew :app:installDebug` + 에뮬레이터 실행
- **Service 추가 3-step (Android 버전)**: §3.4 그대로 명시.
- **금지 사항**: Repository 레이어 신설, enum/sealed Action 도입, 단일 거대 UiState, ViewModel에 Context 주입.
- **언어 정책**: iOS와 동일하게 사용자 응답은 한국어, 코드 식별자는 영문.

---

## §7 Phase 0 셋업 체크리스트

PLAN.md 승인 후 즉시 착수할 항목.

- [ ] `settings.gradle.kts` + `build.gradle.kts`(root) + `app/build.gradle.kts` (Kotlin DSL, AGP 최신)
- [ ] `gradle/libs.versions.toml`에 의존성 카탈로그 정의:
    - Compose BOM, Material3, Navigation Compose
    - Hilt (`hilt-android`, `hilt-compiler`, `hilt-navigation-compose`)
    - Retrofit + OkHttp + kotlinx-serialization-converter
    - Coil-compose
    - Kakao Android SDK (`user`)
    - Naver Map Android SDK
    - Firebase Android BoM (Analytics, Crashlytics)
    - DataStore (Preferences + Proto 선택)
    - JUnit5, Turbine, MockK, Paparazzi
- [ ] `PickflowApplication`(@HiltAndroidApp) + `MainActivity`
- [ ] `core/services/protocols/`에 iOS Service Protocol 14개 1:1 매핑 스텁(빈 interface):
    - `NetworkManager`, `TokenStore`, `UserService`, `AuthService`,
    - `KakaoAuthProvider`, `AppleAuthProvider`(boundary만), `SocialLoginService`,
    - `SpotService`, `SpotListService`(mock), `AddressService`, `ClusteringService`,
    - `BookmarkService`, `ShareIntentService`, `LocationService`,
    - `ExternalAppLauncher`, `AnalyticsLogger`
- [ ] `common/designsystem/`:
    - `Color.kt` (iOS `UIAsset.Colors` → Compose `Color` 토큰)
    - `Typography.kt` (Pretendard `FontFamily` + iOS `PickflowTypography` 매핑)
    - `PickflowTheme` Composable
- [ ] Hilt 모듈: `NetworkModule`, `StorageModule`, `ServiceModule`, `AuthModule`
- [ ] Phase A 테스트 인프라: JUnit5 runner, Turbine, MockK, 샘플 ViewModel 테스트 1개
- [ ] `docs/phases/phase-a-viewmodel-tdd.md` Android 버전 1차 작성
- [ ] `CLAUDE.md` 1차 작성

---

## §8 검증 방법

- PLAN.md를 사용자가 §0~§7 통독 → 승인되면 Phase 0 셋업 PR 착수.
- Phase 0 완료 기준:
    1. `./gradlew assembleDebug` 성공
    2. `./gradlew test` 성공 (샘플 ViewModel 테스트 1개 통과)
    3. 에뮬레이터에서 빈 `MainActivity` + `PickflowTheme` 적용 화면 표시
- 이후 각 KAN-XX 진행 시 iOS `docs/<TICKET>/` 문서를 Android 컨텍스트로 재작성한 뒤 TDD A→B→C 게이트로 머지.

---

## §9 Backend API 통합 — Phase 구현계획 & 검증

> 출처: BE OpenAPI 스펙 `https://pickflow-api.us/api/api-docs` (Title: Photo API v1.0.0)
> 체크리스트: `docs/API_SPEC.md` (26 endpoint, admin 3개 제외)
> 본 섹션은 **§7 Phase 0 셋업 완료 이후**의 백엔드 연동 로드맵.

### §9.0 공통 검증 규약

모든 Phase는 머지 전 다음 3-게이트 통과:

1. **Build gate**: `./gradlew :app:assembleDebug` 성공 + `:app:lintDebug` 신규 경고 0건.
2. **Unit test gate**: `./gradlew :app:testDebugUnitTest` 그린. 신규 Service impl은 **MockWebServer** 기반 happy/error/edge 케이스 최소 3건. 신규 매퍼 확장함수는 입력↔출력 동치 테스트 1건 이상.
3. **체크리스트 동기화 gate**: `docs/API_SPEC.md`의 해당 endpoint 항목을 `[ ]` → `[x]`로 전환하고 매핑된 Service.method 경로(파일:라인) 명시. 미동기화 시 리뷰 반려.

추가 공통:
- 외부 라이브러리 추가 시 `gradle/libs.versions.toml` 카탈로그 경유 (직접 `implementation("...")` 금지).
- Phase 간 PR은 분리. 한 PR = 한 Phase = 한 KAN 티켓 기준.

### §9.A Phase A — 네트워크 부트스트랩

**구현 항목**
- `core/network/NetworkModule.kt`: Hilt `@Module @InstallIn(SingletonComponent)`. `Json`, `HttpLoggingInterceptor`, `AuthInterceptor`(TokenStore 주입, Bearer 헤더), `TokenAuthenticator`(401 → `AuthApi.refresh` → 원요청 재시도, Mutex로 동시 401 직렬화), `OkHttpClient`, `Retrofit` 제공.
- `core/network/ApiResponse.kt`: `@Serializable data class ApiResponse<T>(success, code, message, data: T?)` + `fun <T> ApiResponse<T>.unwrap(): T` (실패 시 `ApiException` throw).
- `core/network/ApiException.kt`: `class ApiException(val code: String, message: String) : RuntimeException(message)`.
- `core/network/Multipart.kt`: `Uri → MultipartBody.Part` 유틸 (ContentResolver 기반).
- `secrets.defaults.properties` + `app/build.gradle.kts`: `PICKFLOW_API_BASE_URL` `buildConfigField`로 노출.
- 빈 패키지 생성: `core/network/{api, dto, mapper}/`.

**검증 방법**
- ✅ Build gate.
- ✅ Unit test:
    1. `ApiResponseTest`: `success=true,data=X → unwrap()=X` / `success=false → throws ApiException`.
    2. `AuthInterceptorTest` (MockWebServer): TokenStore에 토큰 있을 때 `Authorization` 헤더 부착, 없을 때 미부착.
    3. `TokenAuthenticatorTest` (MockWebServer): 첫 응답 401 → refresh 호출 → 새 토큰으로 재시도 → 최종 200. 두 번째 연속 401(refresh 실패) → 포기 + TokenStore.clear() 호출.
- ✅ 수동: 디버그 빌드에서 `OkHttp` 로그가 `BODY` 레벨로 출력되는지 logcat에서 확인 (`adb logcat -s OkHttp:D`).
- ❌ ViewModel/UI 영향 없음 (기존 스텁 그대로). 회귀 테스트는 기존 셋만 그린이면 통과.

### §9.B Phase B — 인증 (4 endpoint)

**구현 항목**
- DTO: `core/network/dto/auth/` — `KakaoLoginRequest`, `AppleLoginRequest`, `RefreshRequest`, `LogoutRequest`, `TokenResponseDto`, `UserProfileDto`, `AppleUserDto`, `AppleNameDto`.
- Api: `core/network/api/AuthApi.kt`.
- 도메인: `core/services/protocols/UserProfile.kt`. `AuthenticatedSession(tokens: SessionTokens, profile: UserProfile)` wrapper.
- Provider: `AppleAuthProvider` interface + `RealAppleAuthProvider` impl.
- 서비스 수정: `DefaultSocialLoginService`(API 호출 + TokenStore.save), `DefaultAuthService.logout()`(API 호출 후 clear).
- ServiceModule: `bindAppleAuthProvider` 추가.

**검증 방법**
- ✅ Build / 체크리스트 동기화 gate.
- ✅ Unit test:
    1. `DefaultSocialLoginServiceTest` (MockWebServer): kakao/apple happy → TokenStore에 access/refresh 저장 확인 + 반환 `AuthenticatedSession.profile` 매핑 확인.
    2. 401 응답 → `ApiException` propagate, TokenStore 변경 없음.
    3. `DefaultAuthServiceTest`: `logout()` 호출 시 `/v1/auth/logout` 발사 + 응답 후 TokenStore.clear() 호출 순서 검증.
    4. `LoginViewModelTest` (Turbine): 기존 테스트의 mock 반환 타입을 `AuthenticatedSession`으로 갱신, `session` StateFlow가 `Idle → Loading → Loaded(...)` 흐름 유지.
- ✅ 수동 (실제 BE 대상): 카카오 로그인 → logcat에서 `POST /v1/auth/kakao` + 200 응답 + 마이페이지 진입 가능 확인. 강제 로그아웃 후 보호 endpoint 호출 → 401 → refresh 자동 재시도 흐름 확인 (RefreshToken 만료 케이스는 BE와 협의 후 별도 시나리오).
- ✅ 회귀: 기존 `LoginViewModelTest`, `MyProfileViewModelTest` 그린 유지.

### §9.C Phase C — 스팟 조회 (4 endpoint)

**구현 항목**
- DTO: `core/network/dto/spot/` — `SpotItemDto`, `SpotListResponseDto`, `SpotDetailResponseDto`, `SpotPreviewResponseDto`, `SpotSummaryDto`, `SpotViewportResponseDto`.
- Api: `core/network/api/SpotApi.kt`.
- 도메인 재정의 (**broad change**): `SpotTheme` enum 교체(`SUNSET`, `YUNSEUL`), `Spot.id: String → Long`, `SpotPage(items, nextCursor)` → `SpotListPage(items, page: Int, hasNext: Boolean)`. 신규 `SpotDetail`, `SpotPreview`, `SpotMapMarker`, `ViewportBox`.
- 서비스 신규/교체: `DefaultSpotListService`(Mock 폐기), `DefaultSpotService`(Stub 폐기, `register()` 메서드는 §9.D `MySpotService`로 이전), `SpotMapService` + `DefaultSpotMapService`.
- ServiceModule: 3개 바인딩 교체/추가.

**검증 방법**
- ✅ Build / 체크리스트 동기화 gate.
- ✅ Unit test:
    1. 매퍼: `SpotItemDto.toSpot()`, `SpotDetailResponseDto.toSpotDetail()` — null/optional 필드, enum 매핑(서버 `SUNSET` → `SpotTheme.SUNSET`), nested address 분리(`address`, `addressRoad`, `addressJibun`) 검증.
    2. `DefaultSpotListServiceTest` (MockWebServer): page+theme+sort 조합으로 query string 직렬화 확인, `hasNext=true` 응답 처리, 빈 `spots[]` 응답 → 도메인은 빈 리스트 그대로 전달 (Empty 변환은 ViewModel 책임).
    3. `DefaultSpotMapServiceTest`: 4 꼭짓점 좌표 query 8개 누락 없이 전송.
    4. `SpotListViewModelTest` (Turbine): cursor 누적 로직 → page 누적 로직 리팩터 후 페이지 2회 로드 시 `spots` StateFlow가 누적 결과 emit. theme 변경 시 페이지 리셋 동작.
    5. `SpotDetailViewModelTest`: 비로그인(isBookmarked=false) / 로그인(서버 isBookmarked 반영) 시나리오.
- ✅ 수동: 디버그 빌드 실행 → 스팟 리스트 → 무한스크롤 1회 → 상세 진입 → preview 정보(거리 표시) 확인. 지도 화면이 구현된 시점이면 viewport 호출 logcat 확인 (`pickflow-api-pingpong` 스킬 활용 가능).
- ✅ 회귀: `feature/spotlist`, `feature/spotdetail`, `feature/map`의 모든 Compose UI 테스트 + Paparazzi 스냅샷 재생성 (theme/id 변경으로 baseline 갱신 필요 — PR에 갱신된 PNG 포함).

### §9.D Phase D — 마이페이지 / 보관함 / 북마크 / 마이스팟 (13 endpoint)

**구현 항목**
- DTO 패키지: `core/network/dto/{user, archive, bookmark, myspot}/`.
- Api: `UserApi`, `ArchiveApi`, `BookmarkApi`, `MySpotApi`.
- 도메인 신규: `MyPageHome`, `Archive`, `SavedSpot`/`SavedSpotPage`, `MySpot`/`MySpotPage`/`MySpotStatus`, `WithdrawalReasonType`.
- 서비스: `UserService` 확장 (5 메서드), `ArchiveService` 신규 (3), `BookmarkService` 변경 (in-memory 폐기 → 서버 add/remove + savedSpots), `MySpotService` 신규 (list, create). `SpotService.register()` 및 `SpotDraft`는 `MySpotService`로 이전 후 삭제.
- ServiceModule: 신규 4, 변경 2.

**검증 방법**
- ✅ Build / 체크리스트 동기화 gate (13개 항목).
- ✅ Unit test:
    1. 각 Service impl MockWebServer 테스트 — happy + 401(refresh 흐름 위임) + 400(`ApiException.code` 보존) 케이스.
    2. `MySpotService.create()`: multipart 빌더 테스트 — 이미지 part 이름, JSON meta part 이름이 BE 확정 명세대로 생성되는지 (Open TODO 해소 전엔 placeholder 기준).
    3. `BookmarkService.add/remove`: 응답 `bookmarkCount` 그대로 반환 검증.
    4. ViewModel 회귀: `MyProfileViewModel`, `SpotListViewModel`(북마크 toggle), `SpotDetailViewModel`(낙관적 토글 + 실패 롤백), `WithdrawalViewModel`, `SpotRegistrationViewModel` 갱신 + 그린.
- ✅ 통합/수동:
    1. 로그인 → 마이페이지 진입 → 닉네임/카운트 표시 확인.
    2. 프로필 이미지 변경 (갤러리 선택) → multipart 업로드 → 응답 URL이 Coil로 즉시 로드되는지.
    3. 스팟 상세에서 북마크 토글 → 서버 응답 `bookmarkCount`로 화면 갱신 → 저장된 스팟 목록에 반영.
    4. 회원 탈퇴 사유 등록 → 탈퇴 → 토큰 클리어 + 로그인 화면 복귀.
- ✅ 데이터 불변식: in-memory `Set<String>` 캐시 제거 후 화면 간 북마크 상태가 서버 응답만으로 일관되는지 회귀 (저장된 스팟 ↔ 상세 ↔ 리스트 카드).

### §9.E Phase E — 게시판 / 신고 / 알림 (5 endpoint)

**구현 항목**
- DTO: `core/network/dto/{board, report, alarm}/`.
- Api: `BoardApi`, `SpotReportApi`, `MySpotAlarmApi`.
- 도메인 신규: `BoardPost`, `BoardPostDetail`, `SpotAlarm`.
- 서비스 신규: `BoardService`, `SpotReportService`, `MySpotAlarmService`.
- 화면 미존재 도메인은 ViewModel 작업 보류 가능 — Service/Api/DTO 단위만 머지.

**검증 방법**
- ✅ Build / 체크리스트 동기화 gate.
- ✅ Unit test:
    1. 각 Service impl MockWebServer 테스트 — happy + validation error(신고 content 5자 미만 → 서버 400 → `ApiException.code` 보존).
    2. Board 페이징: `hasNext=true → false` 흐름 + `pinned` 정렬 보존 검증.
    3. Alarm: `enabled` toggle → 응답 `SpotAlarmResponse.enabled` 그대로 반영.
- ✅ 통합/수동: 화면 구현 시점에 추가 (별도 KAN 티켓).
- ✅ 회귀: 신규 Service 추가만으로는 기존 화면 영향 없음 → 기존 테스트 셋 그린 유지만 확인.

### §9.Z 통합 검증 (모든 Phase 완료 후)

**End-to-end 시나리오** (수동, 실제 BE):
1. 비로그인 → 스팟 리스트 → 상세(`isBookmarked=false`) → 북마크 시도 → 로그인 프롬프트 → 카카오 로그인 → 자동 북마크 → 저장된 스팟에 반영.
2. 프로필 수정(닉네임 + 이미지) → 마이페이지 즉시 반영.
3. 나만의 스팟 등록 → PENDING 상태로 마이스팟 리스트 노출 → 알림 구독 ON.
4. 게시판(공지) 진입 → 페이지 2회 로드 → 상세 진입.
5. 회원 탈퇴 → 사유 등록 → 토큰 클리어 → 로그인 화면.

**스펙 변경 대응**:
- BE 스펙 갱신 시 `curl https://pickflow-api.us/api/api-docs > /tmp/pickflow_openapi.json` → diff 후 `docs/API_SPEC.md`와 본 §9의 영향 항목 동기화. 매뉴얼 diff가 부담되면 `openapi-diff` 도구 도입 검토 (별도 TODO).
- Open TODO (BE 확인 필요)는 `docs/API_SPEC.md` 말미 섹션에 위임. 해소될 때마다 §9.X의 관련 항목 갱신.

---

## 부록 A — iOS Service Protocol → Android Interface 매핑 요약

| iOS Protocol | Android interface 위치 | Scope |
|---|---|---|
| `NetworkManager` | `core/network/NetworkClient.kt` | `@Singleton` |
| `TokenStore` | `core/storage/TokenStore.kt` | `@Singleton` |
| `UserServiceProtocol` | `core/services/protocols/UserService.kt` | default |
| `AuthServiceProtocol` | `core/services/protocols/AuthService.kt` | default |
| `KakaoAuthProviderProtocol` | `core/auth/KakaoAuthProvider.kt` | `@Singleton` |
| `SocialLoginServiceProtocol` | `core/services/protocols/SocialLoginService.kt` | default |
| `SpotServiceProtocol` | `core/services/protocols/SpotService.kt` | default |
| `SpotListServiceProtocol` | `core/services/protocols/SpotListService.kt` | default (mock impl) |
| `AddressServiceProtocol` | `core/services/protocols/AddressService.kt` | default |
| `BookmarkServiceProtocol` | `core/services/protocols/BookmarkService.kt` | default |
| `LocationServiceProtocol` | `core/services/protocols/LocationService.kt` | `@Singleton` |
| `ShareIntentServiceProtocol` | `core/services/protocols/ShareIntentService.kt` | default |
| `ExternalAppLauncherProtocol` | `core/services/protocols/ExternalAppLauncher.kt` | default |
| `ClusteringServiceProtocol` | `core/services/protocols/ClusteringService.kt` | default |
| `AnalyticsLoggerProtocol` | `core/analytics/AnalyticsLogger.kt` | `@Singleton` |
