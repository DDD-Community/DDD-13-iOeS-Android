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
