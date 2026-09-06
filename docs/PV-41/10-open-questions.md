# PV-41 — 기획 대조 미해결 항목

> 기획 문서(V2 유저 스팟 오픈)와 구현(`feature/PV-41`)을 1:1 대조하며 나온 항목이다.
> 답이 나오면 이 문서에서 지우고 해당 기능 문서(02~07)와 코드에 반영한다.
> 대조 기준 커밋: `89c19ca`

## 경로 규칙

이 문서의 표에 쓰는 경로 축약이다.

| 표기 | 실제 경로 |
| --- | --- |
| `main/…` | `app/src/main/java/com/pickflow/android/…` |
| `test/…` | `app/src/test/java/com/pickflow/android/…` |
| `snap/…` | `app/src/test/snapshots/images/com.pickflow.android.feature.…png` |

---

## A. 결정 완료

### A1. 반려 상태의 "오픈 철회하기" — **로컬 배너 닫기로 재정의** (2026-08-22 확정, 2026-08-23 반영 완료)

기획 3.5 는 반려 시 버튼 2개("오픈 철회하기" / "내용 보완해서 다시 신청하기")를 요구한다.
그러나 서버에는 대응 엔드포인트가 없고, 반려 상태에서 해제를 시도하면 `400 SP009`(해제할 대상 없음)가 떨어진다.
서버 관점에서 반려 = 이미 나만보기(DRAFT) 상태라 바꿀 상태가 없기 때문이다.

**결정**: 버튼은 기획대로 유지하되, **서버를 호출하지 않고 반려 사유 배너만 로컬에서 닫는 동작**으로 재정의한다.
유저의 실제 의도가 "반려 배너를 치우고 싶다"이고 서버 상태는 이미 DRAFT 이므로 전이가 불필요하다.

**반영 내역**

| 작업 | 위치 |
| --- | --- |
| `MySpotService.withdrawRejection()` 제거 | `main/core/services/protocols/MySpot.kt` |
| Stub 구현·backend 전이 제거, 쓰이지 않게 된 `clearRejection` 파라미터 정리 | `main/core/services/stub/StubMySpotService.kt`, `StubSpotBackend.kt`, `StubScenario.kt` |
| `SpotOpenViewModel.withdrawRejection()` 제거 — 서버 호출이 아예 없어졌다 | `main/feature/spotdetail/SpotOpenViewModel.kt` |
| 배너 dismiss 를 `SpotOpenDetailContent` 내부 상태로 보유(`activeSheet` 과 같은 범주). 스팟이 바뀌면 초기화 | `main/feature/spotdetail/SpotOpenDetailContent.kt` |
| 확인 모달(`SpotOpenSheet.WITHDRAW_REJECTION`) 제거 | `main/feature/spotdetail/SpotOpenDetailContent.kt` |
| 테스트 갱신 | `test/feature/spotdetail/SpotOpenScreenUiTest.kt`, `SpotOpenViewModelTest.kt`, `test/core/services/stub/StubSpotBackendTest.kt` |

**dismiss 범위: 세션 한정** (2026-08-23 확정). 화면을 벗어났다 다시 들어오면 배너가 복귀한다.
반려 사유는 재신청 전까지 계속 보여야 할 정보라, 영구 저장하면 유저가 사유를 잃는다.

**확인 필요 — 확인 모달을 없앴다.**
기존 모달 카피 "스팟 오픈을 철회할까요? / 철회하면 나만 볼 수 있는 상태로 돌아가요"는
상태가 실제로 바뀌지 않는 지금 동작에서는 사실과 다르다. 배너를 닫는 데 확인을 받는 것도 과하다.
그래서 버튼을 누르면 곧바로 배너가 사라지게 했다. **기획 확정이 필요하다.**
모달을 유지해야 한다면 카피를 새로 받아야 한다.

버튼 라벨 "오픈 철회하기"는 기획 3.5 대로 두었으나, 철회할 대상이 없는 동작이라
라벨도 재검토 대상이다(예: "확인했어요").

**관련 자료**

| 종류 | 경로 |
| --- | --- |
| 스냅샷 | `snap/spotdetail_SpotOpenSnapshotTest_rejected.png` |
| 스냅샷 테스트 | `test/feature/spotdetail/SpotOpenSnapshotTest.kt` |
| UI 테스트 | `test/feature/spotdetail/SpotOpenScreenUiTest.kt` |

---

## B. 기획 확인 대기

### B1. 3.7 표 "리스트 카드 / 나만보기 = 본인 화면에서만 노출"의 대상 화면

**탐색 리스트**(지도 옆 리스트 탭)에 내 나만보기 스팟이 뜬다는 뜻인가, **보관함 MY 스팟 탭**을 말하는 것인가.

현재 구현은 후자다. 탐색 리스트는 공개 스팟만 싣고, 내 DRAFT 는 보관함에만 나온다.
지도에서는 내 DRAFT 가 회색 핀으로 보이는데 리스트에서는 안 보이는 비대칭이 의도인지 확인이 필요하다.

참고로 `SpotOpenListCell` 은 셀 형태 유지를 증명하는 스냅샷·UI 테스트에서만 쓰이고, 실제 탐색 리스트는 기존 `SpotListScreen` 을 그대로 쓴다. 공개된 유저 스팟이 리스트에 실리는지는 서버 `GET /v1/spots` 응답에 달려 있어 클라이언트 변경이 필요 없을 수 있다.

| 종류 | 경로 |
| --- | --- |
| 코드 (지도 partition) | `main/feature/map/HomeMapViewModel.kt:141-168` |
| 코드 (리스트 셀) | `main/feature/map/SpotOpenMapContent.kt:166` |
| 코드 (실제 탐색 리스트) | `main/feature/spotlist/SpotListScreen.kt` |
| 스냅샷 | `snap/map_SpotOpenMapSnapshotTest_map_owned_draft_and_selected_public.png` |
| 스냅샷 | `snap/map_SpotOpenMapSnapshotTest_published_user_list_cell.png` |
| 스냅샷 | `snap/map_SpotOpenMapSnapshotTest_curated_list_cell_keeps_public_card_shape.png` |
| 스냅샷 | `snap/archive_ArchiveSnapshotTest_archive_myspots_loaded_mixed_status_dark.png` |
| 스냅샷 테스트 | `test/feature/map/SpotOpenMapSnapshotTest.kt`, `test/feature/archive/ArchiveSnapshotTest.kt` |
| 단위 테스트 | `test/feature/map/HomeMapViewportPartitionTest.kt` |

### B2. 오픈 취소 후 작성자 본인 화면의 지도 표시

기획 3.6 은 "지도/리스트에서 (타 유저 기준) 즉시 제거"라고만 적혀 있다. 작성자 본인 화면 규칙이 없다.

현재 구현은 오픈 취소 시 DRAFT 로 돌아가므로, **본인 지도에는 회색 MY 핀으로 다시 나타난다.** 의도와 같은지 확인이 필요하다.

| 종류 | 경로 |
| --- | --- |
| 코드 | `main/feature/map/HomeMapViewModel.kt:147-158` |
| 스냅샷 | `snap/map_SpotOpenMapSnapshotTest_map_owned_draft_and_selected_public.png` |
| 스냅샷 | `snap/spotdetail_SpotOpenSnapshotTest_draft.png` |
| 스냅샷 테스트 | `test/feature/map/SpotOpenMapSnapshotTest.kt`, `test/feature/spotdetail/SpotOpenSnapshotTest.kt` |

### B3. 검수 결과 스낵바의 노출 정책

기획 3.4 는 "지도/리스트 화면에서 스낵바로 결과 안내"까지만 정의한다. 노출 빈도와 다건 처리 규칙이 없다.

현재 구현은 이렇다.

- 미확인 결과가 있으면 **홈 진입 시마다** 노출된다.
- 유저가 액션(바로 가기 / 닫기)을 눌러야 확인 처리되어 사라진다.
- 결과가 여러 건이면 **가장 최신 1건만** 보여준다.

앱 세션당 1회로 제한할지, 다건일 때 순차 노출할지 확인이 필요하다.

| 종류 | 경로 |
| --- | --- |
| 코드 (노출 조건) | `main/feature/home/HomeScreen.kt` |
| 코드 (상태) | `main/feature/home/ReviewResultViewModel.kt:104-108` |
| 코드 (스낵바 UI) | `main/feature/home/HomeReviewResultComponents.kt:131` |
| 계약 | `main/core/services/protocols/ReviewResultService.kt:14-21` |
| 스냅샷 | `snap/home_HomeReviewResultSnapshotTest_review_result_snackbar_approved.png` |
| 스냅샷 | `snap/home_HomeReviewResultSnapshotTest_review_result_snackbar_rejected.png` |
| 스냅샷 | `snap/home_HomeReviewResultSnapshotTest_home_bottom_navigation_saved_indicator.png` |
| 스냅샷 테스트 | `test/feature/home/HomeReviewResultSnapshotTest.kt` |
| 단위 테스트 | `test/feature/home/ReviewResultViewModelTest.kt` |

> 이 영역 전체가 서버 계약 미확정이다. 대응 엔드포인트가 서버 문서에 없어 `09-api-mapping.md` C 항목으로도 걸려 있다.

---

## C. 기획 문서에 없는데 구현에 있는 것

### C1. 반려 사유 5종과 배너 문구 출처

기획에는 반려 사유의 문구 규격이 없다. 서버가 `DUPLICATE / LOW_QUALITY / LOCATION_MISMATCH / FILTER_MISMATCH / ETC` 5종으로 확정했고(`ETC` 는 `detail` 필수), 배너는 서버 `guideMessage` 를 우선 쓰고 없으면 `reasonLabel` 로 대체한다.

스텁의 고정 문구(`StubRejections`)는 화면 검증용이며 프로덕션에서는 서버 값을 그대로 쓴다. **기획이 원하는 톤과 서버 `guideMessage` 문구가 일치하는지 확인이 필요하다.**

| 종류 | 경로 |
| --- | --- |
| 코드 | `main/core/services/protocols/MySpot.kt` (`SpotRejection`, `RejectionReason`) |
| 코드 (배너) | `main/feature/spotdetail/SpotOpenDetailContent.kt:318-330` |
| 스텁 문구 | `main/core/services/stub/StubSpotFixtures.kt` (`StubRejections`) |
| 스냅샷 | `snap/spotdetail_SpotOpenSnapshotTest_rejected.png` |
| 스냅샷 테스트 | `test/feature/spotdetail/SpotOpenSnapshotTest.kt` |

### C2. "스팟을 비공개로 전환했어요" 토스트 — **미확정 임시 카피**

서버가 신청 철회와 오픈 취소를 `DELETE .../publications` 하나로 처리하고 `previousStatus` 로 구분한다.
이에 맞춰 결과 문구를 두 갈래로 나눴다.

- `previousStatus` 가 PENDING·RE_REVIEW_PENDING → `오픈 신청을 철회했어요`
- `previousStatus` 가 PUBLISHED → `스팟을 비공개로 전환했어요`

**후자는 기획 확정 카피가 아니라 임시로 지은 문구다.** 기획 3.6 은 오픈 취소 확인 모달 카피만 정의하고 완료 토스트를 정의하지 않았다.

| 종류 | 경로 |
| --- | --- |
| 코드 | `main/feature/spotdetail/SpotOpenViewModel.kt` |
| 계약 | `main/core/services/protocols/MySpot.kt` (`MySpotUnpublishResult`) |
| 스냅샷 | `snap/spotdetail_SpotOpenSnapshotTest_publishedOwner.png` |

### C3. 승인 스낵바 액션 카피 "바로 가기"

기획 3.4 확정본은 **"바로가기"**(붙여쓰기)인데 구현은 **"바로 가기"**(띄어쓰기)다. 어느 쪽으로 통일할지 확인이 필요하다.

| 종류 | 경로 |
| --- | --- |
| 코드 | `main/feature/home/HomeReviewResultComponents.kt` |
| 스냅샷 | `snap/home_HomeReviewResultSnapshotTest_review_result_snackbar_approved.png` |

### C4. 보관함 비공개 안내 시트의 제목·본문

기획 3.9 는 "짧은 안내 + 목록에서 삭제 버튼 하나"까지만 정의한다. 실제 문구는 구현에서 지었다.

- 제목 `비공개로 전환된 스팟이에요`
- 본문 `작성자가 스팟을 비공개로 전환했어요.\n목록에서 삭제할 수 있어요.`
- 카드 오버레이 `등록한 유저가\n비공개로 전환하였어요`

| 종류 | 경로 |
| --- | --- |
| 코드 | `main/feature/archive/ArchiveScreen.kt:491, 544-565` |
| 스냅샷 | `snap/archive_ArchiveSnapshotTest_archive_private_saved_spot_dark.png` |
| 스냅샷 | `snap/archive_ArchiveSnapshotTest_archive_private_delete_dialog_dark.png` |
| 스냅샷 테스트 | `test/feature/archive/ArchiveSnapshotTest.kt` |
| UI 테스트 | `test/feature/archive/ArchiveSpotOpenScreenUiTest.kt` |

---

## D. 기획에 있으나 미구현

### D1. 북마크 시점 사전 안내 — **미구현**

기획 3.9 첫 항목이다.

> 유저오픈 스팟 북마크 시점에 작은 사전 안내 노출: "작성자가 언제든 비공개로 전환할 수 있어요"

보관함 쪽 사후 안내(C4)만 구현했고, **북마크를 누르는 순간의 사전 안내가 빠졌다.**
노출 위치(상세페이지 북마크 버튼 / 지도 카드 / 리스트 카드)와 형태(토스트 / 툴팁 / 1회성 여부)가 기획에 명시되지 않아 착수 전 확인이 필요하다.

| 종류 | 경로 |
| --- | --- |
| 북마크 진입점 | `main/feature/spotdetail/SpotDetailScreen.kt:145-147` |
| 계약 | `main/core/services/protocols/BookmarkService.kt` |
| 스냅샷 | 없음 (미구현) |

### D2. 승인 스낵바 카피 (C3 과 동일 건)

C3 참조. 미구현이 아니라 표기 불일치다.
