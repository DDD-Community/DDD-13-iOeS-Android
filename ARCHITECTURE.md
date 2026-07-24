# ARCHITECTURE.md — Pickflow Android

Pickflow Android 앱의 **아키텍처 규약** 문서. 컨벤션 요약은 [`CLAUDE.md`](./CLAUDE.md),
CI/CD·버전 규칙은 [`docs/ci-cd.md`](./docs/ci-cd.md) 를 참고한다.

> iOS 저장소 `../DDD-13-iOeS-iOS-develop/` 의 규약을 Android 로 1:1 이식한 것이 기준이다.
> 양 플랫폼의 아키텍처 원칙(MVVM + Service 추상화 DI, No Repository, No enum Action,
> 개별 State 노출)을 동일하게 유지한다.

## 핵심 원칙 (요약)

- **MVVM + StateFlow + Hilt**. ViewModel 은 Service `interface` 에만 의존한다.
- **No Repository** — `core/services/protocols/` 의 interface 가 단일 데이터 추상화.
- **No enum/sealed Action** — ViewModel 에 `fun`/`suspend fun` 을 직접 노출(MVI/TCA 금지).
- **개별 StateFlow 노출** — 단일 거대 `data class UiState` 금지.
- **LoadState** sealed class 로 비동기 상태 표현, 빈 결과는 `Empty`.
- **단일 `:app` 모듈 + 패키지 계층**. 멀티모듈은 빌드 시간이 문제가 될 때 고려.

---

## §1 기술 스택

| 영역 | iOS | Android |
|---|---|---|
| UI Framework | SwiftUI | **Jetpack Compose** (Material3) |
| 상태관리 | MVVM + `@Published` + `ObservableObject` | **MVVM + StateFlow + `ViewModel`(androidx.lifecycle)** |
| 비동기 | Swift Concurrency (`async`/`await`, `@MainActor`) | **Kotlin Coroutines + Flow** |
| DI | Swinject (`AppContainer`) | **Hilt** (`@HiltAndroidApp`, `@Module`, `@Binds`) |
| 모듈화 | Tuist 단일 타깃 | **단일 `:app` 모듈 + 패키지 계층**(`core`/`common`/`feature`) |
| 네비게이션 | SwiftUI `NavigationStack` | **Navigation Compose** (`NavHost`) |
| 네트워킹 | Alamofire | **Retrofit + OkHttp + kotlinx-serialization** |
| 이미지 | Native | **Coil** (`coil-compose`) |
| 지도 | NaverMaps iOS | **Naver Map Android SDK** |
| Auth | Kakao + AppleAuth | **Kakao Android SDK** + Apple OAuth(웹) |
| Analytics | Firebase | **Firebase Android BoM** (Analytics) |
| 토큰 저장 | Keychain | **EncryptedSharedPreferences** |
| 권한/위치 | CoreLocation | **FusedLocationProviderClient** |
| 테스팅 | XCTest + SnapshotTesting | **JUnit5 + Turbine + MockK + Paparazzi(스냅샷)** |
| 빌드 | Tuist + Xcode | **Gradle (Kotlin DSL) + `libs.versions.toml`** |
| Min OS | iOS 26 | **Android API 26 (Android 8.0)** |
| 폰트 | Pretendard | **Pretendard** (`FontFamily`) |

- **버전 카탈로그가 단일 출처다.** 라이브러리/플러그인 버전은 모두 `gradle/libs.versions.toml`
  에서 관리한다. Paparazzi 등 Compose BOM·AGP 조합에 민감한 도구는 이 카탈로그 값을 그대로 따른다.

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
│       │   └─ navigation/                  # NavHost, Route
│       ├─ core/
│       │   ├─ network/                     # Retrofit, Interceptor, ApiClient
│       │   ├─ services/
│       │   │   ├─ protocols/               # interface XxxService (iOS *ServiceProtocol 1:1)
│       │   │   └─ impl/                    # 구현체 (Default/Real/Android/Encrypted…)
│       │   └─ analytics/                   # AnalyticsEvent + events/ (GA 이벤트 정의)
│       ├─ common/
│       │   ├─ designsystem/                # Color, Typography, Theme, Components
│       │   └─ ui/                          # LoadState, ext, modifier
│       └─ feature/
│           ├─ login/  onboarding/  myprofile/
│           ├─ spotlist/  spotdetail/  spotsearch/  spotregistration/
│           ├─ map/  home/  notice/  archive/
│           └─ accountmanagement/  withdrawal/  forceupdate/  debug/
├─ docs/                                    # 티켓(KAN-XX/), 단계별 게이트(phases/), ci-cd.md
├─ CLAUDE.md                                # 컨벤션 요약(= iOS AGENTS.md)
├─ ARCHITECTURE.md                          # 본 파일
├─ build.gradle.kts / settings.gradle.kts
└─ gradle/libs.versions.toml
```

각 `feature/<name>/` 내부 표준 레이아웃:

```
feature/spotdetail/
├─ SpotDetailViewModel.kt
├─ SpotDetailScreen.kt      # Composable 진입점
├─ components/              # 화면 전용 Composable
└─ model/                   # 화면 전용 UiState/Model (필요 시)
```

---

## §3 권장 패턴

### 3.1 ViewModel 규약

- `@HiltViewModel class XxxViewModel @Inject constructor(...) : ViewModel()`
- **생성자 주입은 Service `interface` 만.** Activity/Context/Composable 의존 금지.
- **의존성 ≤ 5개.** 초과 시 Service 합성/분리 신호로 본다(iOS SpotDetail 9-deps 안티패턴 재현 금지).
- `viewModelScope` 만 사용. `GlobalScope` 금지.

### 3.2 State 규약

- **개별 `StateFlow` 노출.** 단일 거대 `data class UiState` 금지(iOS "개별 @Published" 정책과 동일).
- 내부는 `private val _xxx = MutableStateFlow(...)`, 외부는 `val xxx: StateFlow<...> = _xxx.asStateFlow()`.
- 비동기 결과는 `LoadState` sealed class 로 표현:
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

- **No enum/sealed `Action`.** ViewModel 에 `fun`/`suspend fun` 을 **직접** 노출.
- TCA Action / MVI Intent 도입 금지(iOS 와 동일).
- View(Composable)는 ViewModel 함수를 람다로 전달받아 호출한다.

### 3.4 Service 규약 (No Repository)

- `core/services/protocols/` 의 `interface` 가 **단일 데이터 추상화.** Repository 레이어를 두지 않는다.
- 신규 Service 추가 = **3-step**:
    1. `core/services/protocols/XxxService.kt` 에 `interface` 작성.
    2. `core/services/impl/<Default|Real|Android|Encrypted…>XxxService.kt` 에 구현(@Inject 생성자).
    3. `app/di/ServiceModule.kt` 에 `@Binds`(또는 `@Provides`) 등록.
- Service Locator(`getXxxService()`) 를 두지 않고 **Hilt 주입으로만** 해결한다.

### 3.5 DI Scope

- `@Singleton`: `NetworkClient`, `TokenStore`, `LocationService`, 각 `AuthProvider`, `AnalyticsLogger`
- 기본 스코프(매 주입마다 새 인스턴스): 그 외 Service 구현체
- `@ViewModelScoped`: ViewModel 보조 의존성이 있을 때만

### 3.6 LoadState 사용

- 빈 응답(`emptyList()`)은 `Loaded(emptyList())` 가 아닌 **`Empty`** 로 매핑한다(iOS 와 동일).
- `Failed` 는 사용자 노출 메시지가 아닌 `Throwable` 을 보존하고, 화면 레이어에서 변환한다.

### 3.7 코드 스타일

- Kotlin 표준 4-space 인덴트, ktlint default.
- 파일명 = 최상위 public 심볼명.
- `companion object` 는 상수/팩토리만. 비즈니스 로직 금지.
- 코드 식별자는 영문, 문서/주석/사용자 응답은 한국어([`CLAUDE.md`](./CLAUDE.md) §8).
