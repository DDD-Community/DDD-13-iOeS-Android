# 릴리스 AAB 빌드 행동지침 (로컬)

Play Console 업로드용 **서명된 release AAB** 를 로컬에서 직접 빌드하는 절차.
CI(`release-aab.yml`, 태그 트리거)를 쓰지 않고 손으로 뽑을 때 사용한다.

> CI 로 뽑는 방법은 [`ci-cd.md`](./ci-cd.md) 의 "Play 심사용 AAB 릴리스" 참고.
> 버전 번호 규칙은 [`ci-cd.md`](./ci-cd.md) 의 "버전 규칙" 을 **먼저** 읽는다.

---

## 0. 사전 준비물 (한 번만 확인)

| 항목 | 경로 | 없으면 |
|---|---|---|
| 서명 keystore | `~/keystores/pickflow-release.keystore` | 팀 키 보관처에서 복구 (분실 시 Play 업로드 불가) |
| 서명 자격증명 | `~/keystores/pickflow-release-credentials.txt` | 위와 동일. `KEY=VALUE` 형식 |
| 앱 비공개 키 | `<repo>/secrets.properties` | 없으면 defaults(빈 값)로 빌드 → 지도/로그인 미동작 |
| GA 설정 | `<repo>/app/google-services.json` | 없으면 GA 수집만 비활성(빌드는 성공) |

- 위 3개 파일은 모두 **`.gitignore` 대상(커밋 금지)** 이다.
- `credentials.txt` 안의 비밀번호는 문서/코드/채팅 어디에도 붙여넣지 않는다.

## 1. 버전 결정

`ci-cd.md §버전 규칙` 에 따라 이번 빌드의 `VERSION_NAME` / `VERSION_CODE` 를 정한다.

- **versionCode 는 단조증가.** `ci-cd.md §버전 기록` 표에서 직전 값을 확인해 **`+1`** 한다 (다음 값 = `1000103`).
- **Play 에 올린 versionCode 는 재사용 불가** — 반려 재업로드도 새 versionCode(+1)가 필요하다.
- versionName 은 마케팅 버전(`X.Y.Z`)이며 versionCode 와 무관하게 정한다.
- 정한 값으로 **`app/build.gradle.kts` fallback 을 갱신 커밋**해 두면 로컬/CI 가 같은 값을 쓴다(단일 출처).

## 2. 빌드

리포 루트에서 실행한다.

```sh
cd /Users/kangdong-yeong/Desktop/DDD-13th-Workspace/DDD-13-iOeS-Android

# 서명 자격증명을 환경변수로 로드 (파일이 이미 KEY=VALUE 형식이라 그대로 source)
set -a; source ~/keystores/pickflow-release-credentials.txt; set +a

# 이번 릴리스 버전 지정 (§1 에서 정한 값)
export VERSION_NAME=1.0.1
export VERSION_CODE=1000102

./gradlew bundleRelease
```

- `KEYSTORE_FILE` 이 존재하면 release 키로 서명된다(없으면 debug 키로 서명 — 그건 스토어 업로드 불가).
- `set -a … set +a` 로 감싸면 `source` 한 변수들이 자식 프로세스(gradle)로 export 된다.
- 산출물: **`app/build/outputs/bundle/release/app-release.aab`**

## 3. 빌드 검증 (업로드 전 필수)

```sh
AAB=app/build/outputs/bundle/release/app-release.aab

# (a) versionName 빠른 확인 (base manifest 는 protobuf → strings 로 문자열만 보임)
unzip -p "$AAB" base/manifest/AndroidManifest.xml | strings | grep -A1 -i versionName

# (a') versionCode(정수)까지 정확히 보려면 bundletool 사용
#   brew install bundletool  또는  ~/Library/Android/sdk/... 의 bundletool.jar
bundletool dump manifest --bundle="$AAB" | grep -iE "versionCode|versionName"

# (b) debug 키가 아니라 release(upload) 키로 서명됐는지 — 인증서 CN 확인
jarsigner -verify -verbose -certs "$AAB" | grep -iE "CN=|jar verified" | head
```

- 서명 인증서의 지문이 `~/keystores/upload_certificate.pem`(Play App Signing 에 등록한 upload key)과 일치해야 한다.
- `versionCode` 가 직전 업로드본보다 **커야** Play 가 받는다.

### 아카이브 네이밍

`bundleRelease` 산출물명은 항상 `app-release.aab` 로 **고정**이라 다음 빌드가 덮어쓴다.
검증 통과 직후, 버전을 식별할 수 있는 이름으로 **복사본**을 남긴다.

```
pickflow-v<versionName>-<versionCode>.aab      # 예: pickflow-v1.0.1-1000102.aab
```

```sh
cp "$AAB" "app/build/outputs/bundle/release/pickflow-v${VERSION_NAME}-${VERSION_CODE}.aab"
```

- `versionCode` 가 유일 키라 충돌이 없고 숫자 정렬이 된다. `versionName` 은 사람이 읽기 위함.
- Play 업로드/재서명은 파일명과 무관 — 이 규칙은 **로컬 보관·공유·추적용**이다.
- 원본 `app-release.aab` 는 그대로 두고(다음 빌드가 덮어씀), 복사본을 배포/공유에 쓴다.

## 4. Play Console 업로드

1. [Play Console](https://play.google.com/console) → 해당 앱 → **테스트 및 배포**
2. 트랙 선택: 내부 테스트 → 비공개 테스트 → 프로덕션 (검증 단계에 맞게)
3. **새 버전 만들기** → 아카이브본(`pickflow-v<…>.aab`) 업로드
4. Play App Signing 이 활성화돼 있으면, 우리가 올린 upload key 서명 AAB 를 Play 가
   **앱 서명 키로 재서명**해 배포한다(정상). upload key 는 업로드 인증용일 뿐.
5. 출시 노트 작성 후 검토 → 출시.

## 5. 릴리스 후 정리

- `ci-cd.md §버전 기록` 표에 이번 태그/버전/상태/내용을 **한 줄 추가**한다.
- 로컬 fallback(`app/build.gradle.kts` 의 `versionName`/`versionCode`)을 이번 값과 맞춰 커밋한다.
  (디버그 빌드 표기와 릴리스가 어긋나면 기기 QA 때 혼선.)
- 태그로도 기록을 남기려면: `git tag vX.Y.Z[-N] && git push origin vX.Y.Z[-N]`
  (이 경우 `release-aab.yml` 이 CI 에서 동일 AAB 를 한 번 더 뽑아 GitHub Release 에 첨부.)

---

## 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| AAB 가 debug 키로 서명됨 | `KEYSTORE_FILE` 미설정. §2 의 `source` 를 빠뜨렸거나 경로 오타 |
| `Keystore was tampered with, or password was incorrect` | `KEYSTORE_PASSWORD`/`KEY_PASSWORD` 불일치 — credentials.txt 재확인 |
| Play 가 "이미 사용된 버전 코드" 거부 | `versionCode` 중복. `-N` 올려 재빌드 (§1) |
| 지도/카카오 로그인 미동작 | `secrets.properties` 누락 → 빈 값 빌드. 실제 키 채운 뒤 재빌드 |
| GA 이벤트 안 들어옴 | `app/google-services.json` 누락 시 수집 비활성. 파일 확인 후 재빌드 |
| 기기에 디버그 빌드 설치 안 됨 | 릴리스 빌드가 이미 설치됨 → 서명 불일치. `adb uninstall com.pickflow.app` 후 설치 |
