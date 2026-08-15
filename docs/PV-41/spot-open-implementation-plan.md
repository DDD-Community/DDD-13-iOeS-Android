# PV-41 — 유저 스팟 오픈 기능 Android 구현 계획

> PV-41의 전체 인덱스다. 세부 명세는 기능·화면별 문서를 기준으로 구현한다.

## 목표

등록 직후 MY 스팟은 검수 없이 나만보기(`DRAFT`)로 저장한다. 작성자가 오픈을 신청한 시점부터 검수를 시작하고, 승인된 유저 스팟만 공용 지도와 리스트에 공개한다.

## 개발 전략 — Stub First, API Later

현재 API 명세와 서버 배포가 준비되지 않았으므로 UI와 클라이언트 기능을 **스텁 우선**으로 구현한다.

1. 도메인 모델과 Service `interface`를 먼저 확정한다.
2. `@Singleton` 인메모리 스텁으로 상태 전이, 추천, 지도·보관함 반영과 오류 시나리오를 구현한다.
3. ViewModel·Compose UI·테스트는 Service 인터페이스만 의존한다.
4. API 명세가 확정되면 각 기능 문서의 **API 계약 입력란**에 endpoint, param, body, response, error code를 기록한다.
5. DTO·API·Mapper·`Default*Service`를 구현한 뒤 Hilt 바인딩을 Stub에서 Default로 교체한다.

스텁은 화면별 고정 데이터가 아니라 여러 Service 구현이 공유하는 `StubSpotBackend`를 사용한다. 신청·승인·오픈 취소가 상세, 지도, 리스트, 보관함에 함께 반영되어 실제 연동과 같은 데이터 흐름을 검증하기 위함이다. `StubSpotBackend`는 개발 전용 인메모리 상태 저장소이며 프로덕션 Repository 계층으로 승격하지 않는다.

## 문서 구성

| 문서 | 내용 |
| --- | --- |
| [01-state-and-api.md](01-state-and-api.md) | 상태 모델, 전이, 서버·클라이언트 계약 |
| [02-spot-detail.md](02-spot-detail.md) | 상태별 상세, 신청·철회·오픈 취소·삭제 |
| [03-revision-and-resubmit.md](03-revision-and-resubmit.md) | 반려 사유, 기존 값 수정, 재신청 |
| [04-recommendation-and-source.md](04-recommendation-and-source.md) | 추천, 출처, 피드백 링크 |
| [05-map-and-list.md](05-map-and-list.md) | 지도 핀, 클러스터, 탐색 리스트 |
| [06-archive.md](06-archive.md) | MY 스팟 목록, 비공개 북마크 |
| [07-review-notification.md](07-review-notification.md) | 검수 결과 스낵바, 탭 인디케이터, 승인 모달 |
| [08-test-and-rollout.md](08-test-and-rollout.md) | 구현 순서, TDD, QA 완료 기준 |
| [09-api-mapping.md](09-api-mapping.md) | **서버 API ↔ 클라이언트 구현 매핑, 조정 필요 항목, 잔여 미확정 계약** |

## 범위

### In Scope

- 오픈 신청·철회, 반려 후 보완·재신청, 공개 후 오픈 취소
- 유저 등록 스팟 삭제
- 반려 사유, 출처, 추천, 피드백 링크
- 공개 유저 스팟의 지도·리스트·클러스터 편입
- 비공개로 전환된 북마크의 보관함 상태 유지
- 검수 결과 안내와 보관 탭 인디케이터
- 오픈 취소·재오픈 후 추천 수 보존
- 작성자 탈퇴 시 기존 복구 정책에 따른 데이터 유지

### Out of Scope

- NEW 배지, 중복 위치 그룹화 기준 변경
- 추천 전용 목록·알림
- 팔로우, 작성자 프로필, 공개 스팟 랭킹·정렬 고도화
- 어드민 검수 정책·화면 및 어드민 승인 취소
- 일반 스팟 수정. 단, 반려 후 보완·재신청은 예외다.

## 구현 순서

1. [상태·API 계약](01-state-and-api.md)
2. [작성자 상세 상태 전환](02-spot-detail.md)
3. [반려 수정·재신청](03-revision-and-resubmit.md)
4. [공개 상세·추천·출처](04-recommendation-and-source.md)
5. [지도·리스트 공개 노출](05-map-and-list.md)
6. [보관함](06-archive.md) 및 [검수 결과 알림](07-review-notification.md)
7. [통합 테스트·릴리스 검증](08-test-and-rollout.md)

각 기능 묶음은 프로젝트 규칙에 따라 Phase A(ViewModel) → Phase B(Compose UI) → Phase C(Paparazzi) 순으로 진행한다.

API 연동 전에는 스텁 테스트를 완료 기준으로 삼고, 서버 배포 후에는 동일 테스트 계약을 `Default*Service`와 실제 응답 fixture에 재사용한다.

## 확정 사항

- 서버 상태는 `DRAFT`, `PENDING`, `REJECTED`, `RE_REVIEW_PENDING`, `PUBLISHED`를 사용한다.
- `PENDING`, `RE_REVIEW_PENDING`은 앱에서 모두 **검수중**으로 표시한다.
- 반려 상태의 **오픈 철회하기**는 삭제가 아니라 `DRAFT` 복귀다.
- 출처는 상세페이지만 표시한다.
- 기획 문구의 좋아요와 추천은 같은 기능이며 앱에서는 **추천**으로 통일한다.
- 추천 수는 축약하지 않는다.
- 작성자 탈퇴만으로 스팟·추천 데이터를 자동 삭제하지 않는다.
