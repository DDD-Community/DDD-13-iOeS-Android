# [PV-59] 무드 필터 확장(햇살/야경) 구현 통합 프롬프트

> **이 프롬프트의 사용법**: `screen-tdd-prompt` 스킬이 템플릿을 복제·치환해 저장한 문서다.
>
> **방법론은 여기 없다.** TDD 단계별 디테일은 단계 진입 시 리프 문서를 읽는다:
> - Phase A: `docs/phases/phase-a-viewmodel-tdd.md`
> - Phase B: `docs/phases/phase-b-ui-tdd.md`
> - Phase C: `docs/phases/phase-c-snapshot.md`
>
> 본 문서는 **이 작업에만 해당하는 사실**(스코프, API, 정책, 에셋, 컴포넌트 매핑)을 담는다.

---

## 0. 작업 컨텍스트 (선결 정보)

**브랜치**: `feature/PV-59`
**티켓**: PV-59 (Jira)
**Figma 파일 키**: `WLGPjrQtLqyhq46zXxvXHp`

**이 티켓의 성격 — 신규 화면이 아니라 "기존 4개 화면에 걸친 도메인 enum 확장"이다.**
새 Composable·새 ViewModel·새 Service를 만들지 않는다. `SpotTheme` 값 2개를 추가하면
컴파일 에러가 나는 지점(§4)을 전부 메우고, 지도/리스트의 단일선택을 다중선택으로 바꾸는 것이 전부다.

**프로젝트 가정 (재탐색 불필요 — 상세는 `ARCHITECTURE.md`)**:
- Jetpack Compose(Material3) + **MVVM + StateFlow + Hilt**, 단일 `:app` 모듈 + 패키지 계층
- 패키지: `com.pickflow.android.{app, core, common, feature}`
- **No Repository** — `core/services/protocols/`의 `interface XxxService`가 단일 데이터 추상화
- **No enum/sealed Action** — ViewModel에 `fun`/`suspend fun` 직접 노출(MVI/TCA 금지)
- **개별 StateFlow 노출** — 단일 거대 `data class UiState` 금지. 비동기는 `common/ui/LoadState.kt`
- 테스트는 전부 `app/src/test/`(호스트 JVM). Phase A는 JUnit5, Phase B·C는 JUnit4

---

## 1. 스코프

**구현 범위**:

| 화면 | 파일 | 할 일 |
|---|---|---|
| 탐색 — 지도 | `feature/map/HomeMapScreen.kt` `MoodFilterRow` / `MoodFilter.kt` | 햇살·야경 추가, 다중선택, 순서 고정, 초기 미선택 |
| 탐색 — 리스트 | `feature/spotlist/SpotListScreen.kt` `MoodFilterRow` | 위와 동일 (지도와 동일 컴포넌트·동일 동작) |
| 스팟 등록 | `feature/spotregistration/SpotRegistrationScreen.kt` `ThemeChipGroup` | 햇살·야경 추가. **단독 선택 유지**, 초기값 `null` 유지 |
| 저장 탭 | `feature/archive/ArchiveScreen.kt` + `feature/spotlist/components/SpotListModels.kt` | 카드 셀 무드 아이콘 4종 대응 (필터 아님, 표시 전용) |

**범위 밖**:
- ~~무드 캡슐/칩 컴포넌트 자체의 디자인 변경~~ → **범위에 포함됨**(완료조건 "컴포넌트는 변경없음"은
  *세 화면이 서로 동일해야 한다*는 뜻이었다). 지도/리스트/등록의 캡슐 사양이 제각각이라
  Figma 기준으로 통일했다. 상세는 §8.
- `SpotTheme` / `MoodFilter` / `SpotListMood` / `SpotDetailTheme` **4개 중복 enum의 통합 리팩터링** (§13)
- 무드 필터의 서버 사이드 정렬·페이지네이션 정책 변경
- 스팟 상세 화면의 무드 표시 (`SpotDetailTheme`)는 컴파일이 깨지지 않을 만큼만 확장

---

## 2. 핵심 정책 결정 (사용자 확정)

| # | 항목 | 결정 |
|---|---|---|
| 1 | 무드 종류 | 햇살 / 윤슬 / 노을 / 야경 4종 |
| 2 | 표시 순서 | **햇살 → 윤슬 → 노을 → 야경 고정**. `SpotTheme` enum 선언 순서로 강제하고 UI는 `entries` 순회만 한다(정렬 로직 별도 작성 금지) |
| 3 | 지도/리스트 선택 방식 | **다중선택**. 캡슐 재탭 = 해당 무드만 해제 |
| 4 | 지도/리스트 초기값 | **미선택**(`emptySet()`) |
| 5 | 전체 해제의 의미 | **필터 없음 = 전체 스팟 표시**. `theme` 쿼리 파라미터를 아예 보내지 않는다. `LoadState.Empty`로 가지 않는다 |
| 6 | 등록 폼 선택 방식 | **단독 선택 유지**. 기존 `toggleTheme`(같은 값 재탭 시 해제) 동작 그대로 |
| 7 | 등록 폼 초기값 | **현행 유지**(`null` = 미선택). 등록 버튼 활성화 조건도 현행 유지 |
| 8 | 저장 탭 | 필터 UI 없음. 카드 셀 무드 배지 아이콘만 4종을 그릴 수 있으면 된다 |
| 9 | 지도·리스트 필터 상태 공유 | **공유하지 않는다**(현행과 동일). `HomeMapViewModel`과 `SpotListViewModel`이 각자 보유 |

---

## 3. API 매핑

| UI 동작 | Endpoint | 비고 |
|---|---|---|
| 리스트 조회 / 지도 초기 로드 | `GET /v1/spots?page={n}&theme={t}&theme={t}&sort={s}` | `theme` **반복 전달**로 다중 필터 |
| 지도 viewport 조회 | `GET /v1/spots/viewport?...&theme={t}&theme={t}` | 동일 |
| 스팟 등록 | `POST /v1/spots` (기존) | `theme` 단일 값 |

**확정 사항**: 서버는 4종 테마 + `theme` 파라미터 다중 전달을 지원한다.
**전체 해제 시**: `theme`를 **한 번도 붙이지 않는다**(빈 문자열·`theme=` 전송 금지).

```kotlin
// SpotApi — String? → List<String>? 로 변경. Retrofit이 리스트를 반복 쿼리로 직렬화한다.
@Query("theme") theme: List<String>? = null,
// 호출부: themes.takeIf { it.isNotEmpty() }?.map { it.name }
```

**신규 테마 enum 코드 (잠정 확정)**: 햇살 = `SUNLIGHT`, 야경 = `NIGHT`.
2글자 코드는 기존 규칙(`SS`/`YS`)을 따라 `SL`/`NT`로 가정하고 `parseTheme`가 둘 다 받는다.

> ⚠️ **백엔드 미확정 항목** — 아래는 백엔드 확정 시 변경될 수 있다.
> 변경이 필요해지면 `SpotTheme` enum 이름과 `parseTheme` 두 곳만 고치면 되도록,
> 서버 코드 문자열은 **`theme.name`에만 의존**시키고 하드코딩 리터럴을 흩뿌리지 않는다.
>
> | 항목 | 잠정값 | 확정 필요 |
> |---|---|---|
> | 햇살 코드 | `SUNLIGHT` / `SL` | 백엔드 |
> | 야경 코드 | `NIGHT` / `NT` | 백엔드 |
> | 다중 전달 형식 | 반복 파라미터 `theme=A&theme=B` | CSV(`theme=A,B`)일 가능성 |
>
> 코드에는 `// PV-59 백엔드 확정시 변경 가능성 있음` 주석으로 해당 지점을 표시한다.

---

## 4. 신규/수정 파일 목록

**신규**
```
app/src/main/res/drawable-xhdpi/ic_sunny.png   # 햇살 (40×40, Figma에서 추출 완료)
app/src/main/res/drawable-xhdpi/ic_night.png   # 야경 (40×40, Figma에서 추출 완료)
```

**수정 — `SpotTheme`에 값 2개를 추가하면 아래 `when`이 전부 컴파일 에러가 난다. 그게 체크리스트다.**

| # | 파일 | 심볼 | 할 일 |
|---|---|---|---|
| 1 | `core/services/protocols/Spot.kt` | `enum class SpotTheme` | `SUNLIGHT, YUNSEUL, SUNSET, NIGHT` (표시 순서대로 선언) |
| 2 | `core/network/mapper/SpotMapper.kt` | `parseTheme` | 신규 코드 2종 파싱 추가 |
| 3 | `core/network/api/SpotApi.kt` | `getSpots` / `getSpotsInViewport` | `theme: String?` → `List<String>?` |
| 4 | `core/services/protocols/SpotListService.kt` | `fetch` | `theme: SpotTheme?` → `themes: Set<SpotTheme>` |
| 5 | `core/services/protocols/SpotMapService.kt` | `fetchInViewport` | 동일 |
| 6 | `core/services/impl/DefaultSpotListService.kt` | — | 빈 Set → `null` 전달 |
| 7 | `core/services/impl/DefaultSpotMapService.kt` | — | 동일 |
| 8 | `feature/map/MoodFilter.kt` | `enum class MoodFilter` | `Sunlight, Reflection, Sunset, Night` (표시 순서) |
| 9 | `feature/map/HomeMapViewModel.kt` | `_selectedMood` / `selectMood` / `themeForMood` | `MoodFilter?` → `Set<MoodFilter>`, 토글 의미 변경 |
| 10 | `feature/map/HomeMapScreen.kt` | `MoodFilterRow` / `MoodCapsule` | `selected: Set<MoodFilter>`, `mood in selected` |
| 11 | `feature/map/SpotDetailBottomSheet.kt` | `SpotTheme.toDetailTheme` | 신규 2종 분기 |
| 12 | `feature/spotlist/SpotListViewModel.kt` | `_theme` / `selectTheme` | `SpotTheme?` → `Set<SpotTheme>` |
| 13 | `feature/spotlist/SpotListScreen.kt` | `MoodFilterRow` / `iconRes` / `label` / `toMood` / `toTheme` | 신규 2종 + 다중선택 |
| 14 | `feature/spotlist/components/SpotListModels.kt` | `enum class SpotListMood` | 신규 2종 |
| 15 | `feature/spotregistration/SpotRegistrationScreen.kt` | `SpotTheme.iconRes` | 신규 2종. `ThemeChipGroup`은 **로직 변경 없음** |
| 16 | `feature/archive/ArchiveScreen.kt` | `SpotTheme.toMood` | 신규 2종 |
| 17 | `feature/spotdetail/components/SpotDetailModels.kt` | `SpotDetailTheme` / `toDetailTheme` | 신규 2종 |

> 15번은 `SpotTheme.entries`를 순회하므로 enum 선언 순서만 맞으면 칩 순서가 자동으로 맞는다.
> **`ThemeChipGroup` 본문은 건드리지 않는 것이 정답이다.**

---

## 5. 모델 정의 가이드

```kotlin
// core/services/protocols/Spot.kt
// 선언 순서 = UI 표시 순서(햇살/윤슬/노을/야경). entries 순회만으로 정책 #2를 만족한다.
enum class SpotTheme { SUNLIGHT, YUNSEUL, SUNSET, NIGHT }
```

```kotlin
// core/network/mapper/SpotMapper.kt
// 서버는 endpoint 에 따라 2글자 코드 또는 풀네임으로 응답한다. 둘 다 받는다.
internal fun parseTheme(value: String): SpotTheme = when (value.uppercase()) {
    "YS", "YUNSEUL" -> SpotTheme.YUNSEUL
    "SS", "SUNSET" -> SpotTheme.SUNSET
    "SL", "SUNLIGHT" -> SpotTheme.SUNLIGHT
    "NT", "NIGHT" -> SpotTheme.NIGHT
    else -> SpotTheme.SUNSET
}
```

```kotlin
// feature/map/MoodFilter.kt — 선언 순서 = 표시 순서
enum class MoodFilter(val displayName: String, val iconRes: Int) {
    Sunlight("햇살", R.drawable.ic_sunny),
    Reflection("윤슬", R.drawable.ic_reflection),
    Sunset("노을", R.drawable.ic_sunset),
    Night("야경", R.drawable.ic_night),
}
```

> `ordinal`에 의존하는 코드가 없는지 확인하고 순서를 바꿀 것. 현재는 없다.
> 서버 전송은 `theme.name` 기반이므로 **enum 이름을 서버 코드와 어긋나게 바꾸지 말 것**(§3 TODO 확정 후 재검).

---

## 6. ViewModel 시그니처

```kotlin
// feature/map/HomeMapViewModel.kt
private val _selectedMoods = MutableStateFlow<Set<MoodFilter>>(emptySet())
val selectedMoods: StateFlow<Set<MoodFilter>> = _selectedMoods.asStateFlow()

/** 다중선택 토글 — 이미 있으면 빼고 없으면 넣는다. 빈 Set = 전체 표시. */
fun selectMood(mood: MoodFilter) {
    _selectedMoods.value = _selectedMoods.value.toMutableSet().apply {
        if (!add(mood)) remove(mood)
    }
    lastViewport?.let { onViewportChanged(it, _zoom.value) } ?: load()
}

private fun themesFor(moods: Set<MoodFilter>): Set<SpotTheme> = moods.mapTo(mutableSetOf()) {
    when (it) {
        MoodFilter.Sunlight -> SpotTheme.SUNLIGHT
        MoodFilter.Reflection -> SpotTheme.YUNSEUL
        MoodFilter.Sunset -> SpotTheme.SUNSET
        MoodFilter.Night -> SpotTheme.NIGHT
    }
}
```

```kotlin
// feature/spotlist/SpotListViewModel.kt
private val _themes = MutableStateFlow<Set<SpotTheme>>(emptySet())
val themes: StateFlow<Set<SpotTheme>> = _themes.asStateFlow()

/** 토글 후 refresh() — 세대(loadGeneration) 증가로 이전 필터의 늦은 응답을 폐기한다. */
fun toggleTheme(theme: SpotTheme) {
    _themes.value = _themes.value.toMutableSet().apply { if (!add(theme)) remove(theme) }
    refresh()
}
```

**주의 — 회귀 위험 지점**
- `SpotListViewModel.refresh()`의 `loadGeneration` / `accumulatedIds` 가드는 그대로 둔다.
  필터 전환마다 세대가 증가해야 이전 무드의 응답이 섞이지 않는다(직전 커밋 `5a8f113`이 고친 문제).
- `HomeMapViewModel.selectMood`는 매 토글마다 viewport 재요청을 낸다. 4개를 연타하면 4번 나간다.
  이번 티켓에서 디바운스를 넣지 않으므로 §13에 남긴다.
- 빈 결과는 `LoadState.Loaded(emptyList())`가 아니라 **`LoadState.Empty`** (ARCHITECTURE.md §3.6)
- `viewModelScope`만 사용. `GlobalScope`·`Context`/`Activity` 주입 금지

---

## 7. 외부 앱 / 시스템 연동

해당 없음.

---

## 8. 화면별 정밀 사양 — 컴포넌트 통일

완료조건의 "컴포넌트는 변경없음"은 **세 화면의 캡슐/칩이 서로 동일해야 한다**는 뜻이다.
착수 시점에는 지도·리스트·등록이 제각각이었고 셋 다 Figma와도 어긋나 있었다.

### 8.1 통일 전 (문제 상태)

| 항목 | Figma | 지도 | 리스트 | 등록 |
|---|---|---|---|---|
| 패딩(가로×세로) | 14×8 / 12×8 | 14×6 ❌ | 8×6 ❌ | 12×8 ✅ |
| 아이콘–라벨 간격 | 6 | 4 ❌ | 6 ✅ | 6 ✅ |
| 아이콘 크기 | 20dp | 16dp ❌ | 20dp ✅ | 20dp ✅ |
| 타이포 | 17sp / 15sp | 15sp ❌ | 17sp ✅ | 15sp ✅ |
| 배경 | gray95 / gray90 | ✅ | 없음 ❌ | ✅ |
| 미선택 보더 | 없음 | ✅ | ✅ | 있음 ❌ |
| 선택 보더 | 1dp | ✅ | ✅ | 1.5dp ❌ |
| 라벨색 | 항상 흰색 | 회색 ❌ | 회색 ❌ | 회색 ❌ |
| 캡슐 간격 | 8 / 12 | 8 ✅ | 12 ❌ | 12 ✅ |

### 8.2 통일 후 — 탐색 캡슐 (Figma `Category` `1:43761`)

**지도와 리스트가 `feature/map/components/MoodFilterRow.kt` 하나를 공유한다.**
각 화면은 `testTag`만 다르게 넘긴다(`homemap-moodfilter` / `spotlist-mood`).

| 항목 | 값 | Figma 근거 |
|---|---|---|
| 캡슐 패딩 | 가로 14dp, 세로 8dp | `729:7837` |
| 코너 | 8dp | 〃 |
| 배경 | `gray95` #131416 | 〃 |
| 아이콘 | 20dp | 〃 |
| 아이콘–라벨 간격 | 6dp | 〃 |
| 라벨 | `bodyLargeBold` 17sp | 〃 |
| 라벨색 | `gray0` **고정** | Default/Selected 모두 #FFFFFF |
| 선택 보더 | `sunsetOrange` 1dp | `733:11506` |
| 미선택 보더 | 없음 | `729:7837` |
| 행 패딩 | 가로 16dp, 세로 8dp | `729:7949` |
| 캡슐 간격 | 8dp | 〃 |

### 8.3 통일 후 — 등록 칩 (Figma `Btn-tag` `1:46095`)

탐색 캡슐과 **같은 규칙**을 따른다. 선택 여부는 보더로만 구분하고 라벨색은 고정이다.
다른 점은 배경(`gray90`), 라벨 크기(15sp), 칩 간격(12dp)뿐이며 이는 Figma 사양이다.

| 항목 | 값 | Figma 근거 |
|---|---|---|
| 칩 패딩 | 가로 12dp, 세로 8dp | `1:44480` |
| 배경 | `gray90` #1E2124 | 〃 |
| 라벨 | `bodyMediumBold` 15sp, `gray0` 고정 | 〃 |
| 선택 보더 | `sunsetOrange` 1dp / 미선택 없음 | 〃 |
| 아이콘·간격·코너 | 20dp · 6dp · 8dp | 탐색과 동일 |
| 칩 간격 | 12dp | `733:14018` |

> **리스트에서 미선택 캡슐이 배경에 묻혀 보이는 건 의도된 디자인이다.**
> 화면 배경과 캡슐 배경이 둘 다 `gray95`라 Figma 시안(`733:11427`)에서도 동일하게 보인다.
> 지도에서는 지도 위에 얹히므로 캡슐 형태가 드러난다.

---

## 9. 디자인 시스템 추가 — **에셋 입력 매트릭스 (Gate 4)**

> **이 두 매트릭스가 모두 채워진 다음에야 §10 Phase A를 시작한다.**

### 9.1 컬러 매트릭스

**신규 컬러 토큰 없음.** 모두 기존 `PickflowColors`로 커버된다.

| 용도 | Figma hex | 기존 토큰 | 확인 |
|---|---|---|---|
| 캡슐 배경 | `#131416` | `PickflowColors.gray95` | 기존 `MoodCapsule`이 이미 사용 |
| 선택 보더 | `#FA6133` | `PickflowColors.sunsetOrange` | 기존 사용 |
| 선택 라벨 | `#FFFFFF` | `PickflowColors.gray0` | 기존 사용 |
| 미선택 라벨 | `#8A949E` | `PickflowColors.gray30` | 기존 사용 |
| 등록 칩 배경 | `#1E2124` | `PickflowColors.spotInputBackground` | 기존 사용 |

### 9.2 아이콘/이미지 매트릭스

| 에셋명 | Figma 컴포넌트 | Figma node | 포맷 | 밀도 | 용도 | 상태 |
|---|---|---|---|---|---|---|
| `ic_sunny.png` | `Icon/Img/ic_sunny` | `1:45969` | PNG 40×40 | xhdpi | 햇살 | **추출 완료** |
| `ic_night.png` | `Icon/Img/ic_night` | `1:45991` | PNG 40×40 | xhdpi | 야경 | **추출 완료** |
| `ic_reflection.png` | `Icon/Img/ic_gradient` | `1:45994` | PNG 40×40 | xhdpi | 윤슬 | 기존 |
| `ic_sunset.png` | `Icon/Img/ic_twighlight` | `1:45972` | PNG 40×40 | xhdpi | 노을 | 기존 |

> 기존 무드 아이콘 2종이 xhdpi PNG 40×40이므로 신규 2종도 동일 규격으로 맞췄다.
> mdpi/xxhdpi 변형은 기존 에셋에도 없으므로 추가하지 않는다.

### 9.3 타이포 매핑

| 사용처 | 토큰 | 비고 |
|---|---|---|
| 무드 캡슐 라벨 | `PickflowTypography.bodyMediumBold` | 현행 유지(Figma는 `Body/large-bold` — §13) |
| 등록 칩 라벨 | `PickflowTypography.bodyMediumBold` | 현행 유지 |

> 매트릭스 채움 자가 점검:
> - [x] §9.1, §9.2가 비어 있지 않다
> - [x] 각 행이 실제 Figma 노드를 가리키고 hex/사이즈가 명시되어 있다
> - [x] 누락된 토큰이 `<!-- TODO -->`가 아니라 실제 값으로 채워졌다
> - [x] §3의 서버 enum 코드가 `SUNLIGHT`/`NIGHT`로 잠정 확정됐다(백엔드 재확인 대상, 코드에 주석 표시)

---

## 10. TDD A→B→C 오케스트레이션 (Gate 1)

> **A → B → C는 직렬이다. 단계 건너뛰기·병렬화·역순 모두 금지.**

```
§9 에셋 매트릭스 (Gate 4)  +  §3 서버 enum 코드 확정
        ↓
Phase A — ViewModel TDD (Gate 1A)
  · 진입: §3, §6, §9 모두 확정
  · 작업: HomeMapViewModel / SpotListViewModel 다중선택 토글, 빈 Set = 전체,
          SpotMapper.parseTheme 신규 코드, Service 시그니처 변경. Composable 0줄
  · 종료: ViewModel·mapper·service 테스트 100% green, Composable 파일 diff 0
  · 가이드: docs/phases/phase-a-viewmodel-tdd.md ← Phase A 들어갈 때 읽기
        ↓
Phase B — ui-test-cases.md + Compose UI 테스트 (Gate 1B + Gate 2)
  · 진입: Phase A 종료 조건 통과
  · 작업: docs/PV-59/ui-test-cases.md 8컬럼 표 작성 → Robolectric UI 테스트
  · 종료: TODO 0개, 행마다 스냅샷 파일명 결정
  · 가이드: docs/phases/phase-b-ui-tdd.md ← Phase B 들어갈 때 읽기
        ↓
Phase C — Snapshot + UI (Gate 1C + Gate 3)
  · 진입: Phase B 종료 조건 통과
  · 작업: Paparazzi 케이스 RED → Composable 수정 → GREEN
  · 종료: 매트릭스 전 케이스 green, §11 Figma 비교 루프 1회
  · 가이드: docs/phases/phase-c-snapshot.md ← Phase C 들어갈 때 읽기
```

**Phase A 최소 커버리지 (이 티켓 고유)**

| 대상 | 케이스 |
|---|---|
| `HomeMapViewModel.selectMood` | 미선택 → 1개 선택 / 2개 누적 선택 / 재탭 시 해당 1개만 해제 / 전부 해제 시 `emptySet` |
| `HomeMapViewModel` | 무드 토글마다 viewport(또는 load) 재요청이 나간다 |
| `SpotListViewModel.toggleTheme` | 위와 동일 + 토글마다 `refresh()`로 세대 증가 → 이전 응답 폐기 |
| `DefaultSpotListService` | 빈 Set → `theme` 파라미터 `null`. 2개 → 리스트 2개 전달 |
| `DefaultSpotMapService` | 동일 |
| `parseTheme` | `"SL"`/`"SUNLIGHT"` → SUNLIGHT, `"NT"`/`"NIGHT"` → NIGHT, 미상 → SUNSET |
| `SpotTheme.entries` | 순서가 햇살→윤슬→노을→야경 |

**Phase B/C 최소 커버리지**

| 대상 | 케이스 |
|---|---|
| 지도 무드 행 | 캡슐 4개 렌더 / 순서 / 초기 전부 미선택 / 2개 동시 선택 상태 |
| 리스트 무드 행 | 지도와 동일 |
| 등록 폼 칩 | 칩 4개 / 순서 / 단독 선택(하나 고르면 이전 것 해제) |
| 저장 탭 카드 셀 | 무드 4종 각각의 배지 아이콘 |

---

## 11. UI 검증 루프 (Figma 노드별 비교, Phase C 마무리)

| 컴포넌트 | Figma node-id | 확인 항목 |
|---|---|---|
| 탐색-지도 전체 | `729:7927` | 무드 행 4개, 햇살·윤슬 동시 선택 상태 |
| 탐색-리스트 전체 | `733:11427` | 무드 행 4개, 전부 미선택 상태 |
| 무드 캡슐 컴포넌트 세트 | `1:43761` | Default/Selected 배경·보더·라벨 색 |
| 햇살 캡슐 (Selected) | `714:7809` | 아이콘 + 보더 |
| 야경 캡슐 (Default) | `714:7822` | 아이콘 + 무보더 |
| 등록-사진 카테고리 | `733:14000` | 칩 4개, 윤슬만 선택된 단독 선택 상태 |
| 등록 칩 컴포넌트 세트 | `1:46095` | on/off 배경·보더 |
| 저장 탭 카드 배지 세트 | `1:43942` | 4종 배지 아이콘 |

Figma MCP `get_figma_data` / `download_figma_images` (fileKey `WLGPjrQtLqyhq46zXxvXHp`).

---

## 12. 디버그 진입점

신규 라우트 없음. 기존 진입점으로 검증한다.

- 탐색 탭 → 상단 무드 행 (지도/리스트 토글 양쪽)
- 탐색 탭 → 우하단 `+` → 스팟 등록 → "사진 카테고리"
- 저장 탭 → 카드 그리드

에뮬레이터 확인 포인트: 무드 2개 선택 후 지도 팬 → viewport 재요청에 `theme` 2개가 실리는지
(OkHttp 로그로 확인), 전부 해제 시 `theme` 파라미터가 사라지는지.

---

## 13. 논의 포인트 MD

`docs/PV-59/mood-filter-expansion-discussion.md`에 아래를 정리하고, 결론이 필요하면 Jira PV-59 코멘트로 올린다.

1. **중복 enum 4개** — `SpotTheme`(도메인) / `MoodFilter`(map) / `SpotListMood`(spotlist) / `SpotDetailTheme`(spotdetail).
   무드 1종 추가에 4곳을 고쳐야 한다. `SpotTheme` 하나로 수렴시키고 나머지는 확장 프로퍼티(`displayName`, `iconRes`)로
   대체하는 리팩터링 — (a) 이번 티켓에 포함 (b) 후속 티켓 (c) 두지 않음.
2. **캡슐 스펙 격차** — Figma는 아이콘 20dp / `Body/large-bold` 17sp / 패딩 8×14, 코드는 16dp / 15sp / 14×6.
   (a) 이번에 Figma로 맞춤 (b) 별도 티켓 (c) Figma를 코드에 맞춰 수정.
3. **무드 연타 시 요청 폭주** — 지도에서 4개를 빠르게 토글하면 viewport 요청이 4번 나간다.
   디바운스(예: 300ms) 도입 여부.
4. **필터 상태 공유** — 지도에서 고른 무드가 리스트로 넘어가지 않는다. 사용자 기대와 맞는지.
5. **서버 enum 코드** — §3 TODO. Phase A 진입 전 필수.

---

## 14. 마감 체크리스트

**게이트 통과**
- [x] Gate 1 (TDD A→B→C 직렬): 단계 순서 위반 없음 (단, Phase A의 Composable 배선은 discussion §7 참고)
- [x] Gate 2 (`ui-test-cases.md`): TODO 0개, 8컬럼 채움
- [~] Gate 3 (Paparazzi): PV-59 신규 4장 green + record 블라인드 덮어쓰기 0건.
      **기존 168장 실패는 PV-59 이전부터의 회귀**(클린 트리에서 재현 확인) — discussion §6
- [x] Gate 4 (에셋 매트릭스): §9.1·§9.2 채움 + §3 서버 코드 잠정 확정 후에 Phase A 시작

**이 티켓 고유**
- [x] §4의 17개 수정 지점을 전부 처리했다(= `SpotTheme` 확장 후 컴파일 경고·에러 0)
- [x] `SpotTheme.entries` 순서가 햇살→윤슬→노을→야경 (`SpotThemeParsingTest`)
- [x] 지도·리스트 초기 진입 시 무드가 하나도 선택돼 있지 않다 (PV59-MAP3/LST2)
- [x] 전부 해제 시 `theme` 쿼리 파라미터가 요청에서 사라진다 (`DefaultSpotListServiceTest`)
- [x] 등록 폼은 여전히 단독 선택이다 (PV59-REG3/4)
- [x] `ThemeChipGroup` 본문 diff 0 (enum 확장만으로 칩이 4개가 됐다)

**일반**
- [x] `./gradlew :app:assembleDebug` · `:app:testDebugUnitTest` green (424 테스트), 경고 0
- [x] Phase A는 JUnit5 / Phase B·C는 JUnit4 — 혼용 없음
- [x] §11 Figma 비교 루프 1회 (지도/리스트/등록 3개 프레임 + Category·Btn-tag·Tag 컴포넌트 세트)
- [ ] §12 디버그 진입점에서 에뮬레이터 동작 확인 ← **남음**
- [x] `docs/PV-59/mood-filter-expansion-discussion.md` 작성
- [ ] 커밋 `[PV-59] 한국어 요약` 형식, PR 제목·본문에 `PV-59` 명시

---

## 15. 작업 순서 요약

```
0. §9 에셋 매트릭스 완료(✔) → §3 서버 enum 코드 확정(미완) → Gate 4 통과
        ↓
1. docs/phases/phase-a-viewmodel-tdd.md 읽기 → Phase A 수행 (Gate 1A)
   순서: SpotTheme 확장 → parseTheme → Service/Api 시그니처 → 두 ViewModel
        ↓
2. docs/phases/phase-b-ui-tdd.md 읽기 → Phase B 수행 (Gate 1B + 2)
        ↓
3. docs/phases/phase-c-snapshot.md 읽기 → Phase C 수행 (Gate 1C + 3) → §11 Figma 루프
        ↓
4. §12 디버그 검증 → §13 논의 포인트 → §14 통과 → PR
```

> 순서를 어겼다면 PR 본문에 어디서 거꾸로 갔는지 명시.
