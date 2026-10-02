# Pickflow ProGuard rules

# Kakao SDK — common/auth/network AAR 에 consumer 규칙이 없다.
# ClientError 가 ClientErrorCause 등 enum 필드를 이름으로 getField() 하고, 모델은 Gson 으로 직렬화하므로
# model 패키지 필드명을 보존해야 한다(미보존 시 실행 직후 NoSuchFieldException: TokenNotFound 크래시).
# https://developers.kakao.com/docs/ko/android/getting-started
-keep class com.kakao.sdk.**.model.* { <fields>; }
