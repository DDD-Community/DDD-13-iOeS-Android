# [PV-41] 유저 스팟 오픈 Stub 화면 구현 통합 프롬프트

> 상세·등록·지도·리스트·보관함을 잇는 cross-screen 기능이다. 이 문서는 TDD 게이트의 오케스트레이터이며 기능 사실은 `docs/PV-41/02-spot-detail.md`부터 `07-review-notification.md`를 참조한다.
>
> 단계별 방법론은 진입할 때만 읽는다.
> - Phase A: `docs/phases/phase-a-viewmodel-tdd.md`
> - Phase B: `docs/phases/phase-b-ui-tdd.md`
> - Phase C: `docs/phases/phase-c-snapshot.md`

## 0. 작업 컨텍스트

- 티켓: PV-41
- 실제 브랜치: `feature-PV-41`
- 기준 기획: `docs/PV-41/spot-open-implementation-plan.md`
- 상태·계약: `docs/PV-41/01-state-and-api.md`
- Figma file key: `WLGPjrQtLqyhq46zXxvXHp`
- 개발 방식: API 미배포 상태의 Stub-first. HTTP 경로를 추측하지 않는다.
- 프로젝트: Compose + MVVM + StateFlow + Hilt, No Repository, Service interface, ViewModel 의존성 5개 이하

## 1. 구현 범위

- `DRAFT`, `PENDING`, `RE_REVIEW_PENDING`, `REJECTED`, `PUBLISHED` 상태와 전이
- 오픈 신청·철회, 반려 철회·보완 재신청, 오픈 취소, 유저 스팟 삭제
- 공개 스팟 추천·취소, 셀프 추천, 로그인 가드와 실패 롤백
- 큐레이션 출처 줄과 공개 유저 스팟 뱃지
- `DRAFT` MY 핀과 공개 유저 스팟의 공용 클러스터 편입
- 보관함 MY 상태 배지와 작성자 비공개 북마크
- 검수 결과 스낵바, 보관 탭 인디케이터, 승인 완료 첫 진입 모달
- 공유 `StubSpotBackend` 기반 성공·지연·실패·철회/검수 경합 시나리오

범위 밖: NEW, 위치 중복 기준 변경, 추천 목록·알림, 팔로우·프로필·랭킹, 어드민 기능, 반려 외 일반 수정, 실제 Retrofit endpoint 구현.

## 2. 확정 정책

| 항목 | 결정 |
| --- | --- |
| 등록 직후 | `DRAFT`, 검수 없음 |
| 최초 신청 | `DRAFT → PENDING` |
| 재신청 | `REJECTED → RE_REVIEW_PENDING`, UI는 검수중 |
| 철회 | pending 계열 또는 rejected → `DRAFT` |
| 오픈 취소 | `PUBLISHED → DRAFT`, 추천·북마크 보존 |
| 삭제 | 상태 전이가 아닌 영구 삭제 |
| 추천 | 공개 스팟만, 셀프 추천 허용, 원 숫자 |
| 출처 | 상세만 표시 |
| 비공개 북마크 | 자동 삭제하지 않고 상태 카드 유지 |
| 탈퇴 | 자동 데이터 삭제 없음 |

## 3. Stub Service 매핑

| UI 동작 | Service 계약 | Stub 동작 |
| --- | --- | --- |
| MY 상세 | `MySpotService.detail` | 공유 backend snapshot 반환 |
| 신청·철회·취소·삭제 | `MySpotService` 전이 명령 | 원자적 상태 변경 및 최종 상태 반환 |
| 반려 편집·재신청 | `detail`, `reviseAndResubmit` | 기존 이미지 유지/교체 후 재검수 상태 |
| 추천·취소 | `RecommendationService` | 사용자 추천 관계와 최종 수 갱신 |
| 지도·리스트 | `SpotMapService`, `SpotListService` | 공유 공개 상태를 필터링 |
| 보관함 | `MySpotService`, `BookmarkService` | MY 상태와 비공개 bookmark 반환 |
| 결과 알림 | `ReviewResultService` | 처리 중·미확인 결과와 확인 여부 관리 |

HTTP endpoint, param, body, response, error code는 기능별 문서의 API 계약 입력란에만 기록한다. Integration 단계 전까지 Retrofit API에 가짜 endpoint를 추가하지 않는다.

## 4. 신규·수정 파일

신규 후보:

```text
core/services/protocols/RecommendationService.kt
core/services/protocols/ReviewResultService.kt
core/services/stub/StubSpotBackend.kt
core/services/stub/StubSpotFixtures.kt
core/services/stub/StubScenario.kt
core/services/stub/StubMySpotService.kt
core/services/stub/StubRecommendationService.kt
core/services/stub/StubSpotMapService.kt
core/services/stub/StubSpotListService.kt
core/services/stub/StubBookmarkService.kt
core/services/stub/StubReviewResultService.kt
feature/spotdetail/SpotOpenViewModel.kt
feature/spotdetail/SpotRecommendationViewModel.kt
```

주요 수정:

- `MySpot.kt`, `SpotDetail.kt`, `SavedSpot.kt`, `SpotMapService.kt`
- `SpotRegistrationViewModel.kt`, `ArchiveViewModel.kt`, `HomeMapViewModel.kt`
- Phase B 이후 `SpotDetailScreen.kt`, `SpotActionButtons.kt`, `SpotHeaderSection.kt`, `ArchiveScreen.kt`, `HomeScreen.kt`
- Stub과 Default를 동시에 바인딩하지 않도록 `ServiceModule.kt`의 개발 바인딩 전략 분리

## 5. 모델 가이드

- `MySpotStatus`: 다섯 상태를 손실 없이 표현한다.
- `MySpotTransitionResult`: `spotId`, 최종 상태, 갱신 시각.
- `RecommendationResult`: `spotId`, 최종 count, `isRecommended`.
- `SavedSpotAvailability`: `AVAILABLE`, `AUTHOR_PRIVATE`, `DELETED`.
- 지도 marker는 source, status, current-user ownership을 분리한다.
- 알 수 없는 서버 상태의 `PENDING` fallback은 금지한다.

## 6. ViewModel 책임

```kotlin
class SpotOpenViewModel(
    private val mySpotService: MySpotService,
) : ViewModel()

class SpotRecommendationViewModel(
    private val authService: AuthService,
    private val recommendationService: RecommendationService,
) : ViewModel()
```

- `SpotOpenViewModel`: 상세 상태, transition in-flight, 토스트, 삭제 완료 1회 이벤트.
- `SpotRecommendationViewModel`: 추천 여부·수, in-flight, 로그인 유도, 실패 롤백.
- `SpotRegistrationViewModel`: create/revise 모드, 반려 상세 프리필, 기존 이미지 유지.
- `ArchiveViewModel`: 비공개 카드 삭제 실패 시 원래 index 복원.
- `HomeMapViewModel`: `DRAFT`만 MY marker, `PUBLISHED`는 public cluster.
- 기존 `SpotDetailViewModel`과 `HomeMapViewModel`의 과도한 의존성을 더 늘리지 않는다.

## 7. 외부 연동

신규 외부 앱 연동은 없다. 기존 네이버 지도 열기, 공유 Intent, 잘못된 정보 제보는 회귀 대상이다.

## 8. 화면 정밀 사양

- 상세·카피: `docs/PV-41/02-spot-detail.md`
- 반려 편집: `docs/PV-41/03-revision-and-resubmit.md`
- 추천·출처: `docs/PV-41/04-recommendation-and-source.md`
- 지도·리스트: `docs/PV-41/05-map-and-list.md`
- 보관함: `docs/PV-41/06-archive.md`
- 결과 알림: `docs/PV-41/07-review-notification.md`

## 9. 에셋 입력 매트릭스 — Gate 4

> 2026-08-06 Figma MCP `get_design_context`로 아래 값을 확인했다. 기존 디자인 시스템 토큰과 값이 같은 항목은 반드시 재사용한다.

### 9.1 컬러

| 토큰 | Figma node | 확정 값 | 용도 | Figma 확인 |
| --- | --- | --- | --- | --- |
| `gray95` | 733:13130, 740:8444 | `#131416` | 화면·바텀시트·카드 배경 | 확정 — 기존 토큰 재사용 |
| `gray90` | 733:13924, 740:8463 | `#1E2124` | 반려 카드·팝업 | 확정 — 기존 토큰 재사용 |
| `gray0` | 733:13954, 729:11224 | `#FFFFFF` | 주 텍스트·보조 CTA·스낵바 | 확정 — 기존 토큰 재사용 |
| `gray30` | 747:7041, 733:13319 | `#B1B8BE` | 본문·출처 | 확정 — 기존 토큰 재사용 |
| `gray50` | 729:11199 | `#6D7882` | 비선택 하단 탭 | 확정 — 기존 토큰 재사용 |
| `gray80` | 733:13463, 729:11199 | `#33363D` | 보조 CTA 텍스트·하단 탭 border | 확정 — 기존 토큰 재사용 |
| `sunsetOrange` | 733:13954, 729:11224 | `#FA6133` | 주 CTA·추천 활성·인디케이터 | 확정 — 기존 토큰 재사용 |
| `rejectionOverlay` | 733:13924 | `#B83311` 12% over `gray90` | 반려 배너 tint | 확정 — `sunsetOrangeBg(#26FA6133)`가 아님; 신규 토큰 또는 inline alpha |

### 9.2 아이콘·이미지

| 에셋 | Figma node | 확정 형식/geometry | 용도 | Figma 확인 |
| --- | --- | --- | --- | --- |
| 추천 outline | 733:12759 | 신규 VectorDrawable/anydpi; outer `24×24dp`, leaf `22×20dp` (`1/1/1/3dp` inset) | 추천 전 | 확정 — Figma `ic_thumb_up` SVG export |
| 추천 filled | 733:12830 | 신규 VectorDrawable/anydpi; outer `24×24dp`, leaf `20×18dp` (`2/2/2/4dp` inset) | 추천 후 | 확정 — Figma `ic_thumb_up_alt` SVG export |
| 출처 globe | 747:7170 | 신규 VectorDrawable/anydpi; outer `16×16dp`, leaf `14×14dp` (`1dp` inset) | 유저 공개 스팟 출처 | 확정 — Figma `ic_global` SVG export |
| 반려 아이콘 | 733:13924 | 없음 | 반려 배너 | 확정 — Figma에 glyph가 없으므로 `ic_error_outline`을 넣지 않음 |
| 삭제 아이콘 | 733:13130 | 없음 | 삭제 바텀시트 | 확정 — 텍스트만 사용, 신규 export 없음 |
| 보관 탭·인디케이터 | 729:11199 | 기존 `ic_bookmark(_selected)` 재사용; 탭 item `114×64dp`, dot `4×4dp`, item 기준 `x=71dp`, `y=11dp` | 검수 결과 미확인 표시 | 확정 — dot은 `sunsetOrange` 원으로 draw, 신규 에셋 없음 |
| 결과 스낵바 close | 729:11224, 729:11253 | outer `20×20dp`, leaf 약 `12×12dp` (`4.17dp` inset) | 승인·반려 스낵바 닫기 | 확정 — Figma `ic_close` glyph 사용 |
| 지도 marker | 764:10482 | 기존 Compose marker 재사용; selected public `60×60dp`, `4dp` orange ring, 중앙 photo outer `20×20dp` | MY/public/selected | 확정 — 신규 marker 에셋 없음 |

### 9.3 타이포 후보

| 사용처 | 확정 토큰 | Figma 확인 |
| --- | --- | --- |
| 상세 화면 제목 | `headingLarge` | 확정 — 733:12998, 733:13331, 747:7041 (`24sp/600/1.2`) |
| 신청·철회·삭제 바텀시트 제목 | `headingMedium` | 확정 — 733:13319, 733:13463, 733:13130 (`22sp/600/1.2`) |
| 보관함 삭제 모달 제목 | `headingSmall` | 확정 — 740:8463 (`19sp/600/1.2`) |
| CTA | `bodyLargeBold` | 확정 — 733:13954, 733:13319, 740:8463 (`17sp/600/1.4`) |
| 상세 출처·추천 수·모달 본문 | `bodyMedium` | 확정 — 747:7041, 733:13319 (`15sp/400/1.4`) |
| 반려 사유·스낵바 제목/CTA | `bodyMediumBold` | 확정 — 733:13924, 729:11224 (`15sp/600/1.4`) |
| 반려 날짜·스낵바 본문·카드 메타 | `bodySmall` | 확정 — 733:13924, 729:11224, 740:8444 (`13sp/400/1.4`) |
| MY/검수 상태 뱃지·비공개 안내 | `bodySmallBold` | 확정 — 733:12998, 733:13331, 740:8444 (`13sp/600/1.3`) |
| 카드 거리 | `labelMedium` | 확정 — 740:8444 (`13sp/500/1.2`, letter `0.2sp`) |
| 하단 탭 | `labelSmall` | 확정 — 729:11199 (`12sp/500/1.2`, letter `0.2sp`) |

Gate 4 체크:

- [x] Figma MCP로 각 node의 실제 fill·stroke·opacity를 확인했다.
- [x] 추천 아이콘 glyph와 outer/leaf 크기를 확정했다.
- [x] 반려·삭제 아이콘이 Figma에 없으며 신규 export하지 않음을 확정했다.
- [x] 686:7273 아래 indicator(`729:11199`), snackbar(`729:11224`, `729:11253`), archive modal(`740:8463`) 하위 node를 식별했다.
- [x] 타이포 토큰을 각 컴포넌트에 하나로 확정했다.

## 10. TDD A→B→C 게이트

```text
Gate 4 에셋 확정
  ↓
Phase A — 도메인·Stub backend·ViewModel RED→GREEN, Composable 수정 0
  ↓
Phase B — docs/PV-41/ui-test-cases.md 8컬럼 작성 후 UI test RED
  ↓
Phase C — Paparazzi RED → Composable 구현 → GREEN → Figma 비교
```

각 Phase는 직렬이며 기능별로 A/B/C를 섞지 않는다. 같은 Phase 안에서 파일이 겹치지 않는 테스트 분석만 병렬화한다.

## 11. Figma 비교 노드

| 컴포넌트 | node-id |
| --- | --- |
| DRAFT / 검수중 / 반려 상세 | 733:12998 / 733:13331 / 733:13935 |
| 등록·보완 폼 | 747:7296 |
| 신청 / 철회 모달 | 733:13319 / 733:13463 |
| 반려 액션 / 배너 | 733:13922 / 733:13924 |
| 오픈 취소 / 삭제 | 733:13716 / 733:13130 |
| 추천 | 733:13954 |
| 출처 | 747:7041 |
| 보관 탭 인디케이터 | 729:11199 |
| 승인 / 반려 결과 스낵바 | 729:11224 / 729:11253 |
| 비공개 카드 / 저장 목록 삭제 모달 | 740:8444 / 740:8463 |

Figma MCP의 `get_design_context`, `get_screenshot`으로 file key `WLGPjrQtLqyhq46zXxvXHp`를 조회한다.

## 12. 디버그 진입

- 상태별 fixture와 다음 요청 실패·경합을 선택할 수 있는 개발 진입점을 `feature/debug/`에 제공한다.
- fixture 선택 API는 개발 전용이며 프로덕션 Service protocol에 노출하지 않는다.
- 앱 재시작 시 초기 fixture로 reset하는 인메모리 정책을 사용한다.

## 13. 논의 포인트

- Stub Hilt binding을 debug build에 한정하는 정확한 module 전략
- 상세 도메인을 `SpotService`와 `MySpotService` 중 어디서 단일화할지
- review result 확인 key와 서버 polling/push 방식
- 북마크 사전 안내 표시 빈도
- 공개 작성자 상세의 오픈 취소·삭제 액션 우선순위
- 비공개 카드 opacity와 접근성 라벨

### 13.1 Stub-first 구현 결정

- API 미배포 기간에는 `MySpotService`, `SpotListService`, `SpotMapService`, `BookmarkService`, `RecommendationService`, `ReviewResultService`를 공유 `StubSpotBackend` 구현에 각각 한 번만 바인딩한다. 실제 API 계약 확정 시 build variant별 module 분리가 필요하다.
- 공개 상세의 최종 서비스 경계는 `TBD`다. 현재 Stub에서는 `MySpotDetail` snapshot이 유저·큐레이션 fixture를 함께 표현하며, Retrofit endpoint·DTO·mapper는 추가하지 않았다.
- 검수 결과는 Stub `ReviewResultService.status()` 재조회 방식이다. polling/push와 서버 확인 key는 Integration 계약에서 확정한다.
- 승인 스낵바 확인과 승인 완료 모달 확인은 별도 상태로 보존한다. 스낵바를 확인한 뒤에도 최초 상세 모달이 남고, 모달 확인 후 제거된다.
- 비공개 북마크 카드는 Figma `740:8444`의 image 20%/meta 28% 의도를 따라 전체 카드 meta를 28%로 낮추고, `등록한 유저가\n비공개로 전환하였어요` 접근성 설명을 제공한다.
- 공개 작성자 상세 액션은 추천 → 오픈 취소 → 삭제 → 기존 피드백 순서로 배치한다.

## 14. 마감 체크리스트

- [x] Gate 4 에셋 매트릭스 실제 Figma 값 확정
- [x] Phase A JUnit5 전체 green (enum 확장에 필요한 기존 `when` exhaustiveness bridge 외 UI 동작 변경 없음)
- [x] Phase B `ui-test-cases.md` TODO 0, JUnit4 UI test 작성
- [x] Phase C PV-41 Paparazzi 17개 green, Figma 비교 1회
- [x] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:verifyPaparazziDebug` — PV-41 대상은 green, 기존 비대상 baseline 151개가 현재 렌더와 불일치
- [x] `./gradlew :app:assembleDebug`
- [x] API 미확정값은 기능별 계약 표에서만 `TBD`, 가짜 endpoint 0
- [x] Stub/Default 중복 Hilt binding 0
- [x] 논의 포인트 문서화

## 15. 작업 순서

```text
Figma MCP 재연결 → §9 실제 값 확정
→ Phase A 가이드 읽기 → Stub/VM TDD
→ Phase B 가이드 읽기 → ui-test-cases 작성·UI test
→ Phase C 가이드 읽기 → UI·snapshot·Figma 비교
→ 통합 검증
```
