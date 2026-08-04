# [PV-59] 무드 필터 확장(햇살/야경) — UI 테스트 케이스

> Phase B 산출물(Gate 2). 이 표가 **Phase C 스냅샷 매트릭스의 단일 진실 소스**다.
> 표에 없는 스냅샷은 찍지 않고, 표에 있는데 없는 스냅샷은 Gate 3 미통과다.

## 컬럼 정의

| 컬럼 | 의미 |
|---|---|
| ID | `PV59-<화면코드><번호>`. 스냅샷 파일명·테스트 함수명과 1:1 |
| 화면 | 대상 Composable |
| 시나리오 | 무엇을 확인하는지 한 줄 |
| 사전 상태 | ViewModel/Service 목이 만들어야 하는 상태 |
| 조작 | 사용자 입력(없으면 `—`) |
| 기대 결과 | 검증 대상. testTag 기준 |
| UI 테스트 | Phase B 테스트 파일 · 함수 (Robolectric/JUnit4) |
| 스냅샷 | Phase C Paparazzi 케이스명(`—` = 스냅샷 대상 아님) |

## 케이스 표

### 탐색 — 지도 무드 행 (`HomeMapScreen` / `homemap-moodfilter`)

| ID | 화면 | 시나리오 | 사전 상태 | 조작 | 기대 결과 | UI 테스트 | 스냅샷 |
|---|---|---|---|---|---|---|---|
| PV59-MAP1 | HomeMapScreen | 무드 캡슐 4개가 렌더된다 | `fetch` → 빈 페이지 | — | 햇살·윤슬·노을·야경 텍스트 4개 표시 | `HomeMapScreenUiTest.mood_filter_renders_four_moods_in_order` | — |
| PV59-MAP2 | HomeMapScreen | 표시 순서가 햇살→윤슬→노을→야경 | 동일 | — | `MoodFilter.entries` 순서와 화면 순서 일치 | 위와 동일 함수 | — |
| PV59-MAP3 | HomeMapScreen | 초기 진입 시 전부 미선택 | 동일 | — | `viewModel.selectedMoods` 가 빈 Set | `HomeMapScreenUiTest.mood_filter_starts_with_nothing_selected` | — |
| PV59-MAP4 | HomeMapScreen | 두 무드를 동시에 선택할 수 있다 | 동일 | 햇살 탭 → 야경 탭 | `selectedMoods == {Sunlight, Night}` | `HomeMapScreenUiTest.tapping_two_moods_selects_both` | — |
| PV59-MAP5 | HomeMapScreen | 선택된 캡슐 재탭 시 그것만 해제 | 햇살·야경 선택됨 | 햇살 탭 | `selectedMoods == {Night}` | `HomeMapScreenUiTest.retapping_a_selected_mood_clears_only_that_one` | — |
| PV59-MAP6 | MoodFilterRow | 미선택 상태 시각 | stateless 렌더 | — | 4개 캡슐 모두 보더 없음 | — | `moodfilter_none_selected_dark` |
| PV59-MAP7 | MoodFilterRow | 햇살+윤슬 선택 상태 시각 | stateless 렌더 | — | 2개 캡슐에 sunsetOrange 보더 | — | `moodfilter_sunlight_reflection_selected_dark` |
| PV59-MAP8 | MoodFilterRow | 전체 선택 상태 시각 | stateless 렌더 | — | 4개 캡슐 모두 보더 | — | `moodfilter_all_selected_dark` |

### 탐색 — 리스트 무드 행 (`SpotListScreen` / `spotlist-mood`)

| ID | 화면 | 시나리오 | 사전 상태 | 조작 | 기대 결과 | UI 테스트 | 스냅샷 |
|---|---|---|---|---|---|---|---|
| PV59-LST1 | SpotListScreen | 무드 캡슐 4개가 순서대로 렌더된다 | `fetch` → 스팟 1개 | — | 햇살·윤슬·노을·야경 표시 | `SpotListScreenUiTest.mood_filter_renders_four_moods_in_order` | — |
| PV59-LST2 | SpotListScreen | 초기 진입 시 전부 미선택 | 동일 | — | `viewModel.themes` 가 빈 Set | `SpotListScreenUiTest.mood_filter_starts_with_nothing_selected` | — |
| PV59-LST3 | SpotListScreen | 두 무드 동시 선택 → 도메인 테마 2개 | 동일 | 햇살 탭 → 야경 탭 | `themes == {SUNLIGHT, NIGHT}` | `SpotListScreenUiTest.tapping_two_moods_selects_both_themes` | — |
| PV59-LST4 | SpotListScreen | 전체 해제 시 필터 없음(빈 결과 아님) | 동일 | 햇살 탭 → 햇살 탭 | `themes` 빈 Set, 그리드 여전히 표시 | `SpotListScreenUiTest.clearing_all_moods_keeps_showing_results` | — |

### 스팟 등록 — 사진 카테고리 (`SpotRegistrationScreen`)

| ID | 화면 | 시나리오 | 사전 상태 | 조작 | 기대 결과 | UI 테스트 | 스냅샷 |
|---|---|---|---|---|---|---|---|
| PV59-REG1 | SpotRegistrationScreen | 카테고리 칩 4개가 순서대로 렌더된다 | 기본 | — | 햇살·윤슬·노을·야경 표시 | `SpotRegistrationScreenUiTest.theme_chips_render_four_themes_in_order` | — |
| PV59-REG2 | SpotRegistrationScreen | 초기값은 미선택 | 기본 | — | `viewModel.theme` 가 null | `SpotRegistrationScreenUiTest.theme_starts_unselected` | — |
| PV59-REG3 | SpotRegistrationScreen | **단독 선택** — 다른 칩을 고르면 이전 것이 해제 | 기본 | 햇살 탭 → 야경 탭 | `theme == NIGHT` (SUNLIGHT 해제) | `SpotRegistrationScreenUiTest.selecting_another_theme_replaces_the_previous_one` | — |
| PV59-REG4 | SpotRegistrationScreen | 같은 칩 재탭 시 해제 | 햇살 선택됨 | 햇살 탭 | `theme == null` | `SpotRegistrationScreenUiTest.retapping_the_selected_theme_clears_it` | — |

### 저장 탭 — 카드 셀 배지 (`ArchiveScreen` / `SpotListCell`)

| ID | 화면 | 시나리오 | 사전 상태 | 조작 | 기대 결과 | UI 테스트 | 스냅샷 |
|---|---|---|---|---|---|---|---|
| PV59-ARC1 | SpotListCell | 무드 4종 배지가 각각 렌더된다 | 각 무드 1개씩 stateless 렌더 | — | 라벨 햇살/윤슬/노을/야경 | `ArchiveScreenUiTest.saved_cards_render_all_four_mood_badges` | `spotlist_cell_moods_all_four_dark` |

## 커버리지 자가 점검

- [x] 무드 캡슐 4개가 햇살→윤슬→노을→야경 순으로 렌더된다 (MAP1/2, LST1)
- [x] 초기 진입 시 아무 무드도 선택돼 있지 않다 (MAP3, LST2, REG2)
- [x] 무드 2개 동시 선택 상태가 표현된다 (MAP4, LST3, MAP7)
- [x] 선택된 캡슐 재탭 시 그 하나만 해제된다 (MAP5)
- [x] 전체 해제 = 전체 표시(빈 결과 아님) (LST4)
- [x] 등록 폼 칩은 단독 선택이다 (REG3/4)
- [x] 저장 탭 카드 배지가 무드 4종 각각으로 렌더된다 (ARC1)
- [x] 모든 스냅샷 행에 파일명이 결정돼 있다 (MAP6/7/8, ARC1)
- [x] TODO 0개
