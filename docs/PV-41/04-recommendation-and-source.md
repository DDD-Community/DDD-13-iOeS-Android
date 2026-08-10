# PV-41 — 추천·출처·피드백

## 디자인 참조

- [추천 버튼 활성·비활성](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=733-13954&t=7jXFETjPNUTJ0bx2-11)
- [출처 줄·유저 등록 뱃지](https://www.figma.com/design/WLGPjrQtLqyhq46zXxvXHp/DDD-design-%EC%B0%90?node-id=747-7041&t=7jXFETjPNUTJ0bx2-11)

> 구현 전 Figma MCP로 추천의 기본·선택·pressed·disabled 상태와 출처 컴포넌트의 최신 속성을 확인한다.

## 추천 노출 규칙

- 관리자 큐레이션과 `PUBLISHED` 유저 스팟에만 추천 버튼을 표시한다.
- 작성자 본인의 공개 스팟에도 셀프 추천을 허용한다.
- `DRAFT`, `PENDING`, `RE_REVIEW_PENDING`, `REJECTED`에는 버튼을 표시하지 않는다.
- 추천 전용 모음 화면은 만들지 않는다.
- 추천 수는 `1천+`처럼 축약하지 않고 서버 원 숫자를 표시한다.

## 추천 인터랙션

1. 미추천 상태에서 누르면 버튼을 활성화하고 카운트를 `+1`로 낙관 반영한다.
2. 추천 상태에서 다시 누르면 비활성화하고 카운트를 `-1`로 낙관 반영한다.
3. 요청 중 추가 탭은 무시해 동일 의도를 중복 전송하지 않는다.
4. 성공 응답의 추천 여부와 최종 카운트로 화면을 다시 맞춘다.
5. 실패하면 직전 상태로 되돌리고 `잠시 후 다시 시도해주세요` 토스트를 표시한다.
6. 비로그인 사용자는 기존 로그인 유도 모달 또는 로그인 화면으로 연결한다.

여러 사용자의 동시 추천 결과는 항상 서버가 반환한 최종 숫자를 기준으로 한다. 오픈 취소 시 추천 관계와 카운트를 삭제하지 않으며 재승인 후 복원한다.

## 출처 표시

출처는 **상세페이지만** 표시한다.

- 관리자 큐레이션: `한국관광공사`, `Pickflow 운영자` 등 서버 소스에 대응하는 출처 줄을 조건 없이 표시한다.
- 공개 유저 스팟: 스팟명 옆에 **유저 등록** 뱃지를 고정 표시한다.
- 탐색 리스트와 보관함 카드에는 출처 줄이나 뱃지를 추가하지 않는다.
- 닉네임 미설정 작성자는 기존 형용사+명사 기본 닉네임을 사용하며 신규 생성 로직을 만들지 않는다.

## 잘못된 정보 피드백

- 관리자 큐레이션과 `PUBLISHED` 유저 스팟에 기존 **잘못된 정보가 있나요?** 링크를 표시한다.
- 비공개 상태에는 표시하지 않는다.
- 기존 `SpotDetailScreens.kt`의 `ReportButton`과 신고 시트를 재사용한다.

## 파일 매핑

| 파일 | 변경 |
| --- | --- |
| `SpotDetail.kt`, `SpotDetailDto.kt`, `SpotMapper.kt` | 추천 수·여부, 출처 유형·표시명 추가 |
| 신규 `RecommendationService`와 API/구현 | 추천 등록·취소 및 서버 최종 카운트 반환 |
| `feature/spotdetail/SpotDetailViewModel.kt` | 로그인 가드, 낙관 반영, 중복 탭 방지, 실패 롤백 |
| `feature/spotdetail/components/SpotHeaderSection.kt` | 유저 등록 뱃지, 큐레이션 출처 줄 |
| `feature/spotdetail/components/SpotActionButtons.kt` | 공개 상태 추천 버튼·카운트 |
| `feature/spotdetail/components/SpotDetailScreens.kt` | 공개 상태에서 기존 피드백 링크 유지 |

## Stub 구현

- `StubRecommendationService`가 공유 스팟의 사용자별 추천 여부와 최종 카운트를 갱신한다.
- 공개 상태가 아니면 실제 서버와 같은 도메인 오류를 반환한다.
- 추천 실패와 지연을 fixture로 제공해 낙관 업데이트·중복 탭 방지를 검증한다.
- 큐레이션과 유저 공개 스팟 fixture에 서로 다른 출처를 포함한다.

## API 계약 입력란

| 기능 | Method | Endpoint | Integration Status |
| --- | --- | --- | --- |
| 공개 상세 조회 | `TBD` | `TBD` | `TBD` |
| 추천 등록 | `TBD` | `TBD` | `TBD` |
| 추천 취소 | `TBD` | `TBD` | `TBD` |
| 잘못된 정보 제보 | 기존 계약 확인 | 기존 endpoint 확인 | `TBD` |

| API 기능 | Path Param | Query Param | Request Body / Content-Type | Success Response | Error Codes / 앱 처리 |
| --- | --- | --- | --- | --- | --- |
| 공개 상세 조회 | `TBD` | `TBD` | 없음 | 추천 수·여부, 출처 유형·표시명 `TBD` | 비공개·미존재 `TBD` |
| 추천 등록 | `TBD` | `TBD` | `TBD` | 최종 추천 수·추천 여부 `TBD` | 비로그인·비공개·중복 `TBD` |
| 추천 취소 | `TBD` | `TBD` | `TBD` | 최종 추천 수·추천 여부 `TBD` | 비로그인·미추천 `TBD` |
| 잘못된 정보 제보 | 기존 `spotId` | 기존 계약 | 기존 body | 기존 response | 유저 공개 스팟 허용 여부 확인 |

추천 API가 멱등인지, 중복 등록·취소를 성공으로 정규화하는지, 카운트를 항상 반환하는지 명세에 기록한다.

## 테스트

- 공개 상태와 큐레이션에만 추천이 표시된다.
- 셀프 추천이 정상 동작한다.
- 빠른 연속 탭이 한 요청으로 제한된다.
- 실패 시 버튼과 카운트가 모두 복구된다.
- 성공 후 서버 최종 카운트가 표시된다.
- 큐레이션 출처 줄과 유저 등록 뱃지가 올바르게 구분된다.
- 탐색·보관함 카드에는 출처가 추가되지 않는다.
- 유저 공개 상세에도 기존 피드백 링크가 동작한다.
