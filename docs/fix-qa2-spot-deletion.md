# 스팟 삭제 후 보관함 갱신 수정

- 작업 브랜치: `feature/fix-qa2`
- 기준: 로컬 `feature/fix-qa`, `ba6e2862426b00560be0a8ae502bb22b8f092e6a`
- 저장소와 상위 디렉터리에 `AGENTS.md`는 없으며, `CLAUDE.md`와 Phase A/B/C 검증 지침을 적용했다.

## 원인과 수정

`ArchiveViewModel`은 HOME 백스택에 남는다. 상세에서 삭제하고 복귀하면
`ArchiveScreen`의 `onAppear()`가 호출되지만, 기존 코드는 보관함 정보와 저장한 스팟만
재조회했다. 나만의 스팟은 `Idle`일 때만 읽어 삭제 전 `Loaded` 목록이 계속 표시됐다.
마이탭은 진입마다 `fetchMyPage()`를 호출하므로 서버 카운트만 먼저 줄어드는 현상과 일치한다.

보관함 재진입 시 선택 중이거나 이미 조회한 나만의 스팟도 첫 페이지부터 재조회한다.
기존 조회 함수로 누적 목록·페이지 번호·다음 페이지 여부를 초기화하고,
이전 나만의 스팟 요청을 취소하여 늦은 페이지 응답이 삭제된 항목을 다시 추가하지 않게 했다.
취소는 오류 토스트/실패 상태로 변환하지 않는다. 아직 방문하지 않은 나만의 스팟은
기존처럼 탭을 선택할 때 처음 조회한다. 프로덕션 변경은 `ArchiveViewModel.kt` 한 파일이다.

## 검증

- Phase A: 수정 전 회귀 테스트 4건 실패를 확인한 뒤 수정 후 통과했다.
  삭제 후 두 목록 재조회, 페이지네이션 재시작, 마지막 항목 삭제의 Empty 상태,
  비선택 탭 캐시 갱신, 재조회 실패와 복구, 이전 페이지 요청 취소, 최초 lazy fetch를 검증했다.
- 마이탭 ViewModel 재조회 시 등록 스팟 수 2→1, 저장 스팟 수 3→2 반영을 검증했다.
- Phase B: 관련 UI 테스트 38건 통과. 실제 보관함 화면과 유지된 ViewModel을 사용하여
  상세 라우트 진입 → 삭제 서비스 성공 → pop → 카드 제거/Empty 상태를 검증했다.
  실제 상세 화면의 삭제 확인 동작과 삭제 완료 콜백 1회 호출도 검증했다.
- 기존 삭제 액션의 성공 ID 이벤트와 실패 시 재시도 토스트 테스트 통과.

- 전체 `./gradlew :app:testDebugUnitTest :app:assembleDebug` 통과.
  테스트 615건, 실패 0건, 오류 0건, 건너뜀 0건.
  APK: `app/build/outputs/apk/debug/app-debug.apk`.

- Phase C: `./gradlew :app:verifyPaparazziDebug`는 이미지 차이 110건으로 실패했다.
  이 워크트리에서 유일한 프로덕션 변경 파일을 기준 HEAD 버전으로 잠시 교체하여
  `./gradlew :app:verifyPaparazziDebug --tests '*SnapshotTest'`로 비교한 결과,
  실패한 110개 테스트 이름과 이미지 차이율이 수정 후와 모두 동일했다.
  따라서 이 실행 환경에서 수정 전에도 발생하는 스냅샷 불일치이며,
  기준 PNG를 갱신하지 않았다. 비교 후 수정 파일은 원본 바이트 그대로 복원했다.

- 복원 후 보관함 ViewModel/UI 회귀 테스트 34건 및 `:app:assembleDebug` 재검증 통과.
- `git diff --check` 통과. 주요 진행 상황과 최종 결과는 Orca 워크트리 코멘트에 기록했다.

## 미확인 사항

실제 서버/로그인 계정/기기에서 삭제하는 수동 QA는 수행하지 않았다.
서비스 응답을 모킹한 JVM·Robolectric 검증이므로 실제 DELETE 응답, 서버 목록 반영 시점,
실기기 토스트 표시 시간은 확인하지 않았다.

기준 커밋의 `SpotOpenActionsViewModel.delete()`는 성공 시 삭제 ID 이벤트만 발행하고,
`SpotDetailScreen`과 `PickflowNavHost`는 이를 받아 탭 이동과 뒤로가기를 수행한다.
이 경로에 삭제 성공 토스트를 설정하는 코드는 없으므로 제보의 성공 토스트 표시는
이 소스로 확인할 수 없다. 삭제/토스트 프로덕션 코드는 변경하지 않았다.
