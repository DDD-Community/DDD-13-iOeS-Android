# PV-41 — 상태별 스팟 상세와 오픈 액션

## 디자인 참조

### 상태별 상세

- [나만보기 MY 스팟](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-12998&t=7jXFETjPNUTJ0bx2-4)
- [검수중](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13331&t=7jXFETjPNUTJ0bx2-4)
- [반려](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13935&t=7jXFETjPNUTJ0bx2-4)
- [등록 화면](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=747-7296&t=7jXFETjPNUTJ0bx2-11)

### 확인 모달

- [오픈 신청](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13319&t=7jXFETjPNUTJ0bx2-4)
- [신청 철회](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13463&t=7jXFETjPNUTJ0bx2-4)
- [오픈 취소](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13716&t=7jXFETjPNUTJ0bx2-4)
- [스팟 삭제](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13130&t=7jXFETjPNUTJ0bx2-4)

> 구현 전 Figma MCP로 해당 노드의 최신 카피, 컴포넌트 속성, 간격과 상태를 확인한다.

## 화면 표시 규칙

| 상태 | 상단 표시 | 주요 CTA | 공개 기능 |
| --- | --- | --- | --- |
| `DRAFT` | MY 스팟 | 내 스팟 오픈하기 | 추천·피드백 없음 |
| `PENDING` / `RE_REVIEW_PENDING` | 검수중 | 신청 철회하기 | 추천·피드백 없음 |
| `REJECTED` | 반려 사유 배너 | 오픈 철회하기 / 내용 보완해서 다시 신청하기 | 추천·피드백 없음 |
| `PUBLISHED` 작성자 | 유저 등록 | 오픈 취소하기 / 삭제 | 추천·피드백 표시 |
| 큐레이션 | 출처 줄 | 기존 상세 액션 | 추천·피드백 표시 |

## 최초 오픈 신청

1. `DRAFT` 상세에서 **내 스팟 오픈하기**를 누른다.
2. 확인 모달을 표시한다.
   - 제목: `스팟을 오픈하면 다른 유저들도 볼 수 있어요`
   - 본문: `간단한 확인 절차를 거친 후 지도에 공개돼요. 확인 전까지는 나만 볼 수 있어요.`
   - 버튼: **오픈 신청하기** / 다음에요
3. 성공 시 `PENDING`으로 갱신하고 `오픈 신청이 접수되었어요` 토스트를 표시한다.
4. 보조 버튼, 바깥 탭, 뒤로가기는 모달만 닫는다.

## 신청 철회

1. 검수중 상세에서 **신청 철회하기**를 누른다.
2. 확인 모달을 표시한다.
   - 제목: `신청을 철회할까요?`
   - 본문: `철회하면 나만 볼 수 있는 상태로 돌아가요. 언제든 다시 신청할 수 있어요.`
   - 버튼: **신청 철회하기** / 계속 기다릴게요
3. 성공 시 `DRAFT`로 갱신한다.
4. 검수 결과가 먼저 확정됐다면 `이미 처리된 신청이에요`를 표시하고 최신 상태를 조회한다.

## 오픈 취소

- `PUBLISHED` 작성자 상세에서 **오픈 취소하기**를 제공한다.
- 모달 본문: `취소하면 나만 볼 수 있는 상태로 돌아가요. 좋아요 수는 그대로 유지되고, 다시 오픈하면 이어서 보여요.`
- 성공 시 `DRAFT`로 갱신하고 타 사용자의 지도·리스트에서 제거한다.
- 기존 추천 수와 추천 관계는 서버에 보존한다.

## 삭제

- 유저 등록 스팟에만 삭제 액션을 제공한다.
- Figma의 삭제 확인 모달을 거친 뒤 서버 삭제를 요청한다.
- 요청 중 중복 실행을 막고 실패 시 상세를 유지하며 공통 실패 토스트를 표시한다.
- 성공 시 상세를 닫고 MY 스팟 목록을 갱신한다.

## 실제 앱 파일 매핑

| 파일 | 변경 |
| --- | --- |
| `feature/spotdetail/SpotDetailScreen.kt` | 상태별 배너·모달·토스트와 최신 상세 재조회 |
| `feature/spotdetail/SpotDetailViewModel.kt` | 상태 명령, 요청 중 상태, 실패·경합 처리 |
| `feature/spotdetail/components/SpotActionButtons.kt` | `isMine` 이분법을 상태별 CTA 모델로 변경 |
| `feature/spotdetail/components/MySpotComingSoonSheet.kt` | 페이크도어 제거 후 정식 확인 모달로 대체 |
| `feature/spotdetail/components/SpotDetailModels.kt` | 상태·출처·반려·추천 정보 추가 |
| `feature/spotdetail/components/SpotDetailScreens.kt` | stateless 테스트 화면도 동일 상태 입력 사용 |

`notifyUpdateRequested()`와 `ShareFakedoorAnalyticsEvent` 사용을 제거한다. 모달 열림 여부는 Screen, 서버 변경 상태와 메시지는 ViewModel에 둔다.

`SpotDetailViewModel`은 현재 의존성 6개로 프로젝트 제한을 이미 넘는다. 추천과 MY 스팟 명령을 추가하기 전에 상세 전용 서비스로 책임을 묶거나 ViewModel을 분리해 의존성을 5개 이하로 맞춘다.

## Stub 구현

- `StubMySpotService`가 상세 조회, 오픈 신청, 신청 철회, 반려 철회, 오픈 취소, 삭제를 구현한다.
- 모든 명령은 공유 `StubSpotBackend`의 상태를 변경하고 실제 응답처럼 최종 상태를 반환한다.
- 지연 중 CTA 비활성화, 일반 실패, 철회/검수 경합을 선택 가능한 fixture로 제공한다.
- 삭제 성공 시 상세 조회가 not-found가 되고 MY 스팟 목록에서도 제거된다.

## API 계약 입력란

### 사용 API 목록

| 기능 | Method | Endpoint | Integration Status |
| --- | --- | --- | --- |
| 내 스팟 상세 조회 | `TBD` | `TBD` | `TBD` |
| 최초 오픈 신청 | `TBD` | `TBD` | `TBD` |
| 신청 철회 | `TBD` | `TBD` | `TBD` |
| 반려 상태 오픈 철회 | `TBD` | `TBD` | `TBD` |
| 공개 오픈 취소 | `TBD` | `TBD` | `TBD` |
| 유저 스팟 삭제 | `TBD` | `TBD` | `TBD` |

### 요청·응답 계약

| API 기능 | Path Param | Query Param | Request Body / Content-Type | Success Response | Error Codes / 앱 처리 |
| --- | --- | --- | --- | --- | --- |
| 내 스팟 상세 조회 | `TBD` | `TBD` | 없음 | `TBD` | `TBD` |
| 최초 오픈 신청 | `TBD` | `TBD` | `TBD` | 최종 상태 포함 `TBD` | 경합·중복 신청 `TBD` |
| 신청 철회 | `TBD` | `TBD` | `TBD` | 최종 상태 포함 `TBD` | 이미 처리됨 `TBD` |
| 반려 상태 오픈 철회 | `TBD` | `TBD` | `TBD` | `DRAFT` 결과 `TBD` | `TBD` |
| 공개 오픈 취소 | `TBD` | `TBD` | `TBD` | `DRAFT` 및 추천 보존 `TBD` | `TBD` |
| 유저 스팟 삭제 | `TBD` | `TBD` | `TBD` | 성공 status `TBD` | 권한·이미 삭제 `TBD` |

명세 확정 시 각 response에서 `status`, `rejectionReason`, `recommendationCount`, `isRecommended`, `source`, `updatedAt`의 타입과 nullable 여부를 함께 기록한다.
