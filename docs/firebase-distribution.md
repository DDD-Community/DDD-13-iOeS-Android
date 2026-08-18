# Firebase App Distribution (debug QA 배포)

`develop` 에 푸시하면 **개발 서버(`PICKFLOW_API_BASE_URL_DEV`)를 보는 debug APK** 가
Firebase App Distribution 의 `qa` 테스터 그룹으로 자동 배포된다.

| 배포 | 워크플로 | 빌드 | 서버 |
|---|---|---|---|
| QA(자동) | `.github/workflows/firebase-distribution.yml` | debug APK | 개발 |
| 사내 검증(수동) | `.github/workflows/cd.yml` | 서명된 release APK | 운영 |
| Play 심사 | `.github/workflows/release-aab.yml` | 서명된 AAB | 운영 |

## 1. 필요한 GitHub Secrets

Settings → Secrets and variables → Actions.

| 이름 | 설명 | 없으면 |
|---|---|---|
| `FIREBASE_APP_ID` | Android 앱 ID (`1:000000000000:android:abcdef...`) | 업로드 실패 |
| `FIREBASE_SERVICE_ACCOUNT` | 서비스 계정 JSON **전체 내용** | 워크플로 즉시 실패 |
| `GOOGLE_SERVICES_JSON` | `app/google-services.json` **전체 내용** | GA 만 비활성(빌드는 성공) |
| `SECRETS_PROPERTIES` | `secrets.properties` **전체 내용**(여러 줄 그대로 붙여넣기) | 빈 기본값으로 빌드(지도/카카오 로그인 미동작) |

> `FIREBASE_APP_ID`, `FIREBASE_SERVICE_ACCOUNT` 는 기존 `cd.yml` 과 같은 시크릿을 공유한다.

## 2. Firebase 콘솔에서 값 얻기

**`FIREBASE_APP_ID`**
1. [Firebase 콘솔](https://console.firebase.google.com) → 프로젝트 선택
2. ⚙️ **프로젝트 설정** → **일반** → "내 앱" 에서 Android 앱(`com.pickflow.app`) 선택
3. **앱 ID** 값(`1:...:android:...`) 복사

**`GOOGLE_SERVICES_JSON`**
- 같은 화면에서 **google-services.json 다운로드** → 파일 내용을 그대로 시크릿 값으로 붙여넣는다.
- 로컬 개발은 이 파일을 `app/google-services.json` 에 둔다(`.gitignore` 처리됨).

**`FIREBASE_SERVICE_ACCOUNT`**
1. 프로젝트 설정 → **서비스 계정** → **모든 서비스 계정 관리**(Google Cloud Console 이동)
2. 서비스 계정 만들기 (예: `app-distribution-uploader`)
3. 역할에 **Firebase App Distribution 관리자 SDK 서비스 에이전트**
   (`roles/firebaseappdistro.admin`) 부여
4. 해당 계정 → **키** → **키 추가 → 새 키 만들기 → JSON** → 다운로드
5. JSON 파일 내용 전체를 시크릿 값으로 붙여넣는다.

> 서비스 계정 JSON 은 절대 커밋하지 않는다. 로컬 파일명은 `firebase-service-account.json`
> 으로 두면 `.gitignore` 가 막아 준다.

## 3. 로컬에서 배포하기

`secrets.properties` 에 두 줄 추가:

```properties
FIREBASE_APP_ID=1:000000000000:android:abcdef0123456789
FIREBASE_SERVICE_ACCOUNT_FILE=firebase-service-account.json
```

(환경변수 `FIREBASE_APP_ID` / `FIREBASE_SERVICE_ACCOUNT_FILE` 가 있으면 그쪽이 우선한다.)

```sh
# 빌드 + 업로드
./gradlew assembleDebug appDistributionUploadDebug

# 테스터 그룹/릴리스 노트를 그때그때 덮어쓰기
./gradlew assembleDebug appDistributionUploadDebug \
  --groups=qa,designer --releaseNotes="스팟 상세 하단 여백 수정"
```

두 값이 모두 비어 있어도 `assembleDebug` / `testDebugUnitTest` 는 그대로 성공한다.
업로드 태스크를 실행할 때만 실패한다.

## 4. 테스터 그룹 관리

Firebase 콘솔 → **App Distribution** → **테스터 및 그룹** 탭.

1. **그룹 추가** 로 `qa` 그룹을 만든다 (그룹 이름이 곧 별칭 — 워크플로가 쓰는 값).
2. 그룹에 테스터 이메일을 추가한다. 초대 메일이 발송되고, 테스터는 기기에서
   초대를 수락한 뒤 App Tester 앱 또는 링크로 설치한다.
3. 그룹 이름을 바꾸면 `app/build.gradle.kts` 의 `groups = "qa"` 도 함께 바꾼다.

빌드마다 다른 그룹으로 보내려면 워크플로를 **Actions → Firebase App Distribution (debug)
→ Run workflow** 로 수동 실행하면서 그룹을 입력한다.

## 5. 릴리스 노트

`app/build.gradle.kts` 가 **최근 커밋 메시지**(`git log -1 --pretty=%s`)를 릴리스 노트로 넣는다.
그래서 커밋 메시지 규칙(`[PV-XX] 한국어 요약`)이 그대로 테스터에게 보인다.
다른 문구가 필요하면 `--releaseNotes=` 로 덮어쓴다.
