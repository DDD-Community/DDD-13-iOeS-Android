# [{{TICKET}}] {{SCREEN_NAME}} 화면 구현 통합 프롬프트

> **이 프롬프트의 사용법**: `screen-tdd-prompt` 스킬이 이 템플릿을 복제·치환해서 `docs/{{TICKET}}/{{SCREEN_SLUG}}-implementation-prompt.md`로 저장한다.
>
> **방법론은 여기 없다.** TDD 단계별 디테일은 단계 진입 시 리프 문서를 읽는다:
> - Phase A: `docs/phases/phase-a-viewmodel-tdd.md`
> - Phase B: `docs/phases/phase-b-ui-tdd.md`
> - Phase C: `docs/phases/phase-c-snapshot.md`
>
> 본 문서는 **이 화면에만 해당하는 사실**(스코프, API, 정책, 에셋, 컴포넌트 매핑)을 담는다.

---

## 0. 작업 컨텍스트 (선결 정보)

**브랜치**: `{{BRANCH}}` (`pickflow-worktree-setup` 스킬로 워크트리 준비 완료)
**티켓**: {{TICKET}} (Jira)
**전체 화면 Figma**: `https://www.figma.com/design/{{FIGMA_FILE_KEY}}/?node-id={{FIGMA_ROOT_NODE_ID}}`

**프로젝트 가정 (재탐색 불필요 — 상세는 `ARCHITECTURE.md`)**:
- Jetpack Compose(Material3) + **MVVM + StateFlow + Hilt**, 단일 `:app` 모듈 + 패키지 계층
- 패키지: `com.pickflow.android.{app, core, common, feature}` — 화면은 `feature/{{FEATURE_PKG}}/`
- **No Repository** — `core/services/protocols/`의 `interface XxxService`가 단일 데이터 추상화
- **No enum/sealed Action** — ViewModel에 `fun`/`suspend fun` 직접 노출(MVI/TCA 금지)
- **개별 StateFlow 노출** — 단일 거대 `data class UiState` 금지. 비동기는 `common/ui/LoadState.kt`
- DI: `@HiltViewModel` + 생성자 주입(Service `interface`만, ≤5개). `app/di/ServiceModule.kt`에 `@Binds` 등록
- 네트워킹 Retrofit + OkHttp + kotlinx-serialization, 이미지 Coil, 지도 Naver Map Android SDK
- 디자인 시스템: `common/designsystem/`의 `PickflowTheme` / `PickflowColors` / `PickflowTypography`
- 테스트는 전부 `app/src/test/`(호스트 JVM). Phase A는 JUnit5, Phase B·C는 JUnit4

<!-- 프로젝트가 위 가정과 다르면 여기 추가 메모 -->

---

## 1. 스코프

**구현 범위**:
<!-- TODO: Jira 티켓 {{TICKET}}의 acceptance criteria를 옮겨오기 -->

**범위 밖**:
<!-- TODO: 의도적으로 뺀 것 -->

---

## 2. 핵심 정책 결정 (사용자 확정)

| # | 항목 | 결정 |
|---|---|---|
<!-- TODO -->

---

## 3. API 매핑

| UI 동작 | Endpoint | 비고 |
|---|---|---|
<!-- TODO -->

---

## 4. 신규/수정 파일 목록

**신규**
```
feature/{{FEATURE_PKG}}/
├─ {{SCREEN_NAME_PASCAL}}ViewModel.kt
├─ {{SCREEN_NAME_PASCAL}}Screen.kt      # Composable 진입점
├─ components/                          # 화면 전용 Composable (stateless 컨텐츠 포함)
└─ model/                               # 화면 전용 모델 (필요 시)
<!-- TODO: core/services/protocols·impl, di 등록 등 추가분 -->
```

**수정**
<!-- TODO -->

---

## 5. 모델 정의 가이드

```kotlin
// TODO: @Serializable DTO + 도메인 모델 + enum
```

Retrofit converter는 kotlinx-serialization을 쓰고 snake_case ↔ camelCase는 `@SerialName`으로 매핑한다. DTO(`core/network/`)와 도메인 모델(`core/services/protocols/`)을 분리한다.

---

## 6. ViewModel 시그니처

```kotlin
@HiltViewModel
class {{SCREEN_NAME_PASCAL}}ViewModel @Inject constructor(
    private val xxxService: XxxService,   // TODO: Service interface만, ≤5개
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<Nothing>>(LoadState.Idle)
    val state: StateFlow<LoadState<Nothing>> = _state.asStateFlow()   // TODO: 타입 확정

    // TODO: 액션 — fun / suspend fun 직접 노출 (enum Action 금지)
}
```

- 빈 결과는 `LoadState.Loaded(emptyList())`가 아니라 **`LoadState.Empty`**로 매핑(ARCHITECTURE.md §3.6)
- `viewModelScope`만 사용. `GlobalScope`·`Context`/`Activity` 주입 금지
- 신규 Service는 3-step(protocols `interface` → impl → `ServiceModule.kt` `@Binds`)

---

## 7. 외부 앱 / 시스템 연동

<!-- TODO: Intent/URL scheme, `<queries>` 매니페스트 선언, fallback. 없으면 섹션 삭제 가능 -->

---

## 8. 화면별 정밀 사양

<!-- TODO: 비표준 시각 컴포넌트만(예: 일몰 progress). 없으면 섹션 삭제 -->

---

## 9. 디자인 시스템 추가 — **에셋 입력 매트릭스 (Gate 4)**

> **이 두 매트릭스가 모두 채워진 다음에야 §10 Phase A를 시작한다.** 미채움 상태로 Phase A 진입 금지.

### 9.1 컬러 매트릭스

| 토큰명 | Figma node | hex (Light) | hex (Dark) | 용도 |
|---|---|---|---|---|
<!-- TODO -->

추가 위치: `common/designsystem/`의 `PickflowColors`.

### 9.2 아이콘/이미지 매트릭스

| 에셋명 | Figma node | export 포맷 | 밀도 | 용도 |
|---|---|---|---|---|
<!-- TODO -->

벡터는 `app/src/main/res/drawable/<name>.xml`(VectorDrawable), 래스터는 `drawable-xxhdpi/` 등 밀도별 디렉터리에 등록. Material 아이콘으로 대체 가능하면 에셋을 늘리지 않는다.

### 9.3 타이포 매핑 (사용한 토큰만)

| 사용처 | 토큰 | 폴백 |
|---|---|---|
<!-- TODO -->

> 매트릭스 채움 자가 점검:
> - [ ] §9.1, §9.2가 비어 있지 않다
> - [ ] 각 행이 실제 Figma 노드를 가리키고 hex/사이즈가 명시되어 있다
> - [ ] 누락된 토큰이 `<!-- TODO -->`가 아니라 실제 값으로 채워졌다

위 3개 모두 통과해야 Phase A 진입.

---

## 10. TDD A→B→C 오케스트레이션 (Gate 1)

> **A → B → C는 직렬이다. 단계 건너뛰기·병렬화·역순 모두 금지.**
> 각 단계의 진입/작업/종료 디테일은 리프 문서에서 봄. 이 섹션은 **순서와 게이트만** 명시한다.

```
§9 에셋 매트릭스 (Gate 4)
        ↓
Phase A — ViewModel TDD (Gate 1A)
  · 진입: §3, §6, §9 모두 확정
  · 작업: 인터랙션별 RED → GREEN, Composable 0줄
  · 종료: ViewModel 테스트 100% green, 화면 파일 0개
  · 가이드: docs/phases/phase-a-viewmodel-tdd.md ← Phase A 들어갈 때 읽기
        ↓
Phase B — ui-test-cases.md + Compose UI 테스트 (Gate 1B + Gate 2)
  · 진입: Phase A 종료 조건 통과
  · 작업: docs/{{TICKET}}/ui-test-cases.md 8컬럼 표 작성 → Robolectric UI 테스트
  · 종료: TODO 0개, 행마다 스냅샷 파일명 결정
  · 가이드: docs/phases/phase-b-ui-tdd.md ← Phase B 들어갈 때 읽기
        ↓
Phase C — Snapshot + UI (Gate 1C + Gate 3)
  · 진입: Phase B 종료 조건 통과
  · 작업: Paparazzi 케이스 RED → Composable → GREEN
  · 종료: 매트릭스 전 케이스 green, Figma 비교 루프 1회
  · 가이드: docs/phases/phase-c-snapshot.md ← Phase C 들어갈 때 읽기
```

> 각 Phase에 **들어갈 때** 해당 리프 문서를 read한다. 미리 다 읽어두지 않는다 — 단계 격리가 게이트의 본체다.

---

## 11. UI 검증 루프 (Figma 노드별 비교, Phase C 마무리)

| 컴포넌트 | Figma node-id | 확인 항목 |
|---|---|---|
<!-- TODO -->

각 노드 조회: Figma MCP `get_design_context` / `get_screenshot` (fileKey `{{FIGMA_FILE_KEY}}`). MCP 미연결이면 dev mode 값을 수기로 받아 비교한다.

---

## 12. 디버그 진입점

`feature/debug/`의 디버그 화면에 진입 버튼을 추가하거나, `app/navigation/`의 `NavHost`에 임시 Route를 등록해 시뮬레이터/에뮬레이터에서 바로 열 수 있게 한다.

```kotlin
composable("{{SCREEN_SLUG}}") {
    {{SCREEN_NAME_PASCAL}}Screen(viewModel = hiltViewModel(), onBack = { navController.popBackStack() })
}
```

---

## 13. 논의 포인트 MD

`docs/{{TICKET}}/{{SCREEN_SLUG}}-discussion.md` — 후속 합의 필요 항목. 결론이 필요하면 Jira {{TICKET}} 코멘트로 끌어올린다.
<!-- TODO: (a)/(b)/(c) 옵션 -->

---

## 14. 마감 체크리스트

각 Phase 리프 문서에 단계별 종료 조건이 있다. 여기서는 **PR 머지 직전 한 번 더 확인할 게이트만** 모은다.

**게이트 통과**
- [ ] Gate 1 (TDD A→B→C 직렬): 단계 순서 위반 없음
- [ ] Gate 2 (`ui-test-cases.md`): TODO 0개, 8컬럼 채움
- [ ] Gate 3 (Paparazzi): `./gradlew :app:verifyPaparazziDebug` green, `app/src/test/snapshots/` PNG PR 첨부, record 블라인드 덮어쓰기 0건
- [ ] Gate 4 (에셋 매트릭스): §9.1·§9.2 채움 후에 Phase A 시작했음

**일반**
- [ ] `./gradlew :app:assembleDebug` · `:app:testDebugUnitTest` green, 경고 0
- [ ] Phase A는 JUnit5 / Phase B·C는 JUnit4 — 혼용 없음
- [ ] §11 Figma 비교 루프 1회 이상
- [ ] §12 디버그 진입점에서 에뮬레이터 동작 확인
- [ ] 외부 앱 연동(있다면) 에뮬/실기기 검증
- [ ] `docs/{{TICKET}}/{{SCREEN_SLUG}}-discussion.md` 작성
- [ ] 커밋 `[{{TICKET}}] 한국어 요약` 형식, PR 제목·본문에 `{{TICKET}}` 명시

> 단계 내부 체크리스트(예: "Phase A 종료 조건")는 해당 리프 문서를 본다. 여기 중복으로 박지 않는다.

---

## 15. 작업 순서 요약

```
0. §0~§8 합의 (Jira {{TICKET}} 본문 기준) → §9 에셋 매트릭스 채움 (Gate 4)
        ↓
1. docs/phases/phase-a-viewmodel-tdd.md 읽기 → Phase A 수행 (Gate 1A)
        ↓
2. docs/phases/phase-b-ui-tdd.md 읽기 → Phase B 수행 (Gate 1B + 2)
        ↓
3. docs/phases/phase-c-snapshot.md 읽기 → Phase C 수행 (Gate 1C + 3) → §11 Figma 루프
        ↓
4. §12 디버그 검증 → §13 논의 포인트 → §14 통과 → PR
```

> 순서를 어겼다면 PR 본문에 어디서 거꾸로 갔는지 명시. 단계 건너뛰기는 회귀 비용으로 직결된다.
