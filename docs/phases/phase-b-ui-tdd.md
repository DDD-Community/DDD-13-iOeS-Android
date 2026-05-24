# Phase B — UI 시나리오 TDD

> iOS `phase-b-ui-tdd.md`의 Android 버전. 도구: Compose UI Test(`androidx.compose.ui.test`).

## 목적

Compose 화면이 ViewModel 상태(`LoadState.Idle/Loading/Loaded/Empty/Failed`)에 맞춰 올바른 위젯 트리를 렌더하는지 검증한다.

## 강제 사항

1. ViewModel은 가짜 인스턴스(`mockk<XxxViewModel>(relaxed = true)` 또는 fake 객체)를 직접 주입한다 — Hilt 없이.
2. `createComposeRule()`(또는 `createAndroidComposeRule<ComponentActivity>()`)을 사용.
3. 시나리오 별로 ViewModel `MutableStateFlow`를 직접 emit 한 뒤 `onNodeWithText/Tag/...`로 assertion.

## 템플릿

```kotlin
class LoginScreenUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun shows_loading_indicator_when_loading() {
        val vm = FakeLoginViewModel(initial = LoadState.Loading)
        composeRule.setContent { LoginScreen(viewModel = vm, onLoggedIn = {}) }
        composeRule.onNodeWithTag("login-progress").assertIsDisplayed()
    }
}
```

## 통과 기준

- `./gradlew :app:connectedDebugAndroidTest` 그린(에뮬레이터 필요).
- 각 화면은 최소: 초기 상태, Loading, Loaded, Failed 4가지 렌더 시나리오를 커버.

## 다음 단계

Phase B 그린 시 `phase-c-snapshot.md` 진행.
