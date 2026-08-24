# CI / CD 가이드

GitHub Actions 워크플로를 운영한다.

| 워크플로 | 파일 | 트리거 | 동작 |
|---|---|---|---|
| CI | `.github/workflows/ci.yml` | `develop`/`main` PR·푸시 | lint → 유닛 테스트 → `assembleDebug` |
| CD | `.github/workflows/cd.yml` | `workflow_dispatch`(브랜치 수동 선택) | 서명된 release APK → Firebase App Distribution |
| QA 배포 | `.github/workflows/firebase-distribution.yml` | `develop` 푸시·`workflow_dispatch` | debug APK(개발 서버) → Firebase App Distribution `qa` 그룹 — [firebase-distribution.md](./firebase-distribution.md) |

## CI

비공개 키가 필요 없다. `secrets.properties` 가 없으면 `secrets.defaults.properties` 의 빈 기본값으로 debug 빌드한다. 별도 Secrets 설정 없이 바로 동작한다.

## CD 사용법

GitHub → **Actions** → **CD (Firebase App Distribution)** → **Run workflow** →
배포할 **브랜치 선택** + 테스터 그룹/릴리스 노트 입력 → 실행.

### 필요한 GitHub Secrets

Settings → Secrets and variables → Actions 에 등록한다.

**서명(keystore)**

| 이름 | 설명 |
|---|---|
| `KEYSTORE_BASE64` | keystore 파일을 base64 인코딩한 값. `base64 -i release.keystore \| pbcopy` |
| `KEYSTORE_PASSWORD` | keystore 비밀번호 |
| `KEY_ALIAS` | 키 별칭 |
| `KEY_PASSWORD` | 키 비밀번호 |

**Firebase App Distribution**

| 이름 | 설명 |
|---|---|
| `FIREBASE_APP_ID` | Firebase 콘솔의 Android 앱 ID(`1:xxxx:android:xxxx`) |
| `FIREBASE_SERVICE_ACCOUNT` | App Distribution 권한이 있는 서비스 계정 JSON 전체 |

**앱 비공개 키**(release 빌드에 실제 값 반영 — `secrets.properties` 로 주입)

`NAVER_MAP_CLIENT_ID`, `KAKAO_NATIVE_APP_KEY`, `PICKFLOW_API_BASE_URL`,
`TERMS_URL`, `PRIVACY_URL`, `NOTICE_BOARD_MASTER_ID`, `KAKAO_REST_API_KEY`,
`APPLE_SERVICE_ID`, `APPLE_REDIRECT_URI`

> 미설정 항목은 빈 값으로 빌드된다. 지도/카카오 로그인 등이 동작하려면 최소
> `NAVER_MAP_CLIENT_ID`, `KAKAO_NATIVE_APP_KEY` 는 등록해야 한다.

## 서명 동작 방식

`app/build.gradle.kts` 의 release 빌드타입은 환경변수 `KEYSTORE_FILE` 이
존재할 때만 release 키로 서명하고, 없으면 debug 키로 서명한다. 따라서 로컬
`./gradlew assembleRelease` 도 그대로 동작하며, CD 에서만 실제 keystore 가 주입된다.

## Play 심사용 AAB 릴리스

> 로컬에서 keystore 로 직접 서명 AAB 를 뽑는 절차는 [`release-build.md`](./release-build.md) 참고.

`vX.Y.Z[-N]` 태그를 푸시하면 `.github/workflows/release-aab.yml` 이 서명된 AAB 를 빌드해
GitHub Release 에 첨부한다 (Actions 아티팩트로도 30일 보관).

```sh
git tag v1.0.1-2 && git push origin v1.0.1-2
```

- versionName 은 태그에서 파생, versionCode 는 build.gradle fallback — 아래 "버전 규칙" 참고.
- 서명/비공개 키 Secrets 는 CD 워크플로와 동일한 항목을 사용한다.
- 워크플로가 기본 브랜치에 없어도 태그 푸시 트리거는 태그 시점의 워크플로 파일로 실행된다.

## 버전 규칙

**versionName(마케팅 버전)과 versionCode(빌드 넘버)는 분리한다.**

| 항목 | 규칙 |
|---|---|
| 태그 | `vX.Y.Z[-N]` — N 은 같은 versionName 의 빌드 차수(생략 시 1, **기록용**) |
| versionName | 태그에서 파생 — `X.Y.Z` (스토어 노출 버전) |
| versionCode | **단조증가 정수.** `app/build.gradle.kts` fallback 이 **단일 출처**이며 릴리스마다 `+1` 해 커밋한다. 태그/versionName 과 무관. |

> **versionCode 스킴 전환(2026-07):** 과거 인코딩 스킴(`X*1000000 + Y*10000 + Z*100 + N`)은
> 폐지했다. Android versionCode 의 유일한 요건은 "이전보다 큰 양의 정수"(상한 약 21억)이며,
> 단조증가가 더 단순하고 실수가 없다. 이미 Play 에 소모된 `1000102` 위에서 순차 증가한다
> (내려갈 수 없으므로 1 부터 리셋은 불가). **다음 릴리스 = `1000103`.**

핵심 원칙:

1. **Play 에 업로드한 versionCode 는 재사용 불가.** 심사 반려/재업로드도 **새 versionCode(+1)** 가 필요하다.
   (같은 버전 재업로드 시 versionName 은 유지하고 태그만 `-N` 을 올려 기록한다.)
2. versionName 을 올리는 것은 **배포(정식 출시)가 나간 뒤** 다음 릴리스부터.
   배포 전 재빌드에 patch 를 소모하지 않는다.
3. 태그를 만들기 전 `app/build.gradle.kts` 의 **versionCode fallback 을 `+1`**(필요 시 versionName 도)
   갱신해 커밋한다. 이 커밋값이 로컬/CI 빌드의 단일 출처다.
4. 실기기 QA 시 주의: 릴리스(스토어) 빌드가 설치된 기기에는 디버그 빌드가
   `INSTALL_FAILED_VERSION_DOWNGRADE` / 서명 불일치로 덮어써지지 않는다 → 제거 후 설치.

## 버전 기록

| 태그 | versionName | versionCode | 상태 | 내용 |
|---|---|---|---|---|
| (없음) | 1.0.0 | 1(추정) | 심사 제출 | 최초 심사 제출 |
| v1.0.1 | 1.0.1 | 10001 (구 인코딩) | 업로드됨 | 스팟 등록/신고 API 수정, 닉네임 저장, 바텀시트 안정화, 등록 완료 플로우, 상세 UI 시안 반영, 딥링크 스킴 |
| ~~v1.0.2~~ | - | ~~10002~~ | **폐기** | 1.0.1 미배포 상태에서 versionName 을 잘못 올린 빌드 — 태그/릴리스 삭제 |
| v1.0.1-2 | 1.0.1 | 1000102 (구 인코딩·마지막) | 심사용 | 위 + 혼잡도 표시 기준 팝업, 실시간 정보 AM/PM 시간 포맷, ? 아이콘 교체 |
| v1.0.1-3 | 1.0.1 | 1000103 | **Play 배포됨** | 스토어 배포본. 표 기록이 누락돼 있던 것을 2026-08-18 실기기 설치본(installer=com.android.vending)에서 확인해 보정 |
| v1.0.2 | 1.0.2 | 1000201 | **Play 배포됨** | 스팟 목록 페이지네이션 중복 key 크래시·중복 요청 방지, 스팟 상세 하단 내비게이션 바 가림 수정, 마이페이지 아바타 프로필 이미지 렌더링, 계정 관리 카메라 배지 아이콘 교체 |
| v1.0.3 | 1.0.3 | 1000301 | 준비 중 | **핫픽스.** v1.0.2 태그 기준(1.1.0 무드 필터 확장은 미포함). 비회원 스팟 신고 차단·나의 스팟 신고 진입점 제거, 북마크 연타 동시성 가드, 공지사항 하단 내비게이션 바 가림 수정, 탐색 리스트 무한 스크롤 복구, 탐색 리스트·지도 바텀시트 북마크 서버 상태 미반영 수정, 북마크 아이콘 교체 |
| (없음) | 1.1.0 | 1010001 | Firebase QA 배포 | 무드 필터 확장(햇살/야경 추가, 지도·리스트 다중선택·선택 공유), debug/release API 엔드포인트 분리, Firebase App Distribution 도입. Play 미업로드 — App Distribution `pickflow-qa` 그룹 전용 debug 빌드 |
| (없음) | 1.0.4 | 1000401 | Firebase QA 배포 | Dev Mode 화면(런타임 dev/prod 전환, 환경 배지, 터치 표시). debug 빌드 전용. Play 미업로드 — App Distribution `pickflow-qa` 그룹 전용 |

> versionCode 는 **단조증가**가 최우선 제약이다(Play 는 직전 업로드보다 큰 값만 받는다).
> 1.0.2 부터는 그 안에서 `XYZNN` 형태(versionName 각 자리 + 빌드 차수 2자리)로 읽는다.
> 예: 1.0.2 의 1번째 빌드 → `1000201`, 2번째 빌드 → `1000202`, 1.0.3 의 1번째 → `1000301`.
> 단, 이 형태가 직전 versionCode 보다 작아지는 경우에는 단조증가가 우선한다.
