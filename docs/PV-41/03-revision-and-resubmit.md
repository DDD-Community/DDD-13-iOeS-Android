# PV-41 — 반려 보완과 재신청

## 디자인 참조

- [반려 상세](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13935&t=7jXFETjPNUTJ0bx2-4)
- [반려 사유 배너](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13924&t=7jXFETjPNUTJ0bx2-11)
- [오픈 철회·재신청](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13922&t=7jXFETjPNUTJ0bx2-4)
- [등록 폼](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=747-7296&t=7jXFETjPNUTJ0bx2-11)

> 구현 전 Figma MCP로 배너와 재신청 모달의 최신 카피, 색상, 간격과 버튼 우선순위를 확인한다.

## 반려 상세

- `REJECTED` 상세 상단에 서버가 내려준 반려 사유를 표시한다.
- **오픈 철회하기**, **내용 보완해서 다시 신청하기** 두 액션을 함께 제공한다.
- **오픈 철회하기**는 스팟을 삭제하지 않고 `DRAFT`로 복귀시킨다.
- 성공 후 반려 사유와 재신청 CTA를 숨기고 일반 나만보기 상세를 표시한다.

## 보완 폼

- 기존 `SpotRegistrationScreen`을 편집 모드로 재사용한다.
- 사진, 스팟명, 주소·좌표, 테마, 촬영일·시간, 코멘트를 기존 값으로 채운다.
- 일반 등록과 달리 기존 서버 이미지를 유지한 채 다른 필드만 수정할 수 있어야 한다.
- 사진을 새로 고른 경우에만 교체용 `ImagePayload`를 전송한다.
- 일반적인 공개·나만보기 상태에서는 수정 진입점을 제공하지 않는다.

현재 등록 요청은 주소를 서버에 전송하지 않는다. 주소를 프리필하려면 편집 상세 응답에 주소 표시값과 좌표가 포함되어야 한다. 원격 이미지 URL을 다시 다운로드해 `ImagePayload`로 만들지 않고, 서버 계약에서 기존 이미지 유지와 새 이미지 교체를 구분한다.

## 재신청 확인

1. 편집 폼 상단 제출 버튼을 누른다.
2. 확인 모달을 표시한다.
   - 제목: `다시 신청할까요?`
   - 본문: `제출하면 검수가 다시 시작돼요.`
   - 버튼: **신청하기** / 계속 수정할게요
3. 성공 시 변경 내용을 저장하고 `RE_REVIEW_PENDING`으로 갱신한다.
4. **계속 수정할게요**, 바깥 탭, 뒤로가기는 모달만 닫고 폼 값을 유지한다.
5. 실패 시 편집 폼과 입력값을 유지하고 공통 실패 토스트를 표시한다.

## 내비게이션과 파일 매핑

| 파일 | 변경 |
| --- | --- |
| `feature/spotregistration/SpotRegistrationViewModel.kt` | 신규 등록/반려 편집 모드, 상세 프리필, 수정 재신청 분기 |
| `feature/spotregistration/SpotRegistrationScreen.kt` | 편집 제목·제출 버튼, 기존 이미지 표시, 재신청 모달 |
| `app/navigation/PickflowRoute.kt` | 선택적 `editSpotId` 또는 별도 재신청 라우트 |
| `app/navigation/PickflowNavHost.kt` | 반려 상세 → 편집 폼 → 성공 후 상세 복귀 연결 |

주소 검색과 장소 상세는 현재 등록 ViewModel의 back stack 공유 방식을 그대로 사용한다. 제출 성공 후 새 상세를 중복 push하지 않고 기존 반려 상세로 복귀해 최신 상태를 다시 로드한다.

## Stub 구현

- `StubMySpotService.detail()`이 편집에 필요한 모든 기존 값을 반환한다.
- `reviseAndResubmit()`은 교체 이미지가 `null`이면 기존 이미지를 유지하고, 값이 있으면 교체한다.
- 성공 시 공유 스팟을 `RE_REVIEW_PENDING`으로 변경하고 반려 사유를 제거한다.
- 실패 fixture는 입력한 폼 값을 유지하며 백엔드 상태를 변경하지 않는다.

## API 계약 입력란

| 기능 | Method | Endpoint | Integration Status |
| --- | --- | --- | --- |
| 편집용 반려 상세 조회 | `TBD` | `TBD` | `TBD` |
| 내용 수정·재신청 | `TBD` | `TBD` | `TBD` |

| API 기능 | Path Param | Query Param | Request Body / Content-Type | Success Response | Error Codes / 앱 처리 |
| --- | --- | --- | --- | --- | --- |
| 편집용 반려 상세 조회 | `TBD` | `TBD` | 없음 | 사진·이름·주소·좌표·테마·촬영정보·코멘트·반려 사유 `TBD` | 권한·상태 불일치 `TBD` |
| 내용 수정·재신청 | `TBD` | `TBD` | multipart/JSON 여부, 기존 이미지 유지 표현 `TBD` | `RE_REVIEW_PENDING` 결과 `TBD` | validation·경합 `TBD` |

재신청 명세가 나오면 각 수정 필드의 필수 여부, 최대 길이, 이미지 미전송 의미, 주소 저장 형식과 상태 version 전달 방식을 반드시 기록한다.

## 테스트

- 편집 진입 시 모든 기존 값이 채워진다.
- 기존 사진 유지 상태에서도 제출할 수 있다.
- 새 사진을 선택하면 교체 payload만 전송한다.
- 보조 버튼으로 모달을 닫아도 작성 중인 값이 유지된다.
- 성공 상태는 `RE_REVIEW_PENDING`, 표시 문구는 검수중이다.
- 실패 후에도 폼과 이전 서버 상태가 유지된다.
