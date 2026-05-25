# Pickflow BE API 체크리스트

> 출처: `https://pickflow-api.us/api/swagger-ui/index.html` (raw spec: `GET https://pickflow-api.us/api/api-docs`)
> Title: **Photo API v1.0.0** · OpenAPI 3.1.0 · 26 endpoints (admin/internal 3개 제외)

## 공통 규약

- **Base URL**: `https://pickflow-api.us/api`
- **인증**: 모든 endpoint가 `Bearer Authentication` 전역 적용. `Authorization: Bearer <accessToken>` 헤더 필수 (로그인/refresh도 스펙상 동일 — BE 확인 TODO).
- **응답 envelope** (모든 응답 공통):
  ```kotlin
  @Serializable data class ApiResponse<T>(
      val success: Boolean,
      val code: String,
      val message: String,
      val data: T? = null,
  )
  ```
- **페이지네이션**: page 기반 (0-base) — `{ page: Int, hasNext: Boolean }`. cursor 아님.
- **테마 enum**: `SUNSET`, `YUNSEUL` (서버 정의 2종). 현재 Android `SpotTheme(CAFE/RESTAURANT/...)`는 폐기 대상.
- **ID 타입**: 서버는 모두 `integer(int64)` → 클라이언트 `Long`.

## 작업 상태 범례

- `[ ]` 미연동 / `[x]` 연동 완료
- **신규** = 대응하는 Android Service 없음 → 신설
- **수정** = 기존 Service 있으나 시그니처/구현 변경
- **그대로** = 기존 인터페이스 유지 (해당 없음, 모두 수정/신규)

---

## Phase B — 인증 (4 endpoints)

### `[x] POST /v1/auth/kakao` — 카카오 로그인
- **operationId**: `kakaoLogin`
- **request** `KakaoLoginRequest`:
  - `*accessToken: String` — Kakao SDK 액세스 토큰
- **response 200** `ApiResponse<TokenResponse>`:
  - `accessToken: String`
  - `refreshToken: String`
  - `profile: UserProfile { userId, email, nickname, profileImageUrl, provider(APPLE|KAKAO) }`
- **매핑**: `SocialLoginService.loginWith(SocialAuthCredential)` → **수정** (반환에 `UserProfile` 포함하도록 `AuthenticatedSession` wrapper 신설) → `app/src/main/java/com/pickflow/android/core/services/impl/DefaultSocialLoginService.kt:20` + `core/network/api/AuthApi.kt:12`

### `[x] POST /v1/auth/apple` — Apple 로그인
- **operationId**: `appleLogin`
- **request** `AppleLoginRequest`:
  - `*identityToken: String` — Apple SDK identity token (RS256 JWT)
  - `user?: AppleUser { name?: { firstName, lastName }, email? }` — 최초 로그인 시만
- **response 200** `ApiResponse<TokenResponse>` (위와 동일)
- **매핑**: `AppleAuthProvider` (**신규**, v1은 StubAppleAuthProvider) + `SocialLoginService.loginWith()` → **수정** → `app/src/main/java/com/pickflow/android/core/services/impl/DefaultSocialLoginService.kt:28` + `core/network/api/AuthApi.kt:17` + `core/services/protocols/AppleAuthProvider.kt:3`

### `[x] POST /v1/auth/refresh` — 토큰 갱신
- **operationId**: `refresh`
- **request** `RefreshRequest`:
  - `*refreshToken: String`
- **response 200** `ApiResponse<TokenResponse>` (Refresh Token Rotation)
- **매핑**: `RefreshApi.refresh()` — OkHttp `TokenAuthenticator`가 401 시 자동 호출 → `app/src/main/java/com/pickflow/android/core/network/TokenAuthenticator.kt:53` + `core/network/api/RefreshApi.kt:18`

### `[x] POST /v1/auth/logout` — 로그아웃
- **operationId**: `logout`
- **request** `LogoutRequest`:
  - `*refreshToken: String`
- **response 200** `ApiResponse<Void>`
- **매핑**: `AuthService.logout()` → **수정** (API 호출 후 finally에서 TokenStore.clear) → `app/src/main/java/com/pickflow/android/core/services/impl/DefaultAuthService.kt:15`

---

## Phase C — 스팟 조회 (4 endpoints)

### `[x] GET /v1/spots/viewport` — 뷰포트 내 스팟 목록
- **operationId**: `getSpotsInViewport`
- **params (query)**:
  - `*topLeftLat: Double`, `*topLeftLng: Double`
  - `*topRightLat: Double`, `*topRightLng: Double`
  - `*bottomLeftLat: Double`, `*bottomLeftLng: Double`
  - `*bottomRightLat: Double`, `*bottomRightLng: Double`
  - `theme?: enum[SUNSET, YUNSEUL]`
- **response 200** `ApiResponse<SpotViewportResponse>`:
  - `spots: [SpotSummary { spotId, spotImageUrl?, latitude, longitude, isMySpot }]`
- **매핑**: `SpotMapService.fetchInViewport(ViewportBox, SpotTheme?)` (**신규**) → `app/src/main/java/com/pickflow/android/core/services/impl/DefaultSpotMapService.kt:14` + `core/network/api/SpotApi.kt:11` + 동반 `SpotTheme` enum 글로벌 마이그레이션(CAFE/RESTAURANT/BAR/ACTIVITY/NATURE → SUNSET/YUNSEUL)

### `[ ] GET /v1/spots` — 스팟 리스트 조회
- **operationId**: `getSpots`
- **params (query)**:
  - `page?: Int` (0-base)
  - `theme?: enum[SUNSET, YUNSEUL]`
  - `latitude?: Double`, `longitude?: Double` (sort=DISTANCE 시 필수)
  - `sort?: enum[DISTANCE, RECOMMENDED]` (기본 RECOMMENDED = 북마크 많은 순)
- **response 200** `ApiResponse<SpotListResponse>`:
  - `spots: [SpotItem { spotId, name, theme, thumbnailUrl, distanceKm? }]`
  - `page: Int`, `hasNext: Boolean`
- **매핑**: `SpotListService.fetch(...)` → **수정** (cursor→page, theme enum 교체, sort 추가)

### `[ ] GET /v1/spots/{spotId}` — 스팟 상세 조회
- **operationId**: `getSpotDetail`
- **params (path)**: `*spotId: Long`
- **response 200** `ApiResponse<SpotDetailResponse>`:
  - `spotId, name, comment, theme(SUNSET|YUNSEUL), latitude, longitude`
  - `address, addressRoad, addressJibun, imageUrl`
  - `recordedDate(yyyy-MM-dd), recordedTime`
  - `weatherSky(CLEAR|MOSTLY_CLOUDY|OVERCAST)`
  - `precipitation(NONE|RAIN|RAIN_SNOW|SNOW|SHOWER), precipitationProbability: Int`
  - `congestionLevel(RELAXED|NORMAL|SLIGHTLY_CROWDED|CROWDED)`
  - `sunsetTime, astronomyDate, weatherUpdatedAt, congestionUpdatedAt`
  - `parkingInfo, bookmarkCount, isBookmarked, isMySpot`
- **매핑**: `SpotService.spot(Long)` → **수정** (반환 모델 `SpotDetail` 전면 확장). 비로그인 시 `isBookmarked`/`isMySpot` = false.

### `[ ] GET /v1/spots/{spotId}/preview` — 스팟 미리보기
- **operationId**: `getSpotPreview`
- **params**: `*spotId(path): Long`, `latitude?: Double`, `longitude?: Double`
- **response 200** `ApiResponse<SpotPreviewResponse>`:
  - `spotId, name, isMySpot, theme(SUNSET|YUNSEUL), bookmarkCount`
  - `distanceKm?, imageUrl?, addressSimple, addressRoad?, addressJibun?`
- **매핑**: `SpotService.preview(Long, Coordinates?)` (**신규**)

---

## Phase D-1 — 마이페이지 / 사용자 (5 endpoints)

### `[ ] GET /v1/users/me` — 마이페이지 홈탭
- **operationId**: `getMyPageHome`
- **response 200** `ApiResponse<MypageHomeResponse>`:
  - `profileImageUrl, nickname(닉네임#해시태그), savedSpotCount, recordedSpotCount`
- **매핑**: `UserService.fetchMyPage(): MyPageHome` → **수정** (현재 `fetchUserName()`만 있음)

### `[ ] PATCH /v1/users/me` — 프로필 수정
- **operationId**: `updateProfile`
- **params (query)**: `nickname?: String`
- **request** `multipart/form-data`:
  - `profileImage?: binary` — 프로필 이미지
- **response 200** `ApiResponse<UpdateProfileResponse>`:
  - `displayName(닉네임#해시태그), profileImageUrl`
- **매핑**: `UserService.updateProfile(nickname?, imageUri?)` (**신규**)

### `[ ] DELETE /v1/users/me` — 회원 탈퇴
- **operationId**: `deleteAccount`
- **response 200** `ApiResponse<Void>` (소프트 삭제 + 모든 토큰/OAuth 해제)
- **매핑**: `AuthService.withdraw()` → **수정**

### `[ ] POST /v1/users/me/withdrawal-reason` — 탈퇴 사유 등록
- **operationId**: `saveWithdrawalReason`
- **request** `WithdrawalReasonRequest`:
  - `*reasonType: enum[OTHERS]`
  - `content?: String` (기타 사유일 때만, max 200)
- **response 200** `ApiResponse<Void>`
- **매핑**: `UserService.saveWithdrawalReason(type, content?)` (**신규**)

### `[ ] PATCH /v1/users/restore` — 탈퇴 계정 복구
- **operationId**: `restoreAccount`
- **params (query)**: `*restoreToken: String`
- **response 200** `ApiResponse<Void>`
- **매핑**: `AuthService.restore(restoreToken)` (**신규**) — 복구 후 소셜 로그인 재시도 필요

---

## Phase D-2 — 보관함 (3 endpoints)

### `[ ] GET /v1/users/me/archive` — 보관함 조회
- **operationId**: `getArchiveImage`
- **response 200** `ApiResponse<ArchiveImageResponse>`:
  - `archiveName, archiveImageUrl?` (presigned URL, 없으면 null)
- **매핑**: `ArchiveService.fetch()` (**신규**)

### `[ ] POST /v1/users/me/archive` — 보관함 이미지 등록/변경
- **operationId**: `updateArchiveImage`
- **request** `multipart/form-data`:
  - `*archiveImage: binary`
- **response 200** `ApiResponse<ArchiveImageResponse>`
- **매핑**: `ArchiveService.updateImage(uri)` (**신규**)

### `[ ] PATCH /v1/users/me/archive/name` — 보관함 이름 수정
- **operationId**: `updateArchiveName`
- **request** `UpdateArchiveNameRequest`:
  - `*archiveName: String` (max 20)
- **response 200** `ApiResponse<ArchiveImageResponse>`
- **매핑**: `ArchiveService.updateName(name)` (**신규**)

---

## Phase D-3 — 북마크 & 저장된 스팟 (3 endpoints)

### `[ ] POST /v1/spots/{spotId}/bookmarks` — 북마크 지정
- **operationId**: `addBookmark`
- **params (path)**: `*spotId: Long`
- **response 201** `ApiResponse<BookmarkResponse>`:
  - `bookmarkCount: Long` (지정 후 현재 북마크 수)
- **매핑**: `BookmarkService.add(spotId): Long` → **수정** (현재 in-memory toggle)

### `[ ] DELETE /v1/spots/{spotId}/bookmarks` — 북마크 해제
- **operationId**: `removeBookmark`
- **params (path)**: `*spotId: Long`
- **response 200** `ApiResponse<BookmarkResponse>`
- **매핑**: `BookmarkService.remove(spotId): Long` → **수정**

### `[ ] GET /v1/users/me/saved-spots` — 저장된 스팟 목록
- **operationId**: `getSavedSpots`
- **params (query)**: `page?: Int`, `latitude?: Double`, `longitude?: Double`
- **response 200** `ApiResponse<SavedSpotListResponse>`:
  - `spots: [SavedSpotItem { spotId, name, theme, imageUrl?, latitude, longitude, distanceKm?, savedAt(date-time), deleted }]`
  - `page: Int`, `hasNext: Boolean`
- **매핑**: `BookmarkService.savedSpots(page, coords?): SavedSpotPage` → **수정**

---

## Phase D-4 — 나만의 스팟 (2 endpoints)

### `[ ] GET /v1/users/me/my-spots` — 나만의 스팟 목록
- **operationId**: `getMySpots`
- **params (query)**: `page?: Int`, `latitude?: Double`, `longitude?: Double`
- **response 200** `ApiResponse<MySpotListResponse>`:
  - `spots: [MySpotItem { spotId, name, theme, imageUrl?, latitude, longitude, distanceKm?, createdAt(date-time), status(PENDING|PUBLISHED|REJECTED), bookmarkCount }]`
  - `page: Int`, `hasNext: Boolean`
- **매핑**: `MySpotService.list(page, coords?)` (**신규**)

### `[ ] POST /v1/users/me/my-spots` — 나만의 스팟 등록
- **operationId**: `createMySpot`
- **request** `multipart/form-data` ⚠️ **스펙 누락(integer로 표기됨), BE 확인 필요**
  - 추정: `image: binary` + `meta: application/json` (name, theme, latitude, longitude, comment, recordedDate, recordedTime 등)
- **response 201** `ApiResponse<CreateMySpotResponse>`:
  - `spotId, status(항상 PENDING), imageUrl`
- **매핑**: `MySpotService.create(draft, imageUri)` (**신규**, 기존 `SpotService.register()` 이전)

---

## Phase E-1 — 게시판 (2 endpoints)

### `[ ] GET /v1/bbs/posts` — 게시글 목록
- **operationId**: `getPosts`
- **params (query)**: `*masterId: Long`, `page?: Int` (페이지당 20개, 고정 공지 상단)
- **response 200** `ApiResponse<BbsPostListResponse>`:
  - `items: [BbsPostItem { postId, title, createdAt(date), pinned }]`
  - `page: Int`, `hasNext: Boolean`
- **매핑**: `BoardService.posts(masterId, page)` (**신규**)

### `[ ] GET /v1/bbs/posts/{postId}` — 게시글 상세
- **operationId**: `getPostDetail`
- **params**: `*postId(path): Long`, `*masterId(query): Long`
- **response 200** `ApiResponse<BbsPostDetailResponse>`:
  - `masterId, postId, title, createdAt(date), content`
- **매핑**: `BoardService.detail(masterId, postId)` (**신규**)

---

## Phase E-2 — 스팟 신고 (1 endpoint)

### `[ ] POST /v1/spots/{spotId}/reports` — 스팟 신고
- **operationId**: `report`
- **params (path)**: `*spotId: Long`
- **request** `SpotReportRequest`:
  - `*content: String` (min 5, max 200)
- **response 201** `ApiResponse<SpotReportResponse>`:
  - `reportId: Long`
- **매핑**: `SpotReportService.report(spotId, content)` (**신규**)

---

## Phase E-3 — 내 스팟 알림 (2 endpoints)

### `[ ] GET /v1/users/me/my-spots/{spotId}/alarm` — 알림 구독 조회
- **operationId**: `getAlarm`
- **params (path)**: `*spotId: Long`
- **response 200** `ApiResponse<SpotAlarmResponse>`:
  - `spotId, enabled` (구독 이력 없으면 enabled=false)
- **매핑**: `MySpotAlarmService.get(spotId)` (**신규**)

### `[ ] PUT /v1/users/me/my-spots/{spotId}/alarm` — 알림 구독 변경
- **operationId**: `updateAlarm`
- **params (path)**: `*spotId: Long`
- **request** `UpdateSpotAlarmRequest`:
  - `*enabled: Boolean`
- **response 200** `ApiResponse<SpotAlarmResponse>`
- **매핑**: `MySpotAlarmService.update(spotId, enabled)` (**신규**)

---

## 제외 (admin / internal, 3 endpoints)

다음은 운영자용으로 앱에서 호출하지 않음:
- `POST /v1/internal/spots` (createSpots)
- `POST /v1/internal/spots/{spotId}/image-sync` (syncImage)
- `POST /v1/internal/spots/batch-upload` (batchUpload)

---

## Open TODO (BE 확인 필요)

- [ ] `POST /v1/users/me/my-spots`의 multipart 명세 누락 — 이미지 part 이름, JSON 메타 part 명세 확정 필요
- [ ] `POST /v1/auth/kakao`, `POST /v1/auth/apple`도 `Bearer Authentication` security 표기 — 실제로는 토큰 불필요 추정. BE 확인.
- [ ] `restoreToken` 발급 시점 — 로그인 API 응답 에러 페이로드에서 오는지 별도 endpoint인지 확정.
- [ ] 모든 enum 값(weatherSky, precipitation, congestionLevel 등)의 정식 한국어 표시 문자열 정의 — 클라이언트 string resource 매핑용.
