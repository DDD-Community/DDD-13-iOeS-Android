# CI / CD 가이드

GitHub Actions 워크플로 2종을 운영한다.

| 워크플로 | 파일 | 트리거 | 동작 |
|---|---|---|---|
| CI | `.github/workflows/ci.yml` | `develop`/`main` PR·푸시 | lint → 유닛 테스트 → `assembleDebug` |
| CD | `.github/workflows/cd.yml` | `workflow_dispatch`(브랜치 수동 선택) | 서명된 release APK → Firebase App Distribution |

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

`vX.Y.Z[-N]` 태그를 푸시하면 `.github/workflows/release-aab.yml` 이 서명된 AAB 를 빌드해
GitHub Release 에 첨부한다 (Actions 아티팩트로도 30일 보관).

```sh
git tag v1.0.1-2 && git push origin v1.0.1-2
```

- versionName/versionCode 는 태그에서 파생 — 아래 "버전 규칙" 참고.
- 서명/비공개 키 Secrets 는 CD 워크플로와 동일한 항목을 사용한다.
- 워크플로가 기본 브랜치에 없어도 태그 푸시 트리거는 태그 시점의 워크플로 파일로 실행된다.

## 버전 규칙

**versionName(마케팅 버전)과 versionCode(빌드 넘버)는 분리한다.**
배포가 나가기 전까지는 같은 versionName 으로 빌드 차수만 올려 재빌드한다.

| 항목 | 규칙 |
|---|---|
| 태그 | `vX.Y.Z[-N]` — N 은 같은 versionName 의 빌드 차수(생략 시 1) |
| versionName | 태그에서 파생 — `X.Y.Z` (스토어 노출 버전) |
| versionCode | 태그에서 파생 — `X*1000000 + Y*10000 + Z*100 + N` (예: v1.0.1-2 → 1000102) |
| 로컬 기본값 | `app/build.gradle.kts` 의 fallback 을 **최신 빌드와 동일하게** 유지 |

핵심 원칙:

1. **Play 에 업로드한 versionCode 는 소모된다(재사용 불가).** 심사 반려/수정으로 같은
   버전을 다시 올릴 땐 versionName 은 그대로 두고 `-N` 만 올린다 (v1.0.1 → v1.0.1-2).
2. versionName 을 올리는 것은 **배포(정식 출시)가 나간 뒤** 다음 릴리스부터.
   배포 전 재빌드에 patch 를 소모하지 않는다.
3. 태그를 만들기 전 로컬 fallback(versionName/versionCode)을 새 값으로 갱신해 커밋한다.
   (디버그 빌드 표기와 릴리스가 일치해야 기기 확인 시 혼선이 없다.)
4. 실기기 QA 시 주의: 릴리스(스토어) 빌드가 설치된 기기에는 디버그 빌드가
   `INSTALL_FAILED_VERSION_DOWNGRADE` / 서명 불일치로 덮어써지지 않는다 → 제거 후 설치.

## 버전 기록

| 태그 | versionName | versionCode | 상태 | 내용 |
|---|---|---|---|---|
| (없음) | 1.0.0 | 1(추정) | 심사 제출 | 최초 심사 제출 |
| v1.0.1 | 1.0.1 | 10001 (구 스킴) | 업로드됨 | 스팟 등록/신고 API 수정, 닉네임 저장, 바텀시트 안정화, 등록 완료 플로우, 상세 UI 시안 반영, 딥링크 스킴 |
| ~~v1.0.2~~ | - | ~~10002~~ | **폐기** | 1.0.1 미배포 상태에서 versionName 을 잘못 올린 빌드 — 태그/릴리스 삭제 |
| v1.0.1-2 | 1.0.1 | 1000102 (신 스킴) | 심사용 | 위 + 혼잡도 표시 기준 팝업, 실시간 정보 AM/PM 시간 포맷, ? 아이콘 교체 |
