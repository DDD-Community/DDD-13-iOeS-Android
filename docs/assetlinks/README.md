# assetlinks.json 배포 가이드

App Links(`android:autoVerify="true"`) 검증을 위해 BE가 아래 URL에서 이 파일을 서빙해야 한다.

```
https://pickflow-api.us/.well-known/assetlinks.json
```

- `Content-Type: application/json` 으로 응답할 것 (리다이렉트 없이 200).
- 검증: https://developers.google.com/digital-asset-links/tools/generator 또는
  `adb shell pm verify-app-links --re-verify com.pickflow.app` (API 31+).

## 현재 프로덕션 이슈 (2026-07-22 확인)

`https://pickflow-api.us/.well-known/assetlinks.json` 이 **JSON 이 아니라 랜딩 HTML**
(`Content-Type: text/html`, 스토어 리다이렉트 페이지)을 반환하고 있다.
이로 인해:

1. App Links 자동 검증 실패 → Android 12+ 에서 공유 링크가 항상 브라우저로 열림.
2. 랜딩 페이지가 Android UA 에서 앱 열기 시도 없이 바로 Play Store 로 `location.replace`
   → **공유 링크 탭 = 무조건 스토어행**.

BE 조치 사항:
- 랜딩 catch-all 라우트보다 **앞에** `/.well-known/assetlinks.json` 전용 라우트를 두고
  본 디렉터리의 `assetlinks.json` 을 그대로 서빙.
- 랜딩 페이지에 Android 분기 추가 — `intent://spot/{token}#Intent;scheme=pickflow;`
  `package=com.pickflow.app;S.browser_fallback_url=<PlayStoreURL>;end`
  (레퍼런스 구현: `../../../pickflow-preview-server/server.js`).
- 랜딩 페이지의 Play Store 링크가 `com.ioes.pickflow` 로 되어 있는 곳이 있다면
  실제 applicationId 인 `com.pickflow.app` 으로 통일할 것.

## 지문 구성

| 키 | SHA-256 | 상태 |
|---|---|---|
| debug (`~/.android/debug.keystore`) | `E8:A1:EB:05:...:17:95` | 반영됨 |
| release (Play Console 앱 서명 키 인증서) | `27:F1:FA:47:...:DA:A2` | 반영됨 |

## release 지문 추출 방법

릴리스(업로드) keystore 는 로컬 `~/keystores/pickflow-release.keystore` 에 보관하며,
GitHub Secrets(`KEYSTORE_BASE64` 등)에도 base64 로 등록되어 있다 (2026-07-23 등록).
로컬 보관본이 있으면 아래로 바로 추출 가능:

```sh
keytool -list -v -keystore ~/keystores/pickflow-release.keystore -alias pickflow | grep SHA256
```

주의: 위 지문은 **업로드 키**다. assetlinks 에 넣는 release 지문은 Play App Signing 의
**앱 서명 키**(Play Console > 앱 무결성) SHA-256 이어야 한다. 로컬 보관본이 없을 때의 대안:

1. **CI에서 출력** — `cd.yml`의 keystore 복원 스텝 뒤에 임시로 추가 후 Actions 로그에서 확인:
   ```yaml
   - name: Print release cert fingerprint
     run: keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "${{ secrets.KEYSTORE_PASSWORD }}" | grep SHA256
   ```
2. **CI 산출물 APK에서 추출** — 릴리스 APK를 받아 로컬에서:
   ```sh
   keytool -printcert -jarfile app-release.apk | grep SHA256
   ```
3. **Play App Signing 사용 시** — Play Console > 설정 > 앱 무결성 > 앱 서명 키 인증서의 SHA-256을 사용해야 한다 (업로드 키가 아님).

추출한 값으로 `assetlinks.json`의 `RELEASE_KEY_SHA256_지문으로_교체` 항목을 교체한다.
