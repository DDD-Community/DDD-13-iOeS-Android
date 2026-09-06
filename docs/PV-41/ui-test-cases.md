# PV-41 — UI 테스트 케이스

> Gate 4와 Phase A 완료 후 작성한 Phase B 단일 진실 소스다. 모든 테스트는 `app/src/test/`의 JUnit4 + Robolectric + Compose UI Test로 실행한다.

## 8컬럼 정의

| 열 | 의미 |
| --- | --- |
| ID | 변경되지 않는 테스트 식별자 |
| 화면 | 검증할 stateless content 또는 screen |
| 상태·fixture | Service/ViewModel 또는 직접 주입할 결정적 상태 |
| 사용자 동작 | 렌더 후 수행할 입력; 없으면 `없음` |
| 기대 UI | 사용자에게 보여야 하거나 숨겨야 하는 결과 |
| 기대 콜백·상태 | 호출 횟수·인자 또는 ViewModel 상태 변화 |
| testTag·접근성 | 우선 조회할 semantics tag와 content description |
| 테스트 메서드 | JUnit4 메서드명 |

## 상세·오픈·추천

| ID | 화면 | 상태·fixture | 사용자 동작 | 기대 UI | 기대 콜백·상태 | testTag·접근성 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| DTL-01 | `SpotOpenDetailContent` | `LoadState.Idle` | 없음 | 정적 로딩 표시 | 없음 | `spot-open-loading` | `idle_renders_loading` |
| DTL-02 | `SpotOpenDetailContent` | `LoadState.Loading` | 없음 | 정적 로딩 표시 | 없음 | `spot-open-loading` | `loading_renders_loading` |
| DTL-03 | `SpotOpenDetailContent` | `LoadState.Failed` | 없음 | 공통 실패 문구 | 없음 | `spot-open-error` | `failed_renders_error` |
| DTL-04 | `SpotOpenDetailContent` | `DRAFT`, MY 소유 | `내 스팟 오픈하기` 탭 | MY 스팟 뱃지, 오픈 신청 확인 시트 | `onRequestOpen`은 확인 전 0회 | `spot-status-draft`, `spot-action-request-open`, `spot-open-request-sheet` | `draft_opens_request_sheet` |
| DTL-05 | 오픈 신청 시트 | `DRAFT` | `오픈 신청하기` 탭 | 시트 닫힘 | `onRequestOpen` 1회 | `spot-open-request-confirm` | `request_sheet_confirms_once` |
| DTL-06 | `SpotOpenDetailContent` | `PENDING` | 없음 | `검수중`, `신청 철회하기`, 추천·신고 없음 | 없음 | `spot-status-pending`, `spot-action-withdraw-request` | `pending_renders_reviewing_actions` |
| DTL-07 | `SpotOpenDetailContent` | `RE_REVIEW_PENDING` | 없음 | `검수중`, `신청 철회하기`, 추천·신고 없음 | 없음 | `spot-status-re-review-pending`, `spot-action-withdraw-request` | `rereview_pending_matches_reviewing_ui` |
| DTL-08 | 신청 철회 시트 | pending 계열 | `신청 철회하기` 탭 | 시트 닫힘 | `onWithdrawRequest` 1회 | `spot-withdraw-request-sheet`, `spot-withdraw-request-confirm` | `withdraw_sheet_confirms_once` |
| DTL-09 | `SpotOpenDetailContent` | `REJECTED`, 사유 있음 | 없음 | 반려 배너와 철회·수정 후 재신청 두 CTA | 없음 | `spot-status-rejected`, `spot-rejection-banner`, `spot-action-withdraw-rejection`, `spot-action-revise` | `rejected_renders_reason_and_two_actions` |
| DTL-10 | 반려 철회 | `REJECTED` | `스팟 오픈 철회` 탭 후 확인 | DRAFT 복귀용 확인 UI | `onWithdrawRejection` 1회 | `spot-withdraw-rejection-sheet`, `spot-withdraw-rejection-confirm` | `rejected_withdraw_confirms_once` |
| DTL-11 | 반려 수정 | `REJECTED` | `수정 후 재신청` 탭 | 편집 화면 이동 | `onRevise` 1회, spot id 일치 | `spot-action-revise` | `rejected_revise_opens_prefilled_form` |
| DTL-12 | `SpotOpenDetailContent` | `PUBLISHED`, 작성자 | 없음 | 유저 공개 출처, 추천, 오픈 취소, 삭제, 신고 표시 | 없음 | `spot-source-user`, `spot-recommendation`, `spot-action-cancel-open`, `spot-action-delete`, `detail-report` | `published_owner_renders_public_actions` |
| DTL-13 | 공개 작성자 확인 시트 | `PUBLISHED` | 오픈 취소 확인 | 시트 닫힘 | `onCancelOpen` 1회 | `spot-cancel-open-sheet`, `spot-cancel-open-confirm` | `cancel_open_confirms_once` |
| DTL-14 | 공개 작성자 삭제 시트 | `PUBLISHED` | 삭제 확인 | 시트 닫힘 | `onDelete` 1회 | `spot-delete-sheet`, `spot-delete-confirm` | `delete_confirms_once` |
| DTL-15 | `SpotOpenDetailContent` | 큐레이션 공개, `한국관광공사` | 없음 | 출처명, 추천, 신고 표시; MY 상태 CTA 없음 | 없음 | `spot-source-curated`, `spot-recommendation`, `detail-report` | `curated_renders_source_recommendation_and_report` |
| DTL-16 | 추천 버튼 | 공개·미추천·로그인 | 추천 탭 | filled 상태와 count +1, 요청 중 disabled | `onToggleRecommendation` 1회 | `spot-recommendation`, `추천하지 않음` → `추천함` | `recommendation_toggles_optimistically` |
| DTL-17 | 추천 버튼 | 공개·비로그인 | 추천 탭 | 로그인 유도 팝업 | 추천 service 0회 | `spot-recommendation`, `spot-recommendation-login` | `recommendation_logged_out_requests_login` |
| DTL-18 | 승인 완료 모달 | 승인 결과, 최초 진입·미확인 | 닫기 | 모달 제거 후 상세 유지 | `onAcknowledgePublishedModal` 1회 | `spot-published-modal`, `spot-published-modal-confirm` | `published_modal_is_shown_once` |

## 반려 편집·재신청

| ID | 화면 | 상태·fixture | 사용자 동작 | 기대 UI | 기대 콜백·상태 | testTag·접근성 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REG-01 | `SpotRegistrationScreen` | CREATE 초기 | 없음 | 등록 제목·빈 입력·비활성 제출 | 없음 | `spotregistration-screen`, `registration-submit` | `create_initial_renders_empty_form` |
| REG-02 | `SpotRegistrationScreen` | REVISE loading | 없음 | 로딩 표시 | 없음 | `registration-revision-loading` | `revision_loading_renders_progress` |
| REG-03 | `SpotRegistrationScreen` | REVISE failed | 없음 | 편집 상세 실패 안내 | 없음 | `registration-revision-error` | `revision_failed_renders_error` |
| REG-04 | `SpotRegistrationScreen` | REJECTED 상세 프리필 | 없음 | 기존 이미지·이름·주소·테마·날짜·시간·코멘트, `다시 신청` | 없음 | `registration-existing-image`, `registration-name`, `registration-address`, `registration-submit` | `revision_loaded_prefills_all_fields` |
| REG-05 | 재신청 확인 시트 | 기존 이미지 유지, 유효 폼 | 제출 후 `신청하기` 탭 | 입력값 유지한 채 확인 시트 닫힘 | `reviseAndResubmit` 1회, replacement image `null` | `registration-resubmit-sheet`, `registration-resubmit-confirm` | `resubmit_keeps_existing_image` |
| REG-06 | 재신청 확인 시트 | 새 이미지 선택, 유효 폼 | `계속 수정할게요` 탭 | 시트만 닫히고 새 이미지·입력값 유지 | service 0회 | `registration-resubmit-cancel` | `resubmit_cancel_preserves_form` |

## 지도·리스트·보관함

| ID | 화면 | 상태·fixture | 사용자 동작 | 기대 UI | 기대 콜백·상태 | testTag·접근성 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MAP-01 | `HomeMapContent` | viewport loading | 없음 | 지도 로딩 상태 | 없음 | `homemap-loading` | `map_loading_renders_progress` |
| MAP-02 | `HomeMapContent` | viewport failed | 없음 | 지도 실패 안내 | 없음 | `homemap-error` | `map_failed_renders_error` |
| MAP-03 | marker content | owned `DRAFT` | 없음 | 회색 MY marker, cluster 제외 | 없음 | `map-marker-my-{id}`, `MY 스팟` | `draft_owner_renders_my_marker_only` |
| MAP-04 | marker content | user `PUBLISHED` selected | 없음 | 일반 public marker와 4dp orange ring, cluster 포함 | 없음 | `map-marker-public-{id}`, `선택된 공개 스팟` | `published_user_uses_public_selected_marker` |
| MAP-05 | list cell | user `PUBLISHED` | 없음 | 기존 공개 카드 유지, 출처 줄·뱃지 없음 | 없음 | `spot-list-cell-{id}` | `published_user_list_has_no_source_line` |
| ARC-01 | `ArchiveScreenContent` | saved loading | 없음 | 로딩 placeholder | 없음 | `archive-loading` | `archive_loading_renders_placeholder` |
| ARC-02 | `ArchiveScreenContent` | saved failed | 없음 | 실패 안내 | 없음 | `archive-failed` | `archive_failed_renders_message` |
| ARC-03 | MY 탭, 다섯 상태 | DRAFT/PENDING/RE_REVIEW_PENDING/REJECTED/PUBLISHED | 없음 | pending 2종은 `검수중`, rejected는 `반려됨`, 나머지는 무배지 | 없음 | `archive-my-badge-pending`, `archive-my-badge-re-review-pending`, `archive-my-badge-rejected` | `my_tab_renders_five_status_policy` |
| ARC-04 | 저장 카드 | `AUTHOR_PRIVATE` | 카드 탭 | 흐릿한 카드와 비공개 문구, 상세 이동 대신 삭제 모달 | `onCellClick` 0회 | `archive-private-{id}`, `archive-private-modal` | `private_saved_spot_opens_delete_modal` |
| ARC-05 | 비공개 삭제 모달 | `AUTHOR_PRIVATE` | `저장 목록에서 삭제` 탭 | 모달 닫힘·카드 낙관 제거 | `onBookmarkClick` 1회, id 일치 | `archive-private-delete-confirm` | `private_delete_confirms_once` |

## 검수 결과 알림

| ID | 화면 | 상태·fixture | 사용자 동작 | 기대 UI | 기대 콜백·상태 | testTag·접근성 | 테스트 메서드 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REV-01 | `HomeBottomNavigation` | 처리 중 신청 있음 | 없음 | 보관 탭 4dp orange indicator | 없음 | `home-saved-indicator`, `확인하지 않은 검수 결과 있음` | `pending_request_shows_saved_indicator` |
| REV-02 | `HomeBottomNavigation` | 미확인 승인 결과 있음 | 없음 | 보관 탭 indicator 유지 | 없음 | `home-saved-indicator` | `unseen_approval_keeps_indicator` |
| REV-03 | 승인 스낵바 | 승인 결과 | `바로 가기` 탭 | 공개 상세 이동 | `onOpenResult` 1회, spot id 일치 | `review-snackbar-approved`, `review-snackbar-action` | `approved_snackbar_opens_published_detail` |
| REV-04 | 반려 스낵바 | 반려 결과 | `확인하기` 탭 | 반려 상세 이동 | `onOpenResult` 1회, spot id 일치 | `review-snackbar-rejected`, `review-snackbar-action` | `rejected_snackbar_opens_rejected_detail` |
| REV-05 | 결과 스낵바 | 미확인 결과 | close 탭 | 스낵바만 숨김, indicator 유지 | acknowledge 0회 | `review-snackbar-close` | `closing_snackbar_does_not_acknowledge` |
| REV-06 | 하단 navigation | 결과 확인 완료·pending 0 | 없음 | indicator 없음 | 없음 | `home-saved-indicator` 미존재 | `acknowledged_result_hides_indicator` |

## 최소 상태 커버리지

- 상세: Idle, Loading, Loaded(다섯 MY 상태 + 큐레이션), Failed.
- 등록: CREATE 초기, REVISE Loading/Loaded/Failed.
- 지도: Loading, Loaded(DRAFT MY + PUBLISHED public), Failed.
- 보관함: Loading, Loaded(다섯 MY 상태 + AUTHOR_PRIVATE), Empty는 기존 회귀 테스트 유지, Failed.
- 알림: pending, unseen approved, unseen rejected, acknowledged.
