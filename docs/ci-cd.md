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

`vX.Y.Z` 태그를 푸시하면 `.github/workflows/release-aab.yml` 이 서명된 AAB 를 빌드해
GitHub Release 에 첨부한다 (Actions 아티팩트로도 30일 보관).

```sh
git tag v1.0.1 && git push origin v1.0.1
```

- `versionCode` 는 태그에서 `X*10000 + Y*100 + Z` 로 계산 (v1.0.1 → 10001) — semver 증가 = versionCode 단조 증가.
- 서명/비공개 키 Secrets 는 CD 워크플로와 동일한 항목을 사용한다.
- 워크플로가 기본 브랜치에 없어도 태그 푸시 트리거는 태그 시점의 워크플로 파일로 실행된다.
