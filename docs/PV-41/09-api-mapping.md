# PV-41 — 서버 API ↔ 클라이언트 구현 매핑

> 출처: `pickflow API — 유저 스팟 공개 시스템` (2026.08.14, Redocly)
> 서버 브랜치 `2026/HJY/유저스팟_공개시스템_개발`, 서버 문서 `docs/user-spot-publication-system.md`
> 대상 구현: 커밋 `00add35` ([PR #4](https://github.com/DDD-Community/DDD-13-iOeS-Android/pull/4)) Stub-first 구현

Stub-first로 먼저 확정한 클라이언트 Service 계약을 실제 서버 명세와 1:1 대조한 결과다.
`Default*Service` 구현 전에 이 문서의 **B. 조정 필요**를 먼저 반영한다.

## 공통 계약 (확정)

| 항목 | 확정값 |
| --- | --- |
| Base URL / API version | context-path `/api` + `/v1` (dev `http://localhost:8080/api`) |
| 인증 방식 | `BearerAuth` (Authorization 헤더) |
| 공통 response envelope | `ApiResponse<T>` = `{ success, code, message, data }` |
| 성공 코드 | `S000`. 좋아요 등록만 `201`, 나머지 성공은 `200` |
| 공통 error envelope | 동일 envelope + 서비스 error code (`SP*`, `SL*`, `C*`) |
| 인증/권한 오류 | 토큰 없음·만료·블랙리스트 `401 C004`, 권한 없음 `403 C005` |
| 비공개 접근 정책 | **403이 아닌 `404 SP001`** — 존재 여부 자체를 숨긴다 |
| 날짜·시간 형식 | 날짜 `2024-05-01`, 시각 `18:30`, 타임스탬프 ISO-8601 UTC(`2019-08-24T14:15:22Z`) |
| 페이지 기준 / size | `page` 0-base, **size 6 고정**(스팟 리스트·저장된 스팟) |
| 상태 동시성 방식 | 서버 비관적 락 + 오픈 신청 이력(`spot_open_requests`) 적재. 경합 시 `409 SP004` |

## A. 매핑 완료 — 변경 불필요

| 서버 | 클라이언트 구현 | 비고 |
| --- | --- | --- |
| 상태값 `DRAFT / PENDING / RE_REVIEW_PENDING / PUBLISHED / REJECTED` | `MySpotStatus` | **5개 wire 값 완전 일치** |
| `POST /v1/users/me/my-spots/{spotId}/open-requests` | `MySpotService.requestOpen` | DRAFT→PENDING, REJECTED→RE_REVIEW_PENDING |
| `DELETE /v1/users/me/my-spots/{spotId}` | `MySpotService.delete` | 논리삭제 |
| `POST /v1/spots/{spotId}/likes` | `RecommendationService.recommend` | 명칭만 추천↔좋아요 |
| `DELETE /v1/spots/{spotId}/likes` | `RecommendationService.cancel` | 응답 `likeCount`가 최종값 — 화면은 이 값에 맞춘다 |
| `likeCount` / `isLiked` | `recommendationCount` / `isRecommended` | 의미 일치 |
| `isCurated` | `SpotSource.Curated` / `SpotSource.User` | 의미 일치 |
| 저장된 스팟 `isPrivate` / `deleted` | `SavedSpotAvailability.AUTHOR_PRIVATE` / `.DELETED` | 운영 삭제와 작성자 비공개 구분 요구 충족 |
| 저장된 스팟 비공개 시 `imageUrl = null` 마스킹 | `SavedSpot.imageUrl: String?` | 이미지 PRIVATE 복귀 대응 |
| `GET /v1/spots` 정렬 `RECOMMENDED`(기본) / `DISTANCE` | 리스트 정렬 | 서버 추천순 기준이 `bookmark_count → like_count`로 전환됨 (클라 영향 없음) |
| `theme` `SUNSET / YUNSEUL / SUNLIGHT / NIGHT_VIEW` | `SpotTheme` | PV-59에서 햇살·야경 추가분과 일치 |

## B. 조정 필요 — `Default*Service` 착수 전 반영

> **B1·B2·B3·B4 는 반영 완료**(아래 표의 상태 열 참고). 나머지는 미반영이다.

| # | 항목 | 현재 클라이언트 | 서버 명세 | 조치 | 상태 |
| --- | --- | --- | --- | --- | --- |
| B1 | 공개 해제 | `withdrawRequest()` + `cancelOpen()` **2개 메서드** | `DELETE /v1/users/me/my-spots/{spotId}/publications` **단일 엔드포인트**. PENDING·RE_REVIEW_PENDING이면 철회, PUBLISHED면 비공개 전환. 응답 `previousStatus`로 구분 | 두 메서드를 하나로 통합하고 `previousStatus`로 화면 문구를 분기 | ✅ **반영 완료** |
| B2 | 반려 상태 해제 | `withdrawRejection()` | 대응 엔드포인트 **없음**. REJECTED에서 해제 시도는 `400 SP009`(해제 대상 없음) | **버튼은 유지, 서버 호출 없이 반려 배너만 로컬에서 닫도록 재정의**([10-open-questions.md](10-open-questions.md) A1, 2026-08-22 확정) | ✅ **반영 완료** |
| B3 | 보완 후 재신청 | `reviseAndResubmit()` **1-step** | `PUT /v1/users/me/my-spots/{spotId}` (수정, **상태 불변**) → `POST .../open-requests` (재신청) **2-step** | 내부 2-call로 구현하고, 수정 성공 + 재신청 실패 시 복구 경로(재시도 안내) 정의 | ✅ **반영 완료** |
| B4 | 반려 사유 | `rejectionReason: String?` | 구조화 객체 `rejection { reason, reasonLabel, guideMessage, detail, rejectedAt }`. `reason` enum = `DUPLICATE / LOW_QUALITY / LOCATION_MISMATCH / FILTER_MISMATCH / ETC` (ETC는 `detail` 필수) | `RejectionReason` enum + 데이터 클래스로 승격. 문구는 서버 `reasonLabel`·`guideMessage` 사용 | ✅ **반영 완료** |
| B5 | 추천 가능 여부 | 필드 없음 | `isLikeable` — 유저 스팟은 **PUBLISHED만** 좋아요 허용(그 외 `400 SL003`), 큐레이션 스팟은 상태 무관 허용 | `SpotDetail`·프리뷰에 `isLikeable` 추가하고 버튼 활성화 근거로 사용 | 미반영 |
| B6 | 상태 변경 응답 | `MySpotTransitionResult(spotId, status, updatedAt)` | 응답 `data`는 `{ spotId, status }` (+해제는 `previousStatus`). **`updatedAt` 없음** | `updatedAt` 제거. 재조회 트리거는 응답 `status`로 처리 | 미반영 |
| B7 | 내 스팟 상세 | `MySpotService.detail()` 별도 | 전용 엔드포인트 없음. `GET /v1/spots/{spotId}`가 본인의 비공개 스팟까지 반환하고, 반려 시 `rejection` 동봉(타인 비노출) | `detail()` 제거하고 `SpotDetailService` 단일 경로로 통합 | 미반영 |
| B8 | 오류 코드 | `MySpotTransitionConflictException` 하나 | `SP001/SP004/SP005/SP008/SP009/SP010/SP011`, `SL001/SL002/SL003`, `C004/C005` | 아래 오류 매핑표대로 예외·문구 분기 | 미반영 |
| B9 | 수정 가능 상태 | 제약 없음 | DRAFT·REJECTED만 수정 가능. PENDING·PUBLISHED는 `400 SP010` → **공개를 먼저 해제**해야 함 | 수정 진입 가드 + SP010 안내 문구 | 미반영 |
| B10 | 삭제 가능 상태 | 제약 없음 | PENDING·RE_REVIEW_PENDING은 `409 SP011` → **오픈 신청을 먼저 철회**해야 함 | 삭제 진입 가드 + SP011 안내 문구 | 미반영 |
| B11 | 비공개 접근 | 목록/지도 복귀 안내 | `404 SP001` (403 아님) | 404를 "삭제되었거나 비공개" 단일 문구로 처리 — 존재 여부 노출 금지 | 미반영 |
| B12 | 좌표 수정 부작용 | 미반영 | 좌표 변경 시 주소·기상 격자·혼잡 지역·날씨가 **재계산**. 좌표 동일이면 문구만 갱신. 이미지 미첨부 시 기존 유지 | 수정 후 상세 강제 재조회 | 미반영 |

### 오류 코드 → 앱 처리 매핑

| 코드 | HTTP | 발생 조건 | 앱 처리 |
| --- | --- | --- | --- |
| `SP001` | 404 | 없는 스팟 / 비공개(타인) | "삭제되었거나 비공개인 스팟이에요" + 목록·지도 복귀 |
| `SP004` | 409 | 철회 직전 검수 확정 경합 | **`이미 처리된 신청이에요`** + 상세 강제 재조회 (기존 원칙 그대로) |
| `SP005` | 400 | 오픈 신청 불가 상태 | 재조회 후 버튼 상태 재계산 |
| `SP008` | 403 | 본인 스팟 아님 | 진입 차단 |
| `SP009` | 400 | 해제할 대상 없음(DRAFT) | 재조회 |
| `SP010` | 400 | 검수중·공개 상태 수정 시도 | "공개를 먼저 해제해주세요" |
| `SP011` | 409 | 검수중 삭제 시도 | "오픈 신청을 먼저 철회해주세요" |
| `SL001` | 409 | 이미 좋아요 | 응답값으로 상태 동기화(멱등 처리) |
| `SL002` | 400 | 좋아요 안 한 스팟 취소 | 응답값으로 상태 동기화 |
| `SL003` | 400 | 공개 아닌 유저 스팟 좋아요 | 버튼 비활성(`isLikeable`) |
| `C004` | 401 | 비로그인·만료 | 로그인 유도 팝업(기존 플로우 재사용) |
| `C005` | 403 | 권한 없음 | 진입 차단 |

## C. 서버 계약 미확정 — 이 문서 범위 밖

`Default*Service` 전환을 막는 잔여 항목이다. 추측으로 채우지 않는다.

| 항목 | 필요한 것 |
| --- | --- |
| **검수 결과 알림** (`ReviewResultService.status / acknowledge / acknowledgePublishedModal`) | 대응 엔드포인트가 문서에 **없다**. 미확인 결과 조회·확인 처리·처리중 신청 존재 여부를 서버가 주는지, 아니면 `GET /v1/users/me/my-spots`의 status 변화를 클라가 비교해 감지하는지 결정 필요 |
| 승인 완료 모달 확인 여부 | 저장 위치(서버 / 로컬) 미정 |
| `GET /v1/users/me/my-spots` (내 스팟 목록) | 이번 브랜치 범위 밖. `MySpot.status`·`bookmarkCount`가 응답에 포함되는지 미확인 |
| **목록 아이템의 `likeCount`** | 셀에 "추천 34"를 표기하려면 필요하다. `MySpotItemDto.likeCount` 로 옵셔널 파싱만 해둠 — 서버가 안 주면 추천 수는 숨는다 |
| **목록의 `isReleased` (노출 플래그)** | `GET /v1/spots/{id}` 에는 2026-09-08 추가돼 상세 토글이 서버 값을 그대로 쓴다(아래). `GET /v1/users/me/my-spots` 에는 아직 없어 목록 셀에서는 노출 여부를 표기하지 못한다 |
| **목록 아이템의 공개 이력 플래그** | "비공개"(공개됐다가 해제) 배지를 "뱃지 없음"(오픈 신청 전 DRAFT)과 가르는 근거. 해제 후 상태는 둘 다 `DRAFT` 라 응답만으로는 구분 불가. `MySpotItemDto.wasPublished` 로 옵셔널 파싱해 뒀고, 필드가 오기 전까지 비공개 배지는 뜨지 않는다 |
| `UpdateMySpotRequest` 필드 | 문서에 object로만 표기되어 개별 필드·validation 미노출. `SpotDraft`와 대조 필요 |
| 탈퇴 후 보존 | 공개 스팟·추천 보존 및 재가입 복구 계약 없음 |
| 어드민 검수 `POST /v1/admin/spots/{spotId}/reviews` | `USER_ADMIN` 전용. 앱 Out of Scope — 클라이언트 미구현이 정상 |

## 공개 토글 → `/releases` 로 교체 (2026-09-04)

dev 서버 OpenAPI 실측(`https://dev-api.pickflow-api.us/api/api-docs`) 결과 노출 전용 엔드포인트가 있다.

| 엔드포인트 | 의미 | status 변화 |
| --- | --- | --- |
| `POST /v1/users/me/my-spots/{spotId}/releases` | 노출 켜기 | 없음(PUBLISHED 유지) |
| `DELETE /v1/users/me/my-spots/{spotId}/releases` | 노출 끄기 | 없음(PUBLISHED 유지) |
| `DELETE /v1/users/me/my-spots/{spotId}/publications` | 공개 해제 | PENDING/RE_REVIEW_PENDING/PUBLISHED → **DRAFT** |

- 상세의 공개 ON/OFF 토글은 `/releases` 를 쓴다. 검수 flow 와 독립이라 **재검수 없이 왕복**한다.
  응답 `{ spotId, released }`. PUBLISHED 가 아니면 `SP012`.
- `/publications` 는 검수중 오픈 신청 철회에 그대로 남는다(상세 하단 "스팟 오픈 철회" 시트).
### `isReleased` 상세 응답 추가 (2026-09-08)

`SpotDetailResponse.isReleased` 가 생겨(`검수완료 후 지도뷰/리스트 노출 on/off, 비공개 시 false`)
토글의 단일 출처가 됐다. 기기 로컬 기억(`MySpotReleaseStore` / `PrefsMySpotReleaseStore`)은 제거했다
— 재설치·다른 기기에서 OFF 가 ON 으로 보이던 문제가 함께 사라진다.

- 상세 진입/재조회 시 `SpotDetail.isReleased` → `SpotOpenActionsViewModel.syncReleased()`.
- 토글 전송 중(`isInFlight`)에는 `syncReleased` 를 무시한다 — 늦게 도착한 예전 응답이 낙관적 값을 되돌리지 않게.
- 남은 것: `GET /v1/users/me/my-spots` 의 노출 플래그(목록 배지용).

## Integration Status

| 영역 | 상태 |
| --- | --- |
| 저장된 스팟(`BookmarkService`) / 좋아요(`LikeService`) / 내 스팟 목록(`MySpotService.list`) | `INTEGRATED` — stub 바인딩 제거 완료 |
| 상태 모델 / 오픈 신청 / 삭제 | `SPEC_READY` |
| 공개 해제(B1) / 반려 배너 로컬 닫기(B2) / 수정·재신청 2-step(B3) / 반려 사유 구조화(B4) | `INTEGRATED` — 실서버 경로 |
| 오류 코드 분기 / `isLikeable` / 상태 가드 (B5~B12) | `SPEC_READY` |
| 검수 결과 알림 | `LOCAL` — 서버 엔드포인트 없음. 목록 status 대조로 감지(`07-review-notification.md`). 2026-09-04 로 stub 패키지 전체 삭제 |

## 반영 내역 (B1·B2·B3·B4)

| 변경 | 파일 |
| --- | --- |
| `withdrawRequest()` + `cancelOpen()` → `unpublish(): MySpotUnpublishResult` (`previousStatus`, `wasOpenRequest`) | `protocols/MySpot.kt`, `stub/StubMySpotService.kt`, `stub/StubSpotBackend.kt`, `feature/spotdetail/SpotOpenViewModel.kt`, `SpotOpenScreen.kt` |
| `reviseAndResubmit()` → `update()`(상태 불변) + `requestOpen()` 2-step. `requestOpen`이 `REJECTED → RE_REVIEW_PENDING` 도 처리 | 위와 동일 + `feature/spotregistration/SpotRegistrationViewModel.kt` |
| 재신청 실패 복구: `isRevisionSaved` StateFlow — 수정 저장 후 재신청만 실패하면 재시도 시 수정을 다시 보내지 않는다 | `SpotRegistrationViewModel.kt` |
| `rejectionReason: String?` → `rejection: SpotRejection?` + `RejectionReason` enum 5종 | `protocols/MySpot.kt`, `protocols/SpotDetail.kt`, `stub/*`, `SpotOpenDetailContent.kt` |
| `withdrawRejection()` 제거 → 반려 배너를 세션 한정 로컬 상태로 닫는다. 확인 모달도 제거 | `protocols/MySpot.kt`, `stub/*`, `SpotOpenViewModel.kt`, `SpotOpenDetailContent.kt` |

화면 문구 `오픈 신청을 철회했어요` / `스팟을 비공개로 전환했어요` 는 `previousStatus` 분기를 위해 추가한 임시 카피다. **기획 확인이 필요하다.**

반려 배너는 서버 `guideMessage` 를 우선 표시하고 없으면 `reasonLabel` 로 대체한다.
스텁의 `reasonLabel` / `guideMessage`(`StubRejections`)는 화면 검증용 고정 문구이며, 프로덕션에서는 서버 값을 그대로 쓴다.

## 실서버 전환 현황 (2026-08-26)

| Service | 바인딩 | 비고 |
| --- | --- | --- |
| `BookmarkService` | `DefaultBookmarkService` | `isPrivate` → `AUTHOR_PRIVATE` 매핑 추가 (A 표 요구사항, 그동안 누락) |
| `LikeService` | `DefaultLikeService` | `POST·DELETE /v1/spots/{spotId}/likes`. `BookmarkService` 와 같은 형태(`add`/`remove`(spotId: String): Long)로 통일. `SpotApi.addLike/removeLike` 와 `SpotService.like/unlike` 는 제거했다 |
| `MySpotService` | `DefaultMySpotService` | **전 오퍼레이션 실서버**(2026-08-26 전환). `detail()` 만 `SpotApi.getSpotDetail` 을 탄다 — 전용 엔드포인트가 없어서다 |
| `ReviewResultService` | `StubReviewResultService` | 서버 엔드포인트 부재(C 표). 계약 확정 전까지 유지 |

`MySpotService` 잔여 위임 사유:

- `detail()` — B7. 전용 엔드포인트가 없어 `GET /v1/spots/{spotId}` 로 통합해야 한다.
  2026-08-26 dev 서버 실측 결과 `status`·`isCurated`·`likeCount`·`isLiked`·`isLikeable`·`rejection`
  이 모두 응답에 포함된다(`GET /api/v1/spots/3` 확인). `status` 는 DTO 가 이미 수신 중이고,
  남은 건 `rejection` 의 형태다 — dev 에 반려 스팟이 없어 항상 `null` 로만 관측된다.
  반려 상태 스팟 하나만 확보되면 `detail()` 은 실서버로 전환 가능하다.
- `create()` — 실 구현은 `DefaultMySpotService.realCreate()` 에 있고 테스트도 붙어 있다.
  상세가 stub 인 동안 등록만 실서버로 보내면 등록 직후 상세 진입이 stub 레코드에 없는 id 로 깨진다.
  B7 해소와 동시에 교체한다.
- `requestOpen()` / `unpublish()` / `update()` / `delete()` — 엔드포인트는 확정이나
  B6(`updatedAt` 부재)·B8(오류 코드 분기)·B9·B10(상태 가드) 미반영. 그대로 붙이면 오류 처리가 없다.

## 좋아요 응답 처리 정책 (2026-08-26 확정)

- 화면은 **낙관값을 유지**한다. `likeCount` 는 서버 최종값이지만 UI 에 되반영하지 않는다.
  `BookmarkService` 와 같은 정책이며, 응답이 늦게 와 화면이 되돌아가 보이는 걸 막는다.
  실패 시에만 직전 값으로 롤백하고 재시도 토스트를 띄운다.
- 409 `SL001`(이미 좋아요) / 400 `SL002`(좋아요 안 한 스팟 취소)는 non-2xx 이므로
  `ApiResponse.unwrap()` 에 닿지 않고 Retrofit 의 `HttpException` 으로 올라온다.
  "응답값으로 동기화"는 구현된 적이 없다 — 별도 분기가 필요하면 서비스에서
  `HttpException.code()` 로 잡아야 한다(현재는 미구현, 롤백 처리로 흡수).

## dev 서버 OpenAPI 실측 (2026-08-26)

`GET https://dev-api.pickflow-api.us/api/api-docs` (앱 JWT 필요, 경로는 `/v3/api-docs` 가 아니라
`/api-docs` 다 — swagger-initializer.js 가 가리키는 경로). 아래는 그 스냅샷에서 확정한 사실이다.

### 나만의 스팟 엔드포인트 (전부 존재)

| 오퍼레이션 | 엔드포인트 | 응답 data |
| --- | --- | --- |
| `list()` | `GET /v1/users/me/my-spots` | `MySpotListResponse{spots[], page, hasNext}` |
| `create()` | `POST /v1/users/me/my-spots` (multipart: `request` JSON + `image`) | `{spotId, status, imageUrl}` |
| `update()` | `PUT /v1/users/me/my-spots/{spotId}` (multipart, image 미첨부 시 기존 유지) | `{spotId, status, imageUrl}` |
| `delete()` | `DELETE /v1/users/me/my-spots/{spotId}` | `Void` |
| `requestOpen()` | `POST /v1/users/me/my-spots/{spotId}/open-requests` | `{spotId, status}` |
| `unpublish()` | `DELETE /v1/users/me/my-spots/{spotId}/publications` | `{spotId, previousStatus, status}` |

- `unpublish` 응답의 `previousStatus` 가 그대로 `MySpotUnpublishResult.previousStatus` /
  `wasOpenRequest` 분기에 쓰인다. 클라 추론이 필요 없다.
- **`detail()` 전용 엔드포인트는 없다**(확정). `GET /v1/spots/{spotId}` 로 통합해야 한다 — B7.

### B7 해소 — `SpotDetailResponse` 가 필요한 걸 다 준다

`status`, `isCurated`, `likeCount`, `isLiked`, `isLikeable`, `rejection` 모두 스키마에 있다.
`rejection` 형태도 확정됐다(그동안 dev 관측값이 항상 `null` 이라 미상이던 부분):

```
RejectionInfo { reason, reasonLabel, guideMessage, detail, rejectedAt }   // 전부 string
```

`SpotRejection` 과 필드가 일치하고 `rejectedAt` 만 추가다. `SpotDetailResponseDto` 의
"형태를 알 수 없어 흘려보낸다" 주석은 이제 유효하지 않다.

`theme` enum 은 `SUNSET | YUNSEUL | SUNLIGHT | NIGHT_VIEW` 다.

### B6 미해소 — `updatedAt` 은 서버 어디에도 없다

전체 스키마를 통틀어 `updatedAt` 필드를 가진 응답이 **하나도 없다**. `MySpotItem` 은
`createdAt` 만 준다. `MySpotDetail.updatedAt` 을 근거로 한 낙관적 경합 감지
(`MySpotTransitionConflictException`)는 서버가 이 값을 주기 전까지 성립하지 않는다.
전이 실패를 오류 코드로 판별하는 방식으로 바꾸거나, 서버에 필드 추가를 요청해야 한다.

### C 표 해소 항목

| 항목 | 결과 |
| --- | --- |
| `UpdateMySpotRequest` 필드 | 확정 — `name`·`theme` 필수, `latitude`·`longitude` 필수, `comment`·`recordedDate`(yyyy-MM-dd)·`recordedTime`(HH:mm) 선택. `CreateMySpotRequest` 와 동일하다 |
| `MySpotItem` 에 `status`·`bookmarkCount` 포함 여부 | 둘 다 포함 확정 |
| **검수 결과 알림** | **대응 엔드포인트 없음이 확정됐다.** `/v1/users/me/my-spots/{spotId}/alarm` 은 알림 설정(GET·PUT)이고 검수 결과 조회가 아니다. `ReviewResultService` 는 stub 유지하거나, `GET /v1/users/me/my-spots` 의 `status` 변화를 클라가 비교해 감지하는 방식으로 설계해야 한다 |

## MySpotService 실서버 전환 완료 (2026-08-26)

`DefaultMySpotService` 가 `StubMySpotService` 위임을 전부 걷어냈다. 잔여 stub 은
`StubReviewResultService` 하나뿐이다.

| 오퍼레이션 | 호출 |
| --- | --- |
| `list()` | `MySpotApi.getMySpots` |
| `detail()` | **`SpotApi.getSpotDetail`** (전용 엔드포인트 부재 — B7) |
| `create()` | `MySpotApi.createMySpot` (기존 `realCreate()` 를 승격) |
| `update()` | `MySpotApi.updateMySpot` — image 미첨부 시 서버가 기존 이미지 유지 |
| `requestOpen()` | `MySpotApi.requestOpen` |
| `unpublish()` | `MySpotApi.cancelPublication` — 응답 `previousStatus` 를 그대로 사용 |
| `delete()` | `MySpotApi.deleteMySpot` |

동반 수정:

- `parseMySpotStatus` 가 다섯 상태를 전부 매핑한다. 그동안 `DRAFT`·`RE_REVIEW_PENDING` 이
  `PENDING` 으로 접혀 상세 화면 액션이 어긋날 수 있었다(목록에만 쓰여 드러나지 않았다).
- `SpotDetailResponseDto.rejection` 을 `RejectionInfoDto` 로 선언했다.
- `MySpotDetail.updatedAt` / `MySpotStatusChange.updatedAt` 제거 — 아래 참조.

### `updatedAt` 제거와 그 여파

서버가 어떤 응답에서도 주지 않으므로 도메인 모델에서 뺐다. 쓰이던 곳은 두 군데였다.

1. **반려 배너 날짜** (`SpotOpenDetailContent`) — `rejection.rejectedAt` 으로 대체했다.
   의미상으로도 이쪽이 정확하다("마지막 수정 시각"이 아니라 "반려된 시각").
2. **전이 후 상세 갱신** (`SpotOpenViewModel.transition`) — `status` 만 갱신하면 충분하다.

낙관적 경합 감지는 넣지 않았다. `MySpotTransitionConflictException` 자체는 남겨뒀다 —
Dev Mode 의 `withdrawalReviewRace` 픽스처가 여전히 던지고 ViewModel 이 받아 재조회한다.
다만 **실서버는 이 예외를 던지지 않으므로 프로덕션에서는 동작하지 않는다.** 서버가
경합을 오류 코드로 구분해 주거나 `updatedAt` 을 내려주기 전까지는 재시도 토스트로 흡수된다.

### Dev Mode 픽스처 영향

`StubSpotBackend` 는 이제 나만의 스팟 흐름을 받치지 않는다. Dev Mode 의 stub 시나리오
(지연·실패·검수 경합 주입)는 `LikeService`·`SpotList`·`SpotMap`·`Bookmark`·`ReviewResult` 에만
걸리고, 상세·오픈 신청·철회·수정·삭제에는 더 이상 영향을 주지 않는다.
