# Phase A — ViewModel 단위 TDD

> iOS `phase-a-viewmodel-tdd.md`의 Android 버전. 도구만 치환(XCTest → JUnit5 + Turbine + MockK).

## 목적

`feature/<name>/<Name>ViewModel.kt`의 비즈니스 로직을 **순수 JVM 테스트**로 강제한다. Android Framework/UI 의존 없이 실행되어야 하며, `:app:testDebugUnitTest`만으로 검증 가능해야 한다.

## 강제 사항

1. 새 ViewModel은 **테스트가 먼저** 작성된 뒤에 구현으로 진행한다(Red → Green → Refactor).
2. 의존성은 모두 Service `interface` 이며, `mockk()`로 대체한다. 실제 구현체 호출 금지.
3. `Dispatchers.setMain(StandardTestDispatcher)` 패턴을 사용한다.
4. 상태 검증은 **Turbine `.test { ... }`** 또는 `viewModel.xxx.value` 둘 중 하나로 일관되게 한다.
5. 각 ViewModel 당 최소 시나리오:
    - 성공(Loaded)
    - 빈 결과(Empty) — 의미가 있는 경우만
    - 실패(Failed)
    - 분기/입력 경계(필터, 페이지네이션 등)

## 템플릿

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class XxxViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `action emits Loading then Loaded`() = runTest(testDispatcher) {
        val service = mockk<XxxService>()
        coEvery { service.doSomething() } returns expected

        val vm = XxxViewModel(service)
        vm.state.test {
            assertEquals(LoadState.Idle, awaitItem())
            vm.action()
            assertEquals(LoadState.Loading, awaitItem())
            assertEquals(LoadState.Loaded(expected), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## 통과 기준

- `./gradlew :app:testDebugUnitTest` 그린.
- 신규/변경 ViewModel은 적어도 성공 + 실패 시나리오 커버.
- 테스트는 Robolectric / Android 의존성 사용 금지(순수 JVM).

## 다음 단계

Phase A 그린 시 `phase-b-ui-tdd.md` 진행.
