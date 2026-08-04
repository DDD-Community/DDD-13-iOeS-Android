# Phase B — UI 시나리오 TDD

> iOS `phase-b-ui-tdd.md`의 Android 버전. 도구: Compose UI Test(`androidx.compose.ui.test`) **+ Robolectric**.

## 목적

Compose 화면이 ViewModel 상태(`LoadState.Idle/Loading/Loaded/Empty/Failed`)에 맞춰 올바른 위젯 트리를 렌더하는지 검증한다.

## 실행 환경 — 에뮬레이터 없이 호스트 JVM에서 돈다

이 저장소에는 **`app/src/androidTest` 소스셋이 없다.** `compose-ui-test-junit4`를 `testImplementation`으로 받아 **Robolectric 위에서** 실행하므로(`app/build.gradle.kts`의 `unitTests { isIncludeAndroidResources = true }`), UI 테스트도 Phase A·C와 같은 `app/src/test/` 아래에 둔다.

- 배치: `app/src/test/java/com/pickflow/android/feature/<name>/<Screen>UiTest.kt`
- 연결 기기·에뮬레이터 불필요. `connectedDebugAndroidTest`는 이 프로젝트에서 실행되지 않는다.

## 강제 사항

1. 클래스에 Robolectric 3종 어노테이션을 **반드시** 붙인다. 빠뜨리면 Compose 룰이 Android 리소스를 찾지 못해 실패한다.
    ```kotlin
    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    ```
2. **JUnit4 API를 쓴다** — `org.junit.Test` / `org.junit.Rule`. Phase A의 `org.junit.jupiter.api.Test`(JUnit5)를 여기 섞으면 `@get:Rule`이 무시돼 룰이 적용되지 않는다. (Gradle은 `useJUnitPlatform()`이지만 vintage 엔진으로 JUnit4를 함께 돌린다.)
3. ViewModel은 **실제 인스턴스를 만들되 생성자의 Service만 `mockk(relaxed = true)`로 채운다.** ViewModel 자체를 목으로 만들지 않는다 — 상태 전이 로직이 화면과 함께 검증돼야 한다. Hilt는 쓰지 않고 생성자를 직접 호출한다.
4. `createComposeRule()`을 사용하고, 컨텐츠는 `PickflowTheme { ... }`로 감싼다.
5. 조회는 `onNodeWithTag`(안정 식별자) 우선. 문구 자체가 검증 대상일 때만 `onNodeWithText`.

## 템플릿

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h950dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun viewModel() = LoginViewModel(
        mockk<KakaoAuthProvider>(relaxed = true),
        mockk<SocialLoginService>(relaxed = true),
        mockk<AuthService>(relaxed = true),
    )

    @Test
    fun renders_login_entry_points() {
        composeRule.setContent {
            PickflowTheme { LoginScreen(viewModel = viewModel(), onLoggedIn = {}) }
        }
        composeRule.onNodeWithTag("login-screen").assertIsDisplayed()
        composeRule.onNodeWithText("카카오로 로그인").assertIsDisplayed()
    }
}
```

특정 상태를 강제로 렌더해야 하면 Service 목의 반환값으로 상태를 만들거나, 화면에서 stateless 컨텐츠 Composable(`XxxScreenContent`)을 추출해 상태를 인자로 직접 넣는다. 후자는 Phase C 스냅샷과 대상을 공유하므로 권장한다.

## 통과 기준

- `./gradlew :app:testDebugUnitTest` 그린. (Phase A와 같은 태스크에서 함께 돈다.)
- 각 화면은 최소: 초기 상태, Loading, Loaded, Failed 4가지 렌더 시나리오를 커버.
- 새 화면은 `app/src/test/.../<Screen>UiTest.kt` 1개 이상 추가.

## 다음 단계

Phase B 그린 시 `phase-c-snapshot.md` 진행.
