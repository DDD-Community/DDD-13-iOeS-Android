# [PV-59] API 문서 ↔ 구현 매핑

**출처**: `docs/PV-59/api-spec.html` — *pickflow API — 유저 스팟 공개 시스템 (2026.08.14)*
브랜치 `2026/HJY/유저스팟_공개시스템_개발`의 신규/변경 API만 모은 문서.

PV-59가 무드 필터를 태우는 엔드포인트는 **`GET /v1/spots`(리스트)** 와
**`GET /v1/spots/viewport`(지도)** 두 개다. 이 문서에는 앞의 것만 있다(§6·§8).

---

## 1. 결론 요약

| # | 항목 | 문서 | 구현(대조 전) | 조치 |
|---|---|---|---|---|
| 1 | 햇살 코드 | `SUNLIGHT` | `SUNLIGHT` | ✅ 일치 |
| 2 | 야경 코드 | **`NIGHT_VIEW`** | `NIGHT` | ✅ **정정 완료** |
| 3 | `theme` 다중 전달 | **미지원**(단일 string) | 반복 파라미터 전제 | 🔜 **백엔드에 요청** (§4·§5) |
| 4 | `theme` 미전달 | 전체 조회 | 빈 Set → 미전달 | ✅ 일치 |
| 5 | 응답 `theme` 형식 | 2글자 코드(`"SS"`) | 코드·풀네임 모두 파싱 | ✅ 일치 (신규 2종 코드는 미확인) |
| 6 | 정렬 기준 변경 | 북마크순 → **좋아요순** | 미반영 | ℹ️ PV-59 범위 밖 (§7) |
| 7 | `likeCount`/`isLiked` 신규 | 응답에 추가 | 미반영 | ℹ️ PV-59 범위 밖 (§7) |
| 8 | 지도 최초 로드가 리스트 API 사용 | — | 좌표 (0,0) | ⚠️ 기존 결함 (§6) |

---

## 2. `GET /v1/spots` 파라미터 대조

문서 원문: *"공개(PUBLISHED)된 스팟 목록을 6개 단위로 페이징 조회한다."*

| 파라미터 | 문서 스펙 | 구현 | 상태 |
|---|---|---|---|
| `page` | integer ≥ 0, 기본 0 | `page: Int` (0-base) | ✅ |
| `theme` | **string** (`SpotTheme`)<br>Enum: `SUNSET` `YUNSEUL` `SUNLIGHT` `NIGHT_VIEW`<br>미전달 시 전체 | `List<String>?` (반복 전달) | ⚠️ §4 |
| `latitude` | double, `sort=DISTANCE` 시 필수 | `coordinates?.latitude` (6자리 절삭) | ✅ |
| `longitude` | double, 〃 | `coordinates?.longitude` | ✅ |
| `sort` | `DISTANCE` \| `RECOMMENDED`(기본) | `SpotSort` enum | ✅ |

**응답**

| 필드 | 문서 | 구현 (`SpotItemDto` → `Spot`) |
|---|---|---|
| `spotId` | integer | `id: String` (toString) ✅ |
| `name` | string | `name` ✅ |
| `theme` | **`"SS"`** (2글자 코드) | `parseTheme()` — 코드·풀네임 모두 수용 ✅ |
| `thumbnailUrl` | string | `imageUrl` (blank → null) ✅ |
| `distanceKm` | double | `distanceKm` ✅ |
| `bookmarkCount` | integer | 미매핑 (리스트 셀에서 미사용) |
| `isBookmarked` | boolean | 미매핑 (`bookmarkedIds`로 별도 관리) |
| `likeCount` | **신규** | 미매핑 — §5 |
| `isLiked` | **신규** | 미매핑 — §5 |
| `page` / `hasNext` | integer / boolean | `SpotPage.page` / `hasNext` ✅ |

> 문서상 **요청 enum은 풀네임인데 응답은 2글자 코드**다. 이 비대칭 때문에
> `parseTheme`가 양쪽을 모두 받도록 되어 있고, 그 설계가 문서로 확인됐다.

---

## 3. 정정 완료 — 야경 코드

문서: *"테마 필터 (SUNSET=노을, YUNSEUL=윤슬, SUNLIGHT=햇살, **NIGHT_VIEW=야경**)"*

구현에서는 `NIGHT`로 가정하고 있었다. 문서 기준으로 일괄 정정했다.

| 파일 | 변경 |
|---|---|
| `core/services/protocols/Spot.kt` | `enum class SpotTheme { SUNLIGHT, YUNSEUL, SUNSET, NIGHT_VIEW }` |
| `core/network/mapper/SpotMapper.kt` | `parseTheme`: `"NV"`, `"NIGHT_VIEW"`, `"NIGHT"` → `NIGHT_VIEW` |
| 그 외 12개 파일 | `SpotTheme.NIGHT` → `SpotTheme.NIGHT_VIEW` (기계적 치환) |

전송값은 `SpotTheme.name`이므로 이 rename만으로 `?theme=NIGHT_VIEW`가 나간다.

**남은 미확인 사항**: 응답의 2글자 코드. 문서에 `SS`만 예시로 있고 신규 2종은 없다.
`SL`/`NV`로 추정해 뒀으며, 실제 응답을 보면 정정해야 한다.

```kotlin
"SL", "SUNLIGHT" -> SpotTheme.SUNLIGHT      // SL 은 추정
"NV", "NIGHT_VIEW", "NIGHT" -> SpotTheme.NIGHT_VIEW  // NV 는 추정
```

---

## 4. 다중선택이 API 스펙에 없다 — 백엔드 요청으로 결정

문서의 `theme`는 **단일 `string`**이다. 배열/반복/CSV 어느 것도 명시돼 있지 않다.

```
theme  string (SpotTheme)
       Enum: "SUNSET" "YUNSEUL" "SUNLIGHT" "NIGHT_VIEW"
       테마 필터 (...), 미전달 시 전체
```

그런데 PV-59 완료조건은 **"다중선택 지원"**이다. 즉 **완료조건과 API 스펙이 충돌한다.**

앞서 운영 서버 실측에서도 같은 결론이 나왔다 — 반복 파라미터를 보내면 400이 아니라
**200을 주면서 둘째 값을 조용히 버렸다.** 문서가 그 동작을 뒷받침한다(단일 파라미터니까).

### 현재 상태

임시 폴백(`MoodCompatSpotListService`)이 이 간극을 메우고 있어 **지금 당장 깨지지는 않는다.**

| 선택 | 서버 요청 | 결과 |
|---|---|---|
| 1개 | `?theme=SUNSET` | 서버 필터 |
| 2개 이상 | `theme` 미전달(전체) | **클라이언트 필터** |
| 햇살/야경 | 요청 안 함 | stub |

즉 다중선택은 **클라이언트 필터링으로만** 동작한다. 페이지네이션과 충돌한다 —
한 페이지(6건) 안에서 다 걸러지면 빈 화면이 되고, 페이지당 건수가 들쭉날쭉해진다.

### 결정 — **(a) 백엔드에 다중 필터 지원 요청** ✅

6건 페이징 구조에서 클라이언트 필터링은 사용자에게 "결과가 없다"로 보이는 구간을 만든다.
기획이 다중선택을 요구하는 이상 서버가 처리해야 한다. 요청 항목은 §5 참고.

클라이언트는 그때까지 임시 폴백으로 버티고, 서버가 준비되면
`backend-compat-rollback.md` 절차로 폴백을 제거한다.

---

## 5. 백엔드 요청 항목

다중 필터 지원을 요청하기로 했다(§4). 아래 4개를 함께 전달한다.

### 5-1. `theme` 다중 전달 지원 — **두 엔드포인트 모두**

| 엔드포인트 | 현재 | 요청 |
|---|---|---|
| `GET /v1/spots` | `theme` 단일 string | 배열/반복 파라미터 수용 |
| `GET /v1/spots/viewport` | 〃 (문서 미기재, 실측으로 동일) | 〃 |

형식은 **반복 파라미터**(`?theme=SUNSET&theme=YUNSEUL`)를 선호한다.
Retrofit이 `List<String>`을 그대로 직렬화하므로 클라이언트 변경이 없다.
CSV(`?theme=A,B`)여도 상관없으나 어느 쪽인지 확정이 필요하다.

> **리스트만 지원되면 안 된다.** 지도는 viewport를 쓰므로 한쪽만 되면
> 지도와 리스트 결과가 어긋난다(현재 폴백의 알려진 한계와 같은 증상).

### 5-2. viewport 응답에 `theme` 필드 추가

`SpotSummaryDto`에는 `theme`가 없다. 그래서 **서버가 다중 필터를 못 해줄 때
클라이언트가 대신 거를 수조차 없다**(리스트는 `theme`가 있어 가능).

5-1이 어려우면 이것만이라도 추가되면 클라이언트 폴백의 품질이 올라간다.

### 5-3. 응답 `theme`의 2글자 코드 확정

요청은 풀네임 enum인데 응답은 `"SS"` 같은 2글자 코드다. 신규 2종의 코드가
문서에 없다 — `SL`(햇살) / `NV`(야경)로 추정해 뒀다. 확정 값이 필요하다.

### 5-4. (참고) 리스트 응답에 좌표가 없는 문제

`GET /v1/spots` 응답(`SpotItemDto`)에는 `latitude`/`longitude`가 없다.
그런데 **지도 초기 로드가 이 엔드포인트를 쓴다**(§6). 자세한 내용은 §6.

---

## 6. 엔드포인트 사용 현황 — 지도는 두 개를 쓴다

| 화면 / 시점 | 서비스 | 엔드포인트 |
|---|---|---|
| 리스트 (최초·페이지네이션·필터 변경) | `SpotListService.fetch` | `GET /v1/spots` |
| **지도 최초 진입** | `SpotListService.fetch` | **`GET /v1/spots`** |
| 지도 카메라 이동·줌·무드 토글 이후 | `SpotMapService.fetchInViewport` | `GET /v1/spots/viewport` |
| 지도 마커 탭 → 바텀시트 | `SpotService.preview` | `GET /v1/spots/{id}/preview` |
| 보관 탭 | `ArchiveService` | `GET /v1/users/me/saved-spots` |

지도가 두 엔드포인트를 쓰는 이유는 `HomeMapViewModel`의 분기 때문이다.

```kotlin
// 최초에는 viewport(카메라 영역)를 아직 모르므로 리스트 API 로 대체한다.
LaunchedEffect(Unit) { viewModel.load() }        // → GET /v1/spots
lastViewport?.let { onViewportChanged(it, ...) } // → GET /v1/spots/viewport
    ?: load()                                    // → GET /v1/spots
```

`load()`는 `lastViewport`가 아직 null일 때만 타므로, **첫 카메라 정착 전까지의 짧은
구간에만** 리스트 API가 쓰인다. 이후로는 전부 viewport다.

### ⚠️ 이 분기의 부작용 (PV-59 이전부터 존재)

두 엔드포인트의 응답 필드가 상호 보완적이라, 서로를 대체할 수 없다.

| DTO | 엔드포인트 | `theme` | `latitude`/`longitude` | `isMySpot` |
|---|---|---|---|---|
| `SpotItemDto` | `/v1/spots` | ✅ | **❌ 없음** | ❌ |
| `SpotSummaryDto` | `/v1/spots/viewport` | **❌ 없음** | ✅ | ✅ |

리스트 응답에 좌표가 없으므로 `SpotMapper.toSpot()`이 **`latitude = 0.0, longitude = 0.0`
으로 하드코딩**한다. 즉 지도 최초 로드가 만들어내는 마커는 좌표가 (0, 0)이다.

첫 카메라 정착 이벤트가 곧바로 viewport 응답으로 덮어쓰기 때문에 실사용에서는
드러나지 않지만, **구조적으로는 잘못된 데이터가 잠깐 지도에 올라간다.**

PV-59 범위 밖이라 이번에 손대지 않았다. 정리하려면 두 갈래다.

- (i) 지도 최초 로드도 viewport를 쓰도록 변경 — 초기 카메라 영역을 기본값으로 계산
- (ii) 리스트 응답에 좌표 추가 요청 (§5-4)

---

## 7. PV-59 범위 밖이지만 알아둘 변경

같은 브랜치에서 리스트 조회 동작이 함께 바뀌었다. **PV-59에서는 손대지 않았다.**

| 변경 | 내용 | PV-59 영향 |
|---|---|---|
| 추천순 정렬 기준 | `bookmark_count` → `like_count` | 없음 (클라는 `sort=RECOMMENDED`만 보냄) |
| `likeCount` / `isLiked` 신규 필드 | 응답에 추가 | 없음 (미매핑, 무시됨) |
| 좋아요 API 신규 | `POST/DELETE /v1/spots/{id}/likes` | 없음 |

`SpotItemDto`가 `ignoreUnknownKeys = true`라 신규 필드가 있어도 파싱이 깨지지 않는다.
좋아요 기능 반영은 별도 티켓이 필요하다.

---

## 8. 문서에 없는 것 — 지도 viewport

PV-59는 `GET /v1/spots/viewport`도 쓰는데 **이 API 문서에 없다**(이번 브랜치 변경분만
모은 문서이므로). 따라서 viewport의 `theme` 파라미터가 신규 2종을 받는지,
다중을 받는지는 여전히 미확인이다.

실측(2026-08-04)으로는 리스트와 동일하게 동작했다 — `SUNSET`/`YUNSEUL` 단일만 처리,
신규 값은 400, 반복 파라미터는 200이지만 첫 값만 적용.

또한 viewport 응답(`SpotSummaryDto`)에는 `theme` 필드가 아예 없어 **클라이언트 재필터가
불가능**하다. 지도에서 무드 2개를 고르면 필터가 걸리지 않은 전체 마커가 나온다
(`backend-compat-rollback.md` §2 참고).

---

## 9. 다음 액션

- [x] 다중 필터를 백엔드에 요청하기로 결정 — §4
- [ ] **(우선)** 백엔드에 §5 요청서 전달 — 다중 전달(두 엔드포인트) · viewport `theme` 필드 · 2글자 코드
- [ ] `GET /v1/spots/viewport` 스펙 문서 확보 — §8
- [ ] 위가 해소되면 `MoodBackendCompat` 제거 — `backend-compat-rollback.md`
- [ ] (별도 티켓) 지도 최초 로드의 (0,0) 좌표 문제 — §6

## 10. 관련 문서

- 폴백 제거 절차: `docs/PV-59/backend-compat-rollback.md`
- 구현 사양: `docs/PV-59/mood-filter-expansion-implementation-prompt.md`
- 후속 논의: `docs/PV-59/mood-filter-expansion-discussion.md`
