---
name: release-build
description: Pickflow 릴리스 AAB(Play 업로드용)를 로컬에서 서명해 빌드하고 검증까지 수행한다. 사용자가 "릴리스 빌드", "release AAB 빌드", "Play 업로드용 빌드", "번들 만들어줘", "서명된 aab 뽑아줘" 류로 요청할 때 사용. 절차 문서는 docs/release-build.md, 버전 규칙은 docs/ci-cd.md. 버전 결정과 Play 업로드는 사람 판단이 필요하므로 완전 자동화하지 말고 각 게이트에서 확인을 받는다.
---

# release-build

로컬에서 **서명된 release AAB** 를 뽑는 실행형 스킬. 상세 배경은 `docs/release-build.md`,
버전 번호 규칙은 `docs/ci-cd.md §버전 규칙 / §버전 기록` 을 단일 출처로 삼는다.

이 스킬은 실수 비용이 큰 작업(versionCode 영구 소모, debug 키 오서명, 비공개 키 누락)을
막기 위한 **게이트 강제**가 핵심이다. 절차를 건너뛰지 않는다.

## 언제 쓰는가
- "릴리스/release AAB 빌드", "Play 업로드용 빌드", "서명된 aab 뽑아줘" 등의 요청.
- debug 빌드(`assembleDebug`)나 CI 태그 릴리스(`release-aab.yml`)를 원하는 경우엔 쓰지 않는다.

## 절대 규칙
- `~/keystores/pickflow-release-credentials.txt` 의 비밀번호를 **출력/로그/커밋에 절대 노출하지 않는다.**
  반드시 `source` 로 환경변수에 로드해서만 사용하고, `echo $KEYSTORE_PASSWORD` 같은 확인도 하지 않는다.
- keystore·`secrets.properties`·`google-services.json` 은 `.gitignore` 대상 — 커밋 스테이징에 넣지 않는다.

## 절차 (게이트 순서 고정)

### 1. 버전 결정 — 사용자 확인 필수
- `docs/ci-cd.md §버전 기록` 표에서 **직전 versionCode** 를 읽는다.
- `versionCode` 는 **단조증가** — 직전 값 `+1`. (인코딩 아님. 예: 1000102 다음은 1000103.)
- `versionName`(마케팅 `X.Y.Z`)은 versionCode 와 무관하게 정한다.
- **정한 값을 사용자에게 제시하고 확인받은 뒤** 진행한다. (versionCode 는 재사용 불가라 임의로 정하지 않는다.)
- 확인되면 `app/build.gradle.kts` fallback(versionCode, 필요 시 versionName)을 갱신 커밋할지 제안한다(단일 출처).

### 2. 사전 파일 존재 확인
아래가 모두 있어야 한다. 없으면 중단하고 사용자에게 알린다.
```sh
ls ~/keystores/pickflow-release.keystore ~/keystores/pickflow-release-credentials.txt
ls secrets.properties app/google-services.json
```
- `secrets.properties` 없음 → 지도/로그인 죽은 채 빌드됨(중단하고 경고).
- `google-services.json` 없음 → GA 비활성(빌드는 됨, 경고만).

### 3. 빌드
```sh
set -a; source ~/keystores/pickflow-release-credentials.txt; set +a
export VERSION_NAME=<1에서 확정> VERSION_CODE=<1에서 확정>
./gradlew bundleRelease
```
- 산출물: `app/build/outputs/bundle/release/app-release.aab`
- 시간이 걸리면 백그라운드 실행 후 완료 통지를 기다린다.

### 4. 검증 — 통과 못 하면 업로드 금지
```sh
AAB=app/build/outputs/bundle/release/app-release.aab
# (a) 버전 확인
unzip -p "$AAB" base/manifest/AndroidManifest.xml | strings | grep -A1 -i versionName
bundletool dump manifest --bundle="$AAB" | grep -iE "versionCode|versionName"   # 있으면
# (b) release(upload) 키 서명 확인 — debug 키면 실패로 간주
jarsigner -verify -verbose -certs "$AAB" | grep -iE "CN=|jar verified" | head
```
- versionName/Code 가 1에서 정한 값과 일치하는지, 서명 CN 이 upload key 인지 확인해 **결과를 사용자에게 보고**한다.

### 5. 아카이브 네이밍 (검증 통과 직후)
`bundleRelease` 산출물명은 항상 `app-release.aab` 고정이라 다음 빌드가 덮어쓴다.
버전 식별용 복사본을 남긴다. 형식: `pickflow-v<versionName>-<versionCode>.aab`.
```sh
cp "$AAB" "app/build/outputs/bundle/release/pickflow-v${VERSION_NAME}-${VERSION_CODE}.aab"
```
- 복사본을 배포/공유/업로드에 쓴다. 원본은 그대로 둔다. Play 업로드는 파일명 무관(로컬 추적용).

### 6. 마무리 안내 (실행 아님 — 리마인드)
- 산출물 경로 안내(아카이브본 이름 포함).
- Play Console 업로드는 사람이 수행(`docs/release-build.md §4`).
- 릴리스 확정 후: `docs/ci-cd.md §버전 기록` 표 한 줄 추가 + `app/build.gradle.kts` fallback 갱신 커밋을 제안한다.

## 산출물
```
app/build/outputs/bundle/release/app-release.aab   ← 서명된 릴리스 번들
```
