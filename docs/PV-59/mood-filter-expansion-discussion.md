# [PV-59] 무드 필터 확장 — 논의 포인트

구현 중 드러난, 이번 티켓 범위 밖이지만 결론이 필요한 항목. 결론이 서면 Jira PV-59 코멘트로 올린다.

---

## 1. 서버 enum 코드 미확정 (⚠️ 머지 전 확인 필요)

현재 `SUNLIGHT` / `NIGHT`로 잠정 구현했다. 서버 전송값은 `SpotTheme.name`이므로
**enum 이름이 곧 API 계약**이다. 백엔드가 다른 코드를 쓴다면 고칠 곳은 두 군데뿐이다.

| 파일 | 심볼 |
|---|---|
| `core/services/protocols/Spot.kt` | `enum class SpotTheme` |
| `core/network/mapper/SpotMapper.kt` | `parseTheme` |

`parseTheme`는 2글자 코드(`SL`/`NT`)와 풀네임을 모두 받도록 관용적으로 작성해 뒀다.
해당 지점에는 `// PV-59 백엔드 확정시 변경 가능성 있음` 주석이 달려 있다.

**다중 전달 형식**도 미확정이다. 지금은 Retrofit `List<String>` → `?theme=A&theme=B` 반복
파라미터로 보낸다. 서버가 CSV(`?theme=A,B`)를 원하면 `DefaultSpotListService.toQueryValues()`
한 곳만 고치면 된다.

- (a) 백엔드에 확인 후 필요시 수정 ← **권장**
- (b) 서버가 확정될 때까지 머지 보류

---

## 2. 중복 무드 enum 4개

같은 개념이 네 곳에 별도 타입으로 존재한다.

| 타입 | 위치 | 용도 |
|---|---|---|
| `SpotTheme` | `core/services/protocols/Spot.kt` | 도메인·API |
| `MoodFilter` | `feature/map/MoodFilter.kt` | 지도/리스트 필터 UI |
| `SpotListMood` | `feature/spotlist/components/SpotListModels.kt` | 카드 셀 배지 |
| `SpotDetailTheme` | `feature/spotdetail/components/SpotDetailModels.kt` | 상세 화면 |

무드 1종을 추가하는 데 네 곳을 고쳐야 했다. 이번에 `SpotTheme.iconRes()` / `label()`을
`MoodFilter`로 위임시켜 매핑 지점을 일부 줄였지만(`SpotListScreen`), 근본 중복은 남아 있다.

- (a) `SpotTheme` 하나로 수렴 + 나머지는 확장 프로퍼티로 대체 — 별도 티켓 ← **권장**
- (b) 이번 티켓에 포함 (스코프 초과)
- (c) 스냅샷 도메인 분리 의도이므로 그대로 둔다

---

## 3. 무드 캡슐 스펙이 Figma와 다르다

| 항목 | Figma (`Category` `1:43761`) | 현재 코드(지도) | 현재 코드(리스트) |
|---|---|---|---|
| 아이콘 | 20dp | 16dp | 20dp |
| 라벨 | `Body/large-bold` 17sp | `bodyMediumBold` 15sp | `bodyLargeBold` 17sp |
| 패딩 | 8×14 | 14×6 | 8×8 |
| 캡슐 간격 | 8px | 8dp | 12dp |

즉 **지도와 리스트의 무드 행이 서로 다르게 생겼고, 둘 다 Figma와도 어긋난다.**
"컴포넌트 변경 없음" 지시에 따라 이번엔 손대지 않았다.

- (a) 별도 티켓에서 Figma 기준으로 통일 ← **권장**
- (b) 이번에 맞춘다 (스냅샷 재record 필요)
- (c) Figma를 코드에 맞춰 수정

---

## 4. 무드 연타 시 요청 폭주

지도에서 무드를 토글할 때마다 viewport 요청이 즉시 나간다. 4개를 빠르게 켜면 4번 나간다.
리스트도 토글마다 `refresh()`가 돌지만, 세대(`loadGeneration`) 가드가 있어 stale 데이터가
섞이지는 않는다. 낭비되는 건 네트워크뿐이다.

- (a) 300ms 디바운스 도입 — 별도 티켓
- (b) 현행 유지 (무드 연타는 드문 조작)

---

## 5. 지도 ↔ 리스트 필터 상태 공유 ✅ 해결됨

~~두 ViewModel 이 선택을 각자 들고 있어 전환 시 필터가 풀렸다.~~

**(b) 공유 상태로 승격**을 채택해 해결했다.
`MoodFilterStore`(`core/services/protocols/`) + `InMemoryMoodFilterStore`(`@Singleton`)를 두고
두 ViewModel 이 같은 인스턴스를 주입받는다.

- 선택 상태의 소유권이 ViewModel 밖으로 나가면서 각자의 `MutableStateFlow` 가 사라졌다.
- 재조회는 각 ViewModel 이 `store.selected` 를 구독해 스스로 한다.
  `drop(1)` 로 최초 값은 건너뛰어 화면 진입 시 중복 요청을 막는다.
- 토글 함수는 이제 `store.toggle()` 위임 한 줄이다.

> ⚠️ `HomeMapViewModel` 의존성이 7개 → **8개**가 됐다. ARCHITECTURE 규약(≤5)을
> 이미 넘고 있던 상태라 이번에 더 벌어졌다. 지도 화면 자체의 책임 분리가 필요하다 —
> 별도 티켓 대상.

---

## 6. 기존 Paparazzi 스냅샷 168장 실패 (PV-59와 무관)

`./gradlew :app:verifyPaparazziDebug`가 **이 브랜치의 변경 이전부터** 168장 실패한다.
`git stash`로 클린 트리를 만들어 확인했다.

| 트리 상태 | 전체 | 실패 |
|---|---|---|
| 클린(HEAD) | 395 | **168** |
| PV-59 적용 후 | 424 | **168** |

실패 그룹에 로그인·온보딩·마이프로필처럼 이번 변경과 무관한 화면이 포함돼 있고,
diff가 1~2% 수준의 미세 픽셀 차이라 렌더 환경(폰트/layoutlib 버전) 드리프트로 보인다.

**블라인드 record는 하지 않았다.** PV-59 신규 4장만 `--tests` 필터로 record 했고,
기존 168장은 그대로 뒀다.

- (a) 별도 티켓에서 원인 규명 후 일괄 재record ← **권장**
- (b) 이번 PR에서 같이 재record (회귀를 정상화시킬 위험)

---

## 7. Phase A에서 Composable을 건드렸다 (게이트 규약 이탈)

`docs/phases/phase-a-viewmodel-tdd.md`의 Phase A 종료 조건은 "Composable 파일 diff 0"이다.
그러나 `HomeMapViewModel.selectedMood: MoodFilter?` → `selectedMoods: Set<MoodFilter>`처럼
**ViewModel의 공개 타입이 바뀌면 호출부 Composable이 컴파일되지 않는다.**
Kotlin에서는 이 배선을 미룰 방법이 없다.

Phase A에서 한 Composable 변경은 배선 한정이다:
- `collectAsStateWithLifecycle` 대상 프로퍼티명·타입
- `MoodFilterRow(selected: MoodFilter?)` → `Set<MoodFilter>`, `selected == mood` → `mood in selected`
- exhaustive `when` 분기 추가(비-Composable 매핑 함수)

레이아웃·색·타이포는 Phase C까지 손대지 않았다.

- (a) 게이트 문서에 "공개 ViewModel 타입 변경 시 호출부 배선은 Phase A 허용" 예외를 명문화 ← **권장**
- (b) 그대로 두고 PR마다 개별 설명
