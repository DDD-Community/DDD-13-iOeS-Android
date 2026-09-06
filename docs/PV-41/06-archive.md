# PV-41 — 보관함과 MY 스팟 목록

## 디자인 참조

- [비공개 북마크 카드·삭제 안내](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=686-7273&p=f&t=7jXFETjPNUTJ0bx2-11)

> 구현 전 Figma MCP로 비공개 카드의 opacity, 상태 문구, 탭 안내와 삭제 버튼 속성을 확인한다.

## MY 스팟 탭

| 상태 | 카드 표시 |
| --- | --- |
| `DRAFT` | 일반 MY 스팟 카드, 상태 배지 없음 |
| `PENDING` | 검수중 배지 |
| `RE_REVIEW_PENDING` | 검수중 배지 |
| `REJECTED` | 반려됨 배지 |
| `PUBLISHED` | 일반 카드, 상태 배지 없음 |

카드를 누르면 기존 스팟 상세로 이동한다. 상태별 액션은 목록이 아니라 상세에서 제공한다.

현재 `ArchiveScreen.MySpotStatusBadge`의 `PENDING` 문구는 **검토중**이다. 기획 확정 표현인 **검수중**으로 변경하고 `RE_REVIEW_PENDING`에도 같은 배지를 사용한다.

## 북마크 사전 안내

- 공개 유저 스팟을 북마크할 때 `작성자가 언제든 비공개로 전환할 수 있어요`를 짧게 안내한다.
- 큐레이션 스팟에는 이 안내를 표시하지 않는다.
- 안내 표시 빈도는 서버·기획 계약이 없으면 해당 북마크 성공 시점마다 표시하는 것으로 시작한다.

## 비공개 전환 카드

- 작성자가 오픈 취소한 스팟을 보관함에서 자동 삭제하지 않는다.
- 흐릿한 카드와 **비공개로 전환됨** 상태를 표시한다.
- 카드 탭 시 상세로 이동하지 않고 짧은 안내를 표시한다.
- 안내에는 **목록에서 삭제** 버튼만 제공한다.
- 바깥 탭과 뒤로가기로 안내를 닫을 수 있다.
- 삭제 실패 시 카드를 원래 위치에 복구하고 실패 토스트를 표시한다.

현재 `SavedSpot.deleted`는 운영 삭제를 의미한다. 작성자 비공개 상태를 같은 값으로 합치지 않고 별도 상태 필드로 추가한다.

## 파일 매핑

| 파일 | 변경 |
| --- | --- |
| `core/services/protocols/SavedSpot.kt` | 운영 삭제와 작성자 비공개 상태 분리, 유저 등록 여부 추가 |
| `BookmarkDtos.kt`, `BookmarkMapper.kt` | 신규 상태 필드 매핑 |
| `feature/archive/ArchiveViewModel.kt` | 다섯 MY 상태, 비공개 북마크 삭제와 실패 복구 |
| `feature/archive/ArchiveScreen.kt` | 검수중·반려 배지, 비공개 카드, 삭제 안내 |
| `feature/spotlist/components/SpotListCell.kt` | 비공개 시각 상태와 접근성 설명을 선택적으로 지원 |

## Stub 구현

- `StubMySpotService.list()`가 공유 스팟의 모든 MY 상태를 반환한다.
- `StubBookmarkService.savedSpots()`는 공개 및 비공개 전환 항목을 함께 반환한다.
- 비공개 항목 삭제는 북마크 관계만 제거하고 원본 스팟을 삭제하지 않는다.
- 오픈 취소 후 보관함을 다시 조회하면 같은 항목이 비공개 상태로 남는다.

## API 계약 입력란

| 기능 | Method | Endpoint | Integration Status |
| --- | --- | --- | --- |
| MY 스팟 목록 | 기존/변경 계약 확인 | 기존/변경 endpoint | `TBD` |
| 저장한 스팟 목록 | 기존/변경 계약 확인 | 기존/변경 endpoint | `TBD` |
| 비공개 항목 목록에서 삭제 | 기존 북마크 취소 계약 확인 | 기존/변경 endpoint | `TBD` |

| API 기능 | Path Param | Query Param | Request Body | Success Response | Error Codes / 앱 처리 |
| --- | --- | --- | --- | --- | --- |
| MY 스팟 목록 | 없음 | page, 좌표 `TBD` | 없음 | 다섯 상태·반려 사유 포함 page `TBD` | `TBD` |
| 저장한 스팟 목록 | 없음 | page, 좌표 `TBD` | 없음 | 운영 삭제·작성자 비공개·유저 등록 구분 `TBD` | `TBD` |
| 비공개 항목 삭제 | `spotId` 형식 `TBD` | `TBD` | `TBD` | 갱신 카운트 또는 성공 status `TBD` | 이미 삭제·권한 `TBD` |

작성자 비공개 항목에 이름·이미지 등 기존 snapshot을 계속 내려주는지, 비공개 상태에서도 북마크 식별자를 유지하는지 명세에 기록한다.

## 테스트

- `PENDING`, `RE_REVIEW_PENDING` 모두 검수중으로 표시된다.
- 운영 삭제와 작성자 비공개가 별도 상태로 매핑된다.
- 비공개 카드는 흐릿하며 상세로 이동하지 않는다.
- 안내에는 삭제 버튼 하나만 존재한다.
- 삭제하지 않으면 카드가 계속 보관함에 남는다.
- 삭제 실패 시 카드가 원래 위치에 복구된다.
