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

| # | 항목 | 현재 클라이언트 | 서버 명세 | 조치 |
| --- | --- | --- | --- | --- |
| B1 | 공개 해제 | `withdrawRequest()` + `cancelOpen()` **2개 메서드** | `DELETE /v1/users/me/my-spots/{spotId}/publications` **단일 엔드포인트**. PENDING·RE_REVIEW_PENDING이면 철회, PUBLISHED면 비공개 전환. 응답 `previousStatus`로 구분 | 두 메서드를 하나로 통합하고 `previousStatus`로 화면 문구를 분기 |
| B2 | 반려 상태 해제 | `withdrawRejection()` | 대응 엔드포인트 **없음**. REJECTED에서 해제 시도는 `400 SP009`(해제 대상 없음) | 메서드 제거 또는 "반려 상태에서는 수정/재신청만 가능"으로 재정의 |
| B3 | 보완 후 재신청 | `reviseAndResubmit()` **1-step** | `PUT /v1/users/me/my-spots/{spotId}` (수정, **상태 불변**) → `POST .../open-requests` (재신청) **2-step** | 내부 2-call로 구현하고, 수정 성공 + 재신청 실패 시 복구 경로(재시도 안내) 정의 |
| B4 | 반려 사유 | `rejectionReason: String?` | 구조화 객체 `rejection { reason, reasonLabel, guideMessage, detail, rejectedAt }`. `reason` enum = `DUPLICATE / LOW_QUALITY / LOCATION_MISMATCH / FILTER_MISMATCH / ETC` (ETC는 `detail` 필수) | `RejectionReason` enum + 데이터 클래스로 승격. 문구는 서버 `reasonLabel`·`guideMessage` 사용 |
| B5 | 추천 가능 여부 | 필드 없음 | `isLikeable` — 유저 스팟은 **PUBLISHED만** 좋아요 허용(그 외 `400 SL003`), 큐레이션 스팟은 상태 무관 허용 | `SpotDetail`·프리뷰에 `isLikeable` 추가하고 버튼 활성화 근거로 사용 |
| B6 | 상태 변경 응답 | `MySpotTransitionResult(spotId, status, updatedAt)` | 응답 `data`는 `{ spotId, status }` (+해제는 `previousStatus`). **`updatedAt` 없음** | `updatedAt` 제거. 재조회 트리거는 응답 `status`로 처리 |
| B7 | 내 스팟 상세 | `MySpotService.detail()` 별도 | 전용 엔드포인트 없음. `GET /v1/spots/{spotId}`가 본인의 비공개 스팟까지 반환하고, 반려 시 `rejection` 동봉(타인 비노출) | `detail()` 제거하고 `SpotDetailService` 단일 경로로 통합 |
| B8 | 오류 코드 | `MySpotTransitionConflictException` 하나 | `SP001/SP004/SP005/SP008/SP009/SP010/SP011`, `SL001/SL002/SL003`, `C004/C005` | 아래 오류 매핑표대로 예외·문구 분기 |
| B9 | 수정 가능 상태 | 제약 없음 | DRAFT·REJECTED만 수정 가능. PENDING·PUBLISHED는 `400 SP010` → **공개를 먼저 해제**해야 함 | 수정 진입 가드 + SP010 안내 문구 |
| B10 | 삭제 가능 상태 | 제약 없음 | PENDING·RE_REVIEW_PENDING은 `409 SP011` → **오픈 신청을 먼저 철회**해야 함 | 삭제 진입 가드 + SP011 안내 문구 |
| B11 | 비공개 접근 | 목록/지도 복귀 안내 | `404 SP001` (403 아님) | 404를 "삭제되었거나 비공개" 단일 문구로 처리 — 존재 여부 노출 금지 |
| B12 | 좌표 수정 부작용 | 미반영 | 좌표 변경 시 주소·기상 격자·혼잡 지역·날씨가 **재계산**. 좌표 동일이면 문구만 갱신. 이미지 미첨부 시 기존 유지 | 수정 후 상세 강제 재조회 |

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
| `UpdateMySpotRequest` 필드 | 문서에 object로만 표기되어 개별 필드·validation 미노출. `SpotDraft`와 대조 필요 |
| 탈퇴 후 보존 | 공개 스팟·추천 보존 및 재가입 복구 계약 없음 |
| 어드민 검수 `POST /v1/admin/spots/{spotId}/reviews` | `USER_ADMIN` 전용. 앱 Out of Scope — 클라이언트 미구현이 정상 |

## Integration Status

| 영역 | 상태 |
| --- | --- |
| 상태 모델 / 오픈 신청 / 삭제 / 좋아요 / 저장된 스팟 | `SPEC_READY` |
| 공개 해제 / 수정·재신청 / 반려 사유 / 오류 코드 | `SPEC_READY` (B 조정 선행) |
| 검수 결과 알림 | `TBD` (C 참조) |
