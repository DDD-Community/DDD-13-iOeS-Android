# PV-79 — 신규 기능 안내 확인 시나리오

Firebase Remote Config `new_feature_flags` 로 노출을 제어하는 기능의 QA 시나리오.
계약(키·스키마·판정 규칙)은 iOS(PV-80)와 공유하므로 여기서 임의로 바꾸지 않는다.

## 0. 확인 대상은 3개 중 2개다

| feature 키 | UI | 상태 |
|---|---|---|
| `v2_update_modal` | 탐색 탭 전체화면 팝업 (`V2NoticePopup`) | ✅ 확인 가능 |
| `home_new_badge` | 무드 필터 칩 햇살·야경의 주황 dot | ✅ 확인 가능 |
| `spot_open_guide` | 보관함 > 나만의 스팟 바텀시트 | ❌ **미구현 — 확인할 화면이 없다** |

`spot_open_guide` 는 안드로이드에 대응 바텀시트가 없어 **키만 예약**했다. 화면이 생기기 전까지
이 키를 켜도 앱에서 일어나는 일은 없다(§4 C3 에서 그것만 확인한다).

## 1. 전제

- **DEBUG 빌드로만 확인한다.** `minimumFetchIntervalInSeconds` 가 DEBUG 0 / Release 3600 이라
  release 는 값을 바꿔도 최대 1시간 뒤에 반영된다.
- Console → Remote Config → `new_feature_flags` 편집 후 **[게시] 까지 눌러야** 반영된다.
  저장만 하고 게시를 안 하는 게 가장 흔한 삽질이다.
- **값을 바꾼 뒤에는 앱을 완전히 종료하고 다시 켠다.** 판정 ViewModel 이 HOME 백스택 엔트리에
  붙어 있어서 탭 전환이나 백그라운드 복귀만으로는 다시 fetch 하지 않는다.
- 로그: `adb logcat | grep NewFeatureGuide`
  (`Log.d` 두 줄 + 최종 판정 한 줄은 `System.out` 이라 태그 필터 `-s` 대신 grep 을 쓴다.)

## 2. 상태 저장 위치 — 초기화용

앱 삭제 후 재설치가 가장 확실하다. 부분 초기화가 필요하면:

| 무엇 | SharedPreferences 파일 | 키 |
|---|---|---|
| "봤음" 플래그 | `v2_notice` | `seen` |
| 원격 설정 캐시 | `new_feature_guide` | `newFeatureGuide.v2.remoteConfig.<versionName>` (현재 `1.1.0`) |
| 최초 판정 시각 | `new_feature_guide` | `newFeatureGuide.firstSeenAt.<feature 키>` |

```sh
adb shell run-as com.pickflow.app rm shared_prefs/new_feature_guide.xml
adb shell run-as com.pickflow.app rm shared_prefs/v2_notice.xml
```

- "봤음" 플래그는 **Dev Mode 스위치로도 되돌릴 수 있다** — 환경 배지 탭 → 패스코드 → "V2 안내 팝업".
- 원격 캐시는 앱 버전을 올리면 키가 바뀌어 저절로 비워진다.

## 3. 붙여넣을 JSON

> 시각은 전부 **밀리초 epoch**. 아래 값은 2026-09-06 기준이라 날짜가 지나면 다시 계산한다.
> `date -u -d '2026-10-06' +%s000` (macOS: `date -u -j -f '%Y-%m-%d' 2026-10-06 +%s000`)

| 앵커 | ms |
|---|---|
| 2026-09-01 | `1788220800000` |
| 2026-09-02 | `1788307200000` |
| 2026-10-06 | `1791244800000` |
| 2026-12-31 | `1798675200000` |

**(A) 팝업 활성 · 뱃지 활성** — 기본 확인용
```json
{"features":[
  {"key":"v2_update_modal","startAt":1788220800000,"endAt":1791244800000},
  {"key":"home_new_badge","durationDays":14}
]}
```

**(B) 팝업 기간 시작 전** — 아직 안 뜸
```json
{"features":[{"key":"v2_update_modal","startAt":1791244800000,"endAt":1798675200000}]}
```

**(C) 팝업 기간 종료** — 더 이상 안 뜸
```json
{"features":[{"key":"v2_update_modal","startAt":1788220800000,"endAt":1788307200000}]}
```

**(D) 끝이 없음** — 무기한이 아니라 **꺼짐**이 정답
```json
{"features":[{"key":"v2_update_modal","startAt":1788220800000}]}
```

**(E) 빈 배열** — 전부 꺼짐
```json
{"features":[]}
```

## 4. 시나리오

### 팝업 (`v2_update_modal`)

| # | 상황 | 절차 | 기대 |
|---|---|---|---|
| P1 | 최초 노출 | 앱 삭제·재설치 → (A) 게시 → 앱 실행 → 탐색 탭 | 팝업이 뜬다 |
| P2 | 확인 후 재진입 | P1 에서 "확인했어요" → 앱 종료 → 재실행 | 다시 뜨지 않는다 |
| P3 | 바깥 탭 dismiss 없음 | P1 상태에서 팝업 바깥 어둔 영역 탭 | 닫히지 않는다(확인 버튼만) |
| P4 | 기간 시작 전 | 앱 삭제·재설치 → (B) 게시 → 재실행 | 안 뜬다 |
| P5 | 기간 종료 | 앱 삭제·재설치 → (C) 게시 → 재실행 | 안 뜬다 |
| P6 | 끝 없는 설정 | 앱 삭제·재설치 → (D) 게시 → 재실행 | **안 뜬다**(무기한 아님) |
| P7 | 파라미터 부재 | 앱 삭제·재설치 → (E) 게시 → 재실행 | 안 뜬다 |
| P8 | Dev Mode 되돌리기 | P2 상태에서 Dev Mode → "V2 안내 팝업" ON | **화면을 다시 열지 않아도** 즉시 다시 뜬다 |
| P9 | 원격 OFF 가 우선 | P8 로 플래그를 되돌린 뒤 (E) 게시 → 재실행 | 안 뜬다(안 봤어도 원격이 꺼짐) |
| **P10** | **게스트/로그아웃** | 로그아웃 상태 또는 미로그인으로 (A) → 탐색 탭 | **뜬다** |
| P11 | 오프라인 폴백 | P1 로 한 번 받은 뒤 앱 종료 → 비행기 모드 → 재실행 | 마지막 캐시로 판정해 그대로 뜬다 |
| P12 | 최초 실행 + 오프라인 | 앱 삭제·재설치 → 비행기 모드 → 실행 | 안 뜬다(캐시도 없음 = 안전하게 꺼짐) |

> **P10 이 이 티켓의 핵심 회귀다.** iOS 는 평가를 로그인 분기 안에 넣어 게스트가 모달을
> 영영 못 받는 버그가 났다(PR #77). 안드로이드는 `V2NoticeViewModel` 이 `AuthService` 를
> 아예 받지 않는다.

### 뱃지 (`home_new_badge`)

지도와 리스트가 같은 `MoodFilterRow` 를 쓰고 판정 ViewModel 인스턴스도 공유한다.

| # | 상황 | 절차 | 기대 |
|---|---|---|---|
| B1 | 켬 | 앱 삭제·재설치 → (A) 게시 → 실행 → 탐색 탭 | 햇살·야경 칩 우상단에 주황 dot |
| B2 | 지도↔리스트 일치 | B1 에서 리스트 토글 | 리스트에서도 같은 dot |
| B3 | 끔 | (E) 게시 → 앱 종료 → 재실행 | **dot 이 사라진다** |
| B4 | 유저 기준 만료 | (A) 로 `durationDays:1` 게시 → 실행 → 기기 날짜를 이틀 뒤로 → 재실행 | dot 이 사라진다 |
| B5 | 윤슬·노을 | 어느 케이스든 | 원래 dot 없음(`MoodFilter.isNew` 가 false) |

> ⚠️ **B3 이 정상 동작이다.** Console 에 `home_new_badge` 를 게시하지 않으면 지금까지 늘 보이던
> 햇살·야경 dot 이 사라진다. "파라미터 부재 = 꺼짐" 이 계약이다. 릴리스 전에 게시 여부를 확인한다.

### 공통

| # | 상황 | 절차 | 기대 |
|---|---|---|---|
| C1 | 앱 버전 업 시 캐시 초기화 | (A) 로 한 번 받은 뒤 `versionName` 을 올려 재설치 | 캐시 키가 바뀌어 새로 fetch |
| C2 | 깨진 JSON | `new_feature_flags` 에 `{{{` 게시 → 재실행 | 크래시 없음, 전부 꺼짐, 로그에 파싱 실패 |
| C3 | `spot_open_guide` | 키를 활성 구간으로 게시 | 앱에서 아무 일도 안 일어남(대응 화면 없음) |
| C4 | Firebase 미설정 | `google-services.json` 없는 빌드 | 크래시 없음, 전부 꺼짐 |

## 5. 안 뜰 때 원인 3분법

로그 세 줄이면 "기간인지 / 이미 봤는지 / 아예 평가를 안 한 건지" 가 바로 갈린다.

```
NewFeatureGuide: fetchAndActivate activated=true source=1        ← 원격을 받아왔는가
NewFeatureGuide: new_feature_flags={"features":[...]}            ← 무슨 값을 받았는가
NewFeatureGuide: isActive(v2_update_modal)=false feature=...     ← 기간 판정 결과
NewFeatureGuide: v2_update_modal isActive=false hasSeen=true visible=false
```

| 증상 | 원인 |
|---|---|
| `isActive=false`, `feature=null` | 배열에 키가 없다 → Console 게시 확인 |
| `isActive=false`, `feature=NewFeature(...)` | 기간 밖 → `startAt`/`endAt` 확인 |
| `isActive=true`, `hasSeen=true` | 이미 봤다 → Dev Mode 스위치나 prefs 초기화 |
| 로그 자체가 없음 | 화면에 도달하지 못했거나 평가가 안 돌았다 |
| `fetch 실패 — 마지막 캐시로 판정한다` | 네트워크/Firebase 설정 문제. 캐시로 계속 돈다 |

## 6. 자동화된 범위

아래는 이미 유닛 테스트가 잡고 있어 수동 확인이 필수는 아니다(회귀 확인용으로만).

- 판정 규칙 전체(경계 포함): `PrefsNewFeatureGuideTest` 11개 — P4·P5·P6·P7·B4·P11·P12 대응
- 노출 합성과 Dev Mode 복귀: `V2NoticeViewModelTest` 8개 — P2·P8·P9·**P10** 대응

수동으로만 확인 가능한 것: **P1·P3·B1·B2·B3·C1·C2·C4**, 그리고 실제 Console 게시 반영 여부.
