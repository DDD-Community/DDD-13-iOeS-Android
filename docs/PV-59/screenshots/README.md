# PV-59 시나리오 실행 증적

**촬영일** 2026-08-05 · **커밋** `52a3a98` · **기기** Pixel_8_API_35 (1080×2400, xhdpi) · **빌드** debug

> ⚠️ **이 이미지들은 검증되지 않는다.** `verifyPaparazziDebug` 가 확인하는 Paparazzi 스냅샷과 달리,
> 여기 있는 PNG 는 위 시점의 수동 기록일 뿐이다. UI 가 바뀌어도 자동으로 갱신되거나 실패하지 않으므로
> **날짜와 커밋을 함께 읽어야 한다.** 재촬영 시 이 헤더를 반드시 갱신할 것.
> 절차는 `docs/PV-59/simulator-test-scenarios.md`.

## 목록

| 시나리오 | 파일 | 확인 내용 |
|---|---|---|
| EXP-01 | `PV59-EXP-01-map.png` / `-list.png` | 무드 4종·햇살→윤슬→노을→야경 순서 |
| EXP-02 | `PV59-EXP-02-initial-map.png` / `-list.png` | 초기 진입 시 전부 미선택 |
| EXP-03 | `PV59-EXP-03-multi-map.png` / `-list.png` | 햇살+야경 동시 선택(다중선택) |
| EXP-04 | `PV59-EXP-04-after-retap-*.png` | 재탭 시 그 하나만 해제 → 야경만 남음 |
| EXP-04 | `PV59-EXP-04-cleared-*.png` | 전체 해제 = 전체 목록 복귀(빈 화면 아님) |
| EXP-05 | `PV59-EXP-05-sunset-*.png`, `-yunseul-*.png` | 기존 무드 회귀 — 실서버 데이터 |
| REG-01 | `PV59-REG-01-chips.png` | 등록 칩 4종·순서·초기 미선택 |
| REG-02 | `PV59-REG-02-sunlight-selected.png`, `-night-selected.png` | 등록 폼 **단독** 선택 |
| CARD-01 | `PV59-CARD-01-list.png`, `-list-scrolled.png` | 리스트 카드 배지 4종 |
| CARD-02 | `PV59-CARD-02-map-all.png`, `-sheet.png` | 지도 4종 선택 + 바텀시트 무드 표기 |
| NET-01 | `PV59-NET-01-log.txt` | 전 조합 HTTP 400 **0건** |

## REG 증적이 Paparazzi 인 이유

등록 폼은 **로그인이 필요해 비회원 상태의 에뮬레이터로는 진입할 수 없다**(로그인 유도 팝업에서 막힘).
그래서 REG-01/02 는 에뮬레이터 스크린샷 대신 `SpotRegistrationThemeChipSnapshotTest` 의
Paparazzi 결과를 복사해 두었다. 원본은 `app/src/test/snapshots/images/` 에 있고 CI 가 검증한다.

동작(단독 선택 여부) 자체는 `SpotRegistrationScreenUiTest` 가 Robolectric 으로 검증한다.

## NET-01 결과 요약

전 조합에서 서버로 나간 `theme` 값은 `SUNSET` / `YUNSEUL` 뿐이고 응답은 전부 200이었다.
`SUNLIGHT`·`NIGHT` 가 붙은 요청과 `theme` 가 두 번 붙은 요청은 0건이다
(임시 폴백이 걸러낸다 — `docs/PV-59/backend-compat-rollback.md`).

## `[STUB]` 표기에 대해

햇살/야경 스팟 이름 앞의 `[STUB]` 은 정상이다. 백엔드가 두 무드를 아직 모르므로
임시로 채운 가짜 데이터이며, 백엔드 완료 시 실데이터로 바뀐다.
