# PV-41 — 지도 핀·클러스터·탐색 리스트

## 노출 규칙

| 상태·출처 | 지도 | 탐색 리스트 | 클러스터 |
| --- | --- | --- | --- |
| 작성자의 `DRAFT` | 회색 MY 핀 | 미노출 | 제외 |
| `PENDING` / `RE_REVIEW_PENDING` | 미노출 | 미노출 | 제외 |
| `REJECTED` | 미노출 | 미노출 | 제외 |
| `PUBLISHED` 유저 스팟 | 일반 공개 핀 | 기존 공개 카드 | 공용 풀 포함 |
| 관리자 큐레이션 | 기존 오렌지 핀 | 기존 공개 카드 | 기존 풀 유지 |

- 공개 유저 스팟의 기본 핀은 일반 스팟과 같은 형태를 사용한다.
- 선택 시 기존 주황 링을 사용한다.
- 유저 공개 스팟 전용 모양이나 NEW 신호를 추가하지 않는다.
- 위치가 겹쳐도 별도 그룹화 기준을 도입하지 않는다.
- 탐색 리스트 카드에는 출처를 표시하지 않는다.

## 현재 구현과 변경

현재 `HomeMapViewModel`은 viewport 응답을 `isMySpot`으로 큐레이션과 MY 스팟으로 나눈다. V2에서는 작성자 소유 여부만으로 공개 상태를 추론할 수 없다.

| 파일 | 변경 |
| --- | --- |
| `core/services/protocols/SpotMapService.kt` | 마커 출처·공개 상태 모델 추가 |
| `core/network/dto/spot/SpotDtos.kt`, `SpotMapper.kt` | viewport의 출처·상태 필드 매핑 |
| `feature/map/HomeMapViewModel.kt` | `DRAFT` MY 핀과 공개 클러스터 입력을 상태 기반으로 분리 |
| `feature/map/NaverMapView.kt` | 회색 MY 마커 유지, 공개 유저 스팟을 기존 Clusterer 입력에 포함 |
| `feature/map/SpotDetailBottomSheet.kt` | 공개 유저 스팟을 큐레이션과 동일한 방식으로 열기 |
| `feature/spotlist/SpotListViewModel.kt` | 서버 공개 목록의 유저 스팟을 그대로 포함 |
| `feature/spotlist/components/SpotListCell.kt` | 기존 공개 카드 형태 유지 |

viewport 응답은 최소한 `spotId`, 좌표, 이미지, 소유 여부, 출처, 공개 상태를 제공해야 한다. 서버 공개 endpoint 자체가 비공개 상태를 제외하더라도 클라이언트 모델에는 출처를 보존한다.

## 갱신 시점

- 승인 결과 확인 후 지도와 리스트 데이터를 다시 불러온다.
- 작성자가 오픈 취소한 뒤 상세에서 복귀하면 현재 viewport를 다시 조회한다.
- 필터 변경과 카메라 idle 시 기존 viewport 조회 흐름을 유지한다.
- 선택 중인 스팟이 비공개로 바뀌면 바텀시트를 닫고 선택 상태를 초기화한다.

## Stub 구현

- `StubSpotMapService`와 `StubSpotListService`가 같은 `StubSpotBackend`를 조회한다.
- viewport Stub은 좌표 범위와 테마 필터를 적용하고 공개 스팟 및 작성자의 `DRAFT`만 반환한다.
- 리스트 Stub은 `PUBLISHED` 큐레이션·유저 스팟만 반환한다.
- 승인·오픈 취소 후 재조회하면 변경된 공유 상태가 즉시 반영된다.

## API 계약 입력란

| 기능 | Method | Endpoint | Integration Status |
| --- | --- | --- | --- |
| 지도 viewport 조회 | 기존/변경 계약 확인 | 기존/변경 endpoint | `TBD` |
| 공개 스팟 리스트 조회 | 기존/변경 계약 확인 | 기존/변경 endpoint | `TBD` |
| 지도 preview 조회 | 기존/변경 계약 확인 | 기존/변경 endpoint | `TBD` |

| API 기능 | Path Param | Query Param | Request Body | Success Response | Error Codes / 앱 처리 |
| --- | --- | --- | --- | --- | --- |
| viewport 조회 | 없음 | 네 꼭짓점, 테마, 소유 스팟 포함 조건 `TBD` | 없음 | 상태·출처·소유 여부가 포함된 marker 목록 `TBD` | 인증·좌표 validation `TBD` |
| 공개 리스트 | 없음 | page, sort, theme, 좌표 `TBD` | 없음 | 공개 유저 스팟 포함 page `TBD` | `TBD` |
| preview 조회 | `spotId` 형식 `TBD` | 사용자 좌표 `TBD` | 없음 | 공개 여부·출처가 포함된 preview `TBD` | 비공개 전환 `TBD` |

기존 endpoint가 서버에서 공개 상태만 필터링하는지, 작성자의 `DRAFT` MY 핀을 같은 viewport에서 내려주는지 또는 별도 API로 합칠지 명세에 기록한다.

## 테스트

- `DRAFT`는 작성자 지도에서 회색 MY 핀으로만 보인다.
- 검수중·반려는 작성자 지도에서도 보이지 않는다.
- 공개 유저 스팟은 기존 Clusterer에 포함된다.
- 선택 주황 링과 큐레이션 핀 회귀가 없다.
- 공개 취소 후 지도·리스트에서 즉시 제거된다.
- 리스트 카드에 출처 UI가 추가되지 않는다.
