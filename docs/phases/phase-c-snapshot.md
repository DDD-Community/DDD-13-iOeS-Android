# Phase C — Snapshot 테스트

> iOS `phase-c-snapshot.md`의 Android 버전. 도구: Paparazzi.

## 목적

디자인 시스템(Theme + Components)이 변경되었을 때 시각적 회귀를 막는다. `record` → `verify` 사이클로 PR 리뷰에서 픽셀 단위 diff를 확인한다.

## 강제 사항

1. Paparazzi는 호스트 사이드 JVM에서 실행되며 에뮬레이터를 요구하지 않는다. 테스트 파일은 `app/src/test/java/.../<Screen>SnapshotTest.kt`에 둔다.
2. **JUnit4 API를 쓴다** — `org.junit.Test` / `org.junit.Rule`. Phase A의 JUnit5(`org.junit.jupiter.api.*`)를 섞으면 `@get:Rule`이 무시돼 Paparazzi가 붙지 않는다. (Phase B와 동일한 제약.)
3. 스냅샷 대상: `common/designsystem/` 컴포넌트, 각 화면의 핵심 Composable(다크/라이트, a11y fontScale).
   - 프로덕션 `XxxScreen`을 직접 찍지 말고 **stateless 컨텐츠 Composable**(`XxxScreenContent`, `components/`)을 추출해 찍는다. ViewModel·Hilt 의존이 들어가면 결정적 렌더가 깨진다.
4. PNG는 **`app/src/test/snapshots/images/`에 평문으로 커밋**한다(git LFS 미사용). 파일명은 Paparazzi가 `<패키지>_<클래스>_<테스트함수>.png`로 생성하므로 손대지 않는다.
5. 변경이 의도된 경우만 `./gradlew :app:recordPaparazziDebug`로 새 스냅샷을 채택한다. diff 원인 분석 없는 블라인드 record는 금지(회귀를 정상화시킨다).

## 템플릿

```kotlin
class LoginScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun login_idle_light() {
        paparazzi.snapshot {
            PickflowTheme { LoginScreenContent(state = LoadState.Idle) }
        }
    }
}
```

케이스별로 캔버스 크기·fontScale이 달라야 하면 `DeviceConfig`를 직접 만들어 넘긴다(가로형 캔버스는 `orientation = ScreenOrientation.LANDSCAPE` 지정 필수 — layoutlib이 세로로 강제 회전한다). 실제 예시는 `SpotListSnapshotTest.kt` / `OnboardingSnapshotTest.kt`의 `device(...)` 헬퍼 참고.

## 통과 기준

- `./gradlew :app:verifyPaparazziDebug` 그린.
- 새 화면/컴포넌트는 PR 시 최소 1개 스냅샷 포함.

## 비고

Paparazzi는 Compose BOM과 AGP 버전 조합에 민감하다. `gradle/libs.versions.toml`(ARCHITECTURE.md §1)의 버전 카탈로그를 그대로 따른다.

---

## iOS 스냅샷 1:1 대응 진행 현황

iOS `PickflowTests/__Snapshots__/` 165장과 Android Paparazzi 스냅샷을 레이아웃 단위로 맞춘다.
커스텀 에셋(일러스트·사진·브랜드 로고·커스텀 드로잉 아이콘)은 재현하지 않고
raw 텍스트 / Material 컴포넌트 / 이모지 placeholder로 자리만 잡는다(에셋 치환 규칙).

| 그룹 | iOS 장수 | Android 대응 | 상태 |
|---|---|---|---|
| LoginView | 12 | 12 | ✅ 완료 |
| Onboarding | 30 | 30 | ✅ 완료 |
| MyProfile | 32 | 32 | ✅ 완료 |
| SpotList | 28 | 28 | ✅ 완료 |
| SpotDetail | 36 | 36 | ✅ 완료 |
| SpotDetailBottomSheet | 7 | 7 | ✅ 완료 |
| MapClustering | 20 | 20 | ✅ 완료 |
| **iOS 대응 합계** | **165** | **165** | **165/165 ✅** |

### Android 자체 추가 (iOS 대응 범위 밖)

iOS에 대응 스냅샷이 없는, Android 쪽에서 독립적으로 추가한 그룹.

| 그룹 | Android 장수 | 비고 |
|---|---|---|
| Archive | 17 | 보관함 목록·이름 변경 다이얼로그 |
| MapMarker | 9 | 지도 마커 선택/미선택 상태 |
| Notice | 5 | 공지 목록·상세 |
| MoodFilter(PV-59) | 4 | 무드 필터 4종 선택 상태 + 카드 배지 |
| PaparazziSetup(smoke) | 1 | 하네스 동작 확인용 |
| **소계** | **36** | |

**저장소 전체 스냅샷: 201장** (`app/src/test/snapshots/images/`). 그룹을 추가·삭제하면 이 표를 함께 갱신한다.

### LoginView (12/12 완료)

- `feature/login/LoginScreen.kt` — stateless `LoginScreenContent` 추출, iOS `LoginView` 레이아웃 1:1 이식.
  4-stop 배경 그라데이션, header(워드마크/닫기), centerContent(앱로고/타이틀/서브타이틀), bottomCTA.
- `feature/login/components/KakaoLoginButton.kt`, `AppleLoginButton.kt` — iOS 컴포넌트 1:1.
- `LoginScreenSnapshotTest.kt` — iOS 12케이스 대응(390x844, iPad 834x1194, a11y fontScale 2.0).
- 에셋 placeholder: `pickflow_wordmark`→raw 텍스트 "PICKFLOW"(Dynamic Type 비반응 고정),
  `ic_flare`→Material `Star` 아이콘, 카카오/Apple 브랜드 아이콘→이모지(💬/🍎).
- **알려진 차이**: 타이틀/서브타이틀이 iOS는 2줄, Android는 3줄로 줄바꿈됨.
  원인은 Pretendard 폰트 미반영(`PickflowTypography`가 `FontFamily.Default` 사용 — 코드베이스 기존
  placeholder). 레이아웃 토큰(maxWidth 295dp, displayLarge/bodyLarge)은 iOS와 동일하므로
  Pretendard ttf 합류 시 자동 일치 = "에셋만 끼우면 되는 상태".

### Onboarding (30/30 완료)

- `feature/onboarding/model/OnboardingPageContent.kt` — iOS `OnboardingPage` 1:1
  (4페이지, layout 3종 TOP/BOTTOM/MOOD_CAROUSEL, 그라데이션 3종, mood 헤더).
- `feature/onboarding/components/` — iOS 컴포넌트 1:1 이식:
  `OnboardingPalette`, `OnboardingPageIndicator`(20x8 캡슐), `OnboardingPrimaryButton`,
  `OnboardingToast`, `OnboardingMoodHeader`(칩 59x28/84x40), `OnboardingFocusedCarousel`(정적),
  `OnboardingIllustration`(layout 분기), `OnboardingPanel`(타이틀 하이라이트 강조).
- `OnboardingScreen.kt` — stateless `OnboardingScreenContent` 추출. CTA는 iOS와 동일하게
  모든 페이지에서 "시작하기" 고정 + `finish` 호출(페이지 전환은 스와이프 제스처 담당).
- `OnboardingSnapshotTest.kt` — iOS 30케이스 대응. Paparazzi `DeviceConfig`를 케이스별
  크기로 지정(illustration 393x600, panel 393x320, indicator 160x60, cta 393x96, screen 390x844).
  가로형 캔버스는 `orientation=LANDSCAPE` 지정 필수(layoutlib가 세로 강제 회전하는 문제).
- 에셋 placeholder: 폰 목업/캐러셀 사진→placeholder 박스(📱/🏞), mood 아이콘→이모지(🌅/🌊),
  `Image(.logo)` 워드마크→raw 텍스트, `checkmark.circle.fill`→Material `CheckCircle`.
- iOS `OnboardingPalette`가 고정색이라 light/dark 렌더가 동일 — 양쪽 이름으로 동일 record.
- `OnboardingScreenUiTest`는 iOS 동작(CTA "시작하기" 고정)에 맞춰 갱신. ViewModel은 무변경.

### MyProfile (32/32 완료)

- `feature/myprofile/components/` — iOS 스냅샷 컨텐츠 1:1 대응 stateless Composable:
  `MyProfileSignedOutContent`(워드마크/안내문/로그인 CTA), `MyProfileSignedInContent`
  (프로필 헤더 + 메뉴 행), `MyProfileLoadingContent`·`MyProfileFailedContent`(상태 화면).
- `feature/accountmanagement/components/` — `AccountManagementContent`(navBar +
  프로필이미지/닉네임/소셜/계정액션), `LogoutConfirmDialogOverlay`(스크림 + 흰 카드 다이얼로그).
- `feature/withdrawal/` — `model/WithdrawalReason`(사유 7종 enum), `components/`
  `WithdrawalReasonDropdown`(헤더 + 7행 목록), `WithdrawalContent`(유의사항/사유/동의/제출).
- `MyProfileSnapshotTest.kt` — iOS 32케이스 1:1 대응. 전부 393x852 고정.
  iOS는 마이페이지/계정관리/회원탈퇴를 정적 다크 토큰으로 렌더하므로 light/dark/a11y가
  동일 — Android도 같은 컨텐츠를 각 이름으로 record. `pretendard` 고정 크기라 a11y 무반응.
- 에셋 placeholder: 프로필 사진→Material `Person`, 카메라 배지→이모지(📷),
  `pickflow_wordmark`→raw 텍스트, `exclamationmark.triangle`→Material `Warning`,
  `checkmark.circle.fill`→Material `CheckCircle`, ProgressView→정적 호(결정성).
- **레퍼런스 주의**: iOS `__Snapshots__` PNG는 현재 iOS 소스보다 과거 시점 기록이라
  `MyProfileSignedInContent`/`MyProfileSignedOutContent`/`LogoutConfirmDialog` 소스와
  레이아웃·문구가 다르다. 루프 규약(레퍼런스=PNG)에 따라 **PNG 기준**으로 이식했다.
- 프로덕션 `MyProfileScreen`/`AccountManagementScreen` 및 ViewModel·UI 테스트는 무변경
  (스냅샷용 stateless 컨텐츠만 신규 추가 — 기존 테스트 그린 유지).

### SpotList (28/28 완료)

- `feature/spotlist/components/` — iOS SpotList 스냅샷 컨텐츠 1:1 대응 stateless Composable:
  `SpotListModels`(iOS `SpotTheme`(노을/윤슬)·`SpotListItem`·`SpotListSort` 대응 —
  프로덕션 `core...SpotTheme`(CAFE/...)와 충돌 피해 `SpotListMood`/`SpotListGridItem`/
  `SpotListSortOption`로 명명), `SpotListCell`(썸네일+badge+meta), `SpotListLoadedGrid`
  (Masonry 2열 비대칭 + `SpotListLoadingContent` 스켈레톤), `SpotListPlaceholders`
  (empty/failed/unauthorized), `SpotListSortBar`(정렬 드롭다운 헤더+옵션).
- `SpotListSnapshotTest.kt` — iOS 28케이스 1:1 대응. screen 393x852, sortbar 393x48·200,
  cell 168x280·420(a11y). iOS SpotList의 `pretendard`는 a11y(DynamicType)에 **반응**하므로
  empty/unauthorized/cell a11y는 fontScale 2.0으로 렌더(MyProfile과 다른 점).
- 에셋 placeholder: 썸네일 사진→gray90 박스, mood overlay 커스텀 아이콘→이모지(🌅/🌊),
  북마크 아이콘→Material `Favorite`/`FavoriteBorder`, `magnifyingglass`→`Search`,
  `icErrorOutline`→`Warning`, `icLocationOn`→`LocationOn`.
- 프로덕션 `SpotListScreen`/`SpotListViewModel`(별도 도메인 모델 사용)은 무변경.

### SpotDetail (36/36 완료)

- `feature/spotdetail/components/` — iOS SpotDetail 스냅샷 컨텐츠 1:1 대응 stateless Composable:
  `SpotDetailModels`(`SpotDetailData`/`SpotDetailTheme`), `SpotDetailNavBar`(공유/닫기),
  `SpotHeaderSection`(이름·MY배지·코멘트), `SpotPhotoSection`(사진 박스+주소),
  `SpotActionButtons`(길안내+북마크/내스팟), `SpotRealTimeInfoSection`(실시간 정보 카드),
  `SpotDetailScreens`(loading/error/ReportButton/loaded 전체 화면).
- `SpotDetailSnapshotTest.kt` — iOS 36케이스 1:1 대응. screen 393x852, navbar 393x48,
  header 361x200·300, photo 361x240, action 361x68·64, realtime 361x300·400.
- 컴포넌트 고립 스냅샷은 iOS swift-snapshot-testing `.fixed` 동작(컨텐츠를 자연 높이로
  측정 후 프레임 중앙 배치, 오버플로 시 상하 균등 클립)을 `wrapContentHeight(unbounded=true)`
  + Center 정렬로 재현 — realtime 카드가 padding만 클립되고 4행이 모두 보이도록.
- 에셋 placeholder: 사진→gray90 박스, `icShare`/`icClose`→Material `Share`/`Close`,
  `icNearMe`→`Send`, 북마크→`Favorite`/`FavoriteBorder`, `icLocationOn`→`LocationOn`,
  날씨/일몰/혼잡 아이콘→이모지(☀️/🌅/👥)·raw 텍스트("P"), `icHelpOutline`→`Info`.
- a11y(header/realtime)는 SpotList와 동일하게 fontScale 2.0. 프로덕션 `SpotDetailScreen`
  /ViewModel·테스트 무변경.

### SpotDetailBottomSheet (7/7 완료)

- `feature/spotdetail/components/SpotDetailSheetContent.kt` — iOS `SheetChromeView`
  (`SpotSheetChrome`: 상단 24 패딩 + gray95 + 상단 코너 20) + `SpotDetailSheetContentView`
  (`SpotDetailSheetContent`: 이름·MY배지·닫기 / 테마·북마크 / 거리·주소(펼침 토글) / 사진 /
  `SpotBottomSheetActionButtons`) 1:1 이식. `SpotDetailData`에 `distanceKm` 필드 추가.
- `SpotDetailBottomSheetSnapshotTest.kt` — iOS 7케이스 1:1 대응. 시트는 iOS `.sizeThatFits`
  (폭 390 고정, 높이 컨텐츠) — iOS PNG 종횡비에서 산출한 디바이스 높이로 고정 렌더
  (collapsed/bookmarked/expanded/mySpot 390x438, longName 390x468, AXL 390x510, chrome 390x230).
- AXL(`dynamicTypeAXL`)는 fontScale 2.0. 길 안내/저장 버튼 텍스트는 iOS와 동일하게
  maxLines=1 + Ellipsis(iOS는 truncation/minimumScaleFactor) — 큰 폰트에서 버튼 깨짐 방지.
- 에셋 placeholder: 사진→gray90 박스, `icClose`→`Close`, `icNearMe`→`Send`,
  북마크→`Favorite`/`FavoriteBorder`. 프로덕션 코드·테스트 무변경.

### MapClustering (20/20 완료)

- `feature/map/clustering/` — iOS 지도 핀 컴포넌트 1:1 이식:
  `ClusterPinView`(개수별 4단계 지름 44/54/64/74의 sunsetOrange 원 + 개수),
  `MyClusterPinView`(56 흰 원+검정 0.2+그라데이션+사진 아이콘+"MY"),
  `SpotMarkerView`(44 검정 그라데이션 원 + 사진 아이콘).
- `MapClusteringSnapshotTest.kt` — iOS 20케이스 1:1 대응. iOS `.sizeThatFits`처럼
  디바이스를 핀 지름에 맞춰 렌더. 핀 내용물이 정적 색이라 light/dark 동일.
- 에셋 placeholder: `icPhoto` 커스텀 에셋→이모지(🖼️). 핀 아이콘은 iOS `Image`처럼
  Dynamic Type 비반응(`LocalDensity` fontScale 1 고정).
- a11y(accessibility3)는 fontScale 2.0. `MyClusterPinView`의 "MY"는 iOS와 동일하게
  a11y 시 폭 초과로 ellipsis 처리(폭 26dp 제한).
- **알려진 차이**: iOS 핀 스냅샷은 코너가 투명(`.sizeThatFits`), Android Paparazzi는
  코너가 디바이스 배경(검정)으로 렌더된다 — 원형 컴포넌트 바깥 4개 코너 픽셀 한정이며
  핀 본체(원·텍스트·아이콘) 레이아웃은 완전 일치.

---

## 최종 결과 — iOS 165장 1:1 대응 완료 ✅

7개 그룹 165장 전부 Android Paparazzi 스냅샷으로 대응 완료(여기에 Android 자체 추가 32장을
더해 저장소 전체는 197장).
`./gradlew :app:assembleDebug` · `:app:testDebugUnitTest` 그린, 미대응 0건.
공통 패턴: 정적 다크 토큰을 쓰는 화면은 light/dark 동일 record, a11y는 fontScale 2.0
(MyProfile은 iOS pretendard가 a11y 비반응이라 예외), 커스텀 에셋은 raw 텍스트 /
Material 컴포넌트 / 이모지 placeholder로 치환(에셋 치환 규칙). 프로덕션 화면·ViewModel·
기존 테스트는 무변경 — 스냅샷용 stateless 컨텐츠 Composable만 신규 추가.
