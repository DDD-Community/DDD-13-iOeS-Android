# KAN-76 — Login (Android)

> iOS KAN-76 `login-implementation-prompt.md`의 Android 컨텍스트 이식판.

## 산출물

- `feature/login/LoginViewModel.kt` (구현 완료)
- `feature/login/LoginScreen.kt` (구현 완료)
- `core/services/protocols/KakaoAuthProvider.kt`, `SocialLoginService.kt`, `TokenStore.kt`, `AuthService.kt`
- 단위 테스트: `app/src/test/.../LoginViewModelTest.kt` (2 케이스 그린)

## 다음 작업

- Phase B: Compose UI Test 추가 — 카카오 버튼 탭 → 진행 인디케이터 → 성공 시 `onLoggedIn` 콜백.
- Phase C: 라이트/다크 스냅샷.
- 실 카카오 SDK 연동: `StubKakaoAuthProvider` → `RealKakaoAuthProvider`(KakaoSDK `UserApiClient.loginWithKakaoTalk/Account`).
- Apple/Google 로그인 도입 시 `SocialProvider` 확장.

## 흐름

1. `LoginScreen` → `loginWithKakao()` → `KakaoAuthProvider.login()` (SDK)
2. → `SocialLoginService.loginWith(KAKAO, ...)` → 백엔드 세션 토큰 발급
3. → `TokenStore.save(...)` → `_session = Loaded`
4. → `onLoggedIn()` 콜백 호출
