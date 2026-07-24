# CLAUDE.md — Pickflow Android

> iOS Pickflow(`DDD-13-iOeS-iOS-develop/`)의 AGENTS.md를 Android로 1:1 이식한 컨벤션 문서.

## 1. 작업 흐름

- 브랜치: `develop` 베이스, 티켓 작업은 `feature/KAN-XX` 워크트리.
- 커밋: `[KAN-XX] 한국어 간단 요약` 형식 그대로 유지.
- 모든 코드 리뷰 PR은 TDD A→B→C 게이트 통과(아래 §5)를 전제로 한다.

## 2. 빌드 / 검증

| iOS | Android |
|---|---|
| `tuist generate --no-open` | `./gradlew assembleDebug` |
| `xcodebuild ... build` (시뮬레이터) | `./gradlew :app:installDebug` (에뮬레이터/실기기) |
| `xcodebuild test` | `./gradlew :app:testDebugUnitTest` |

내부 검증용 무 옵션 빌드는 사용 가능, 사용자가 명시한 빌드 명령이 있다면 그것을 우선 사용한다.

## 3. 코드 스타일

- Kotlin 표준 4-space 인덴트.
- 파일명 = 최상위 public 심볼명.
- 패키지 구조: `com.pickflow.android.{app, core, common, feature}` (PLAN.md §2).
- `companion object`는 상수/팩토리만, 비즈니스 로직 금지.

## 4. 아키텍처 규약 (요약, 상세는 PLAN.md §3)

- **MVVM + StateFlow + Hilt**. ViewModel 의존성 ≤5개, 모두 Service `interface`.
- **No Repository**: `core/services/protocols/`의 interface가 단일 데이터 추상화.
- **No enum/sealed Action**: ViewModel에 `suspend fun` 또는 `fun` 직접 노출.
- **개별 StateFlow** 노출. 단일 거대 `data class UiState` 금지.
- **LoadState** sealed class로 비동기 상태 표현(`common/ui/LoadState.kt`).
- 빈 결과는 `LoadState.Empty`로 매핑.

## 5. TDD 게이트 (Phase A → B → C)

세부 사항은 `docs/phases/`.

| Phase | 도구 | 출력 |
|---|---|---|
| A. ViewModel 단위 | JUnit5 + Turbine + MockK | `app/src/test/.../<Feature>ViewModelTest.kt` |
| B. UI 시나리오 | Compose UI Test (`createComposeRule`) | `app/src/androidTest/.../<Screen>UiTest.kt` |
| C. 스냅샷 | Paparazzi (호스트 사이드) | `app/src/test/.../<Screen>SnapshotTest.kt` |

각 단계는 직전 단계가 그린일 때만 진행한다.

## 6. 신규 Service 추가 절차 (3-step)

1. `app/src/main/java/com/pickflow/android/core/services/protocols/<Name>.kt`에 `interface` 선언.
2. `core/services/impl/<Default|Stub|Noop|Mock><Name>.kt`에 구현체 작성(@Inject 생성자).
3. `app/di/ServiceModule.kt`에 `@Binds`로 등록. `@Singleton` 여부는 PLAN.md §3.5 참고.

## 7. 금지 사항

- Repository 레이어 신설 금지.
- enum/sealed `Action` 클래스 도입 금지(MVI/TCA 패턴 도입 금지).
- 단일 거대 `data class UiState` 노출 금지(개별 StateFlow 사용).
- ViewModel에 `Context`/`Activity`/`Composable` 주입 금지.
- `GlobalScope` 사용 금지. `viewModelScope` 또는 명시적 스코프만.
- `tuist`/`xcodebuild`처럼 iOS 전용 명령 호출 금지.

## 8. 언어 정책

- 사용자 응답 및 문서는 **한국어**.
- 코드 식별자(클래스, 함수, 변수)는 **영문** 유지.
- 액센트/특수문자(예: `não`, `für`, `löschen`)는 ASCII 치환 금지.

## 9. 참고

- 마스터 플랜: `PLAN.md`
- 티켓 문서: `docs/KAN-XX/<feature>-implementation-prompt.md`
- 단계별 게이트: `docs/phases/phase-{a,b,c}-*.md`
- CI/CD·버전 규칙: `docs/ci-cd.md`
- 릴리스 AAB 빌드(로컬): `docs/release-build.md` (`release-build` 스킬)
- iOS 원본: `../DDD-13-iOeS-iOS-develop/`
