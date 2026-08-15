# [PV-59] API 문서 ↔ 구현 매핑

**출처**: `docs/PV-59/api-spec.html` — *pickflow API — 유저 스팟 공개 시스템 (2026.08.14)*
브랜치 `2026/HJY/유저스팟_공개시스템_개발`의 신규/변경 API만 모은 문서.

PV-59가 쓰는 엔드포인트는 **`GET /v1/spots` (스팟 리스트 조회)** 하나다.

---

## 1. 결론 요약

| # | 항목 | 문서 | 구현(대조 전) | 조치 |
|---|---|---|---|---|
| 1 | 햇살 코드 | `SUNLIGHT` | `SUNLIGHT` | ✅ 일치 |
| 2 | 야경 코드 | **`NIGHT_VIEW`** | `NIGHT` | ✅ **정정 완료** |
| 3 | `theme` 다중 전달 | **미지원**(단일 string) | 반복 파라미터 전제 | ⚠️ **결정 필요** (§4) |
| 4 | `theme` 미전달 | 전체 조회 | 빈 Set → 미전달 | ✅ 일치 |
| 5 | 응답 `theme` 형식 | 2글자 코드(`"SS"`) | 코드·풀네임 모두 파싱 | ✅ 일치 (신규 2종 코드는 미확인) |
| 6 | 정렬 기준 변경 | 북마크순 → **좋아요순** | 미반영 | ℹ️ PV-59 범위 밖 (§5) |
| 7 | `likeCount`/`isLiked` 신규 | 응답에 추가 | 미반영 | ℹ️ PV-59 범위 밖 (§5) |

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

## 4. ⚠️ 결정 필요 — 다중선택이 API 스펙에 없다

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

### 선택지

| 안 | 내용 | 영향 |
|---|---|---|
| **(a) 백엔드에 다중 필터 요청** | `theme`를 배열로 받도록 스펙 확장 | 완료조건 그대로 충족. 서버 작업 필요 |
| (b) 단일선택으로 완료조건 변경 | UI를 단일선택으로 되돌림 | 서버 작업 없음. 기획 변경 필요 |
| (c) 클라 필터 유지 | 현행 폴백을 영구화 | 페이지네이션 문제가 남음 |

**(a)를 권장한다.** 6건 페이징 구조에서 클라이언트 필터링은 사용자에게 "결과가 없다"로
보이는 구간을 만든다. 기획이 다중선택을 요구하는 이상 서버가 처리해야 맞다.

---

## 5. PV-59 범위 밖이지만 알아둘 변경

같은 브랜치에서 리스트 조회 동작이 함께 바뀌었다. **PV-59에서는 손대지 않았다.**

| 변경 | 내용 | PV-59 영향 |
|---|---|---|
| 추천순 정렬 기준 | `bookmark_count` → `like_count` | 없음 (클라는 `sort=RECOMMENDED`만 보냄) |
| `likeCount` / `isLiked` 신규 필드 | 응답에 추가 | 없음 (미매핑, 무시됨) |
| 좋아요 API 신규 | `POST/DELETE /v1/spots/{id}/likes` | 없음 |

`SpotItemDto`가 `ignoreUnknownKeys = true`라 신규 필드가 있어도 파싱이 깨지지 않는다.
좋아요 기능 반영은 별도 티켓이 필요하다.

---

## 6. 문서에 없는 것 — 지도 viewport

PV-59는 `GET /v1/spots/viewport`도 쓰는데 **이 API 문서에 없다**(이번 브랜치 변경분만
모은 문서이므로). 따라서 viewport의 `theme` 파라미터가 신규 2종을 받는지,
다중을 받는지는 여전히 미확인이다.

실측(2026-08-04)으로는 리스트와 동일하게 동작했다 — `SUNSET`/`YUNSEUL` 단일만 처리,
신규 값은 400, 반복 파라미터는 200이지만 첫 값만 적용.

또한 viewport 응답(`SpotSummaryDto`)에는 `theme` 필드가 아예 없어 **클라이언트 재필터가
불가능**하다. 지도에서 무드 2개를 고르면 필터가 걸리지 않은 전체 마커가 나온다
(`backend-compat-rollback.md` §2 참고).

---

## 7. 다음 액션

- [ ] **(우선)** 백엔드에 `theme` 다중 전달 지원 여부 확인 — §4
- [ ] 응답의 신규 2종 2글자 코드 확인 (`SL`? `NV`?) — §3
- [ ] `GET /v1/spots/viewport` 스펙 확보 — §6
- [ ] 위 3개가 해소되면 `MoodBackendCompat` 제거 — `backend-compat-rollback.md`

## 8. 관련 문서

- 폴백 제거 절차: `docs/PV-59/backend-compat-rollback.md`
- 구현 사양: `docs/PV-59/mood-filter-expansion-implementation-prompt.md`
- 후속 논의: `docs/PV-59/mood-filter-expansion-discussion.md`
