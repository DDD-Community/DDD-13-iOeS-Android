# [PV-59] 백엔드 미구현 임시 폴백 — 롤백 가이드

> ## ✅ 2026-09-11 제거 완료 — 이 문서는 이력이다
>
> `core/services/impl/compat/` 를 통째로 삭제하고 DI 를 `DefaultSpotListService` /
> `DefaultSpotMapService` 로 되돌렸다(§3-1 수동 경로). 아래 내용은 **왜 넣었고 무엇을
> 지웠는지**의 기록으로만 남긴다. 되돌릴 것은 더 없다.
>
> **미해결 전제 하나**: 제거 시점에 운영 서버는 아직 구버전이었다(`?theme=SUNLIGHT` → 400
> `C002`, 반복 파라미터는 첫 값만 적용). 운영 배포가 임박했다는 판단으로 먼저 지웠으므로,
> **배포 전까지 release 빌드는 햇살/야경 선택 시 400 이 난다.** 배포 후 §4 검증 절차를
> 운영 주소로 한 번 돌려 확인한다.

---

> **(원문) 이 문서의 목적은 되돌리는 것이다.** 백엔드가 신규 무드(햇살/야경)와 다중 `theme`
> 파라미터를 지원하는 순간, 아래 절차대로 이 계층을 **통째로 삭제**한다.

---

## 1. 왜 넣었나 — 서버 실측 결과

2026-08-04, 운영 서버 `https://pickflow-api.us/api` 직접 호출로 확인한 사실이다.

| 요청 | 응답 | 해석 |
|---|---|---|
| `?theme=SUNSET` | 200, SS 6건 | 정상 |
| `?theme=YUNSEUL` | 200, YS 6건 | 정상 |
| `?theme=SUNLIGHT` | **400 `C002`** 요청 타입이 올바르지 않습니다 | 서버가 값을 모름 |
| `?theme=NIGHT` | **400 `C002`** | 서버가 값을 모름 |
| `?theme=SL` / `?theme=NT` | **400 `C002`** | 2글자 코드도 모름 |
| `?theme=SUNSET,YUNSEUL` (CSV) | **400 `C002`** | CSV 미지원 |
| `?theme=SUNSET&theme=YUNSEUL` (반복) | **200, SS 6건** | ⚠️ **첫 값만 적용, 둘째는 조용히 무시** |

`/v1/spots`와 `/v1/spots/viewport` 모두 동일하게 동작한다.
서버가 실제로 내려주는 `theme` 값도 `YS` / `SS` 두 종뿐이다(전체 조회 18건 기준).

**가장 위험한 건 마지막 줄이다.** 반복 파라미터가 400이 아니라 200을 돌려주므로,
클라이언트는 "다중 필터가 걸렸다"고 믿지만 실제로는 첫 무드만 적용된 결과를 받는다.
이 폴백이 없으면 조용히 잘못된 목록을 보여주게 된다.

## 1-B. ⚠️ 2026-08-18 갱신 — 빌드타입별로 서버가 다르다

PV-85(`3e4ed9d`)로 **debug=개발 서버, release=운영 서버**로 갈렸다. 두 환경의 능력이 다르다.

| | 개발(debug) | 운영(release) |
|---|---|---|
| `SUNLIGHT` / `NIGHT_VIEW` | **200 (실데이터)** ✅ | 400 `C002` ❌ |
| 반복 파라미터 다중 필터 | 미지원 ⚠️ | 미지원 ⚠️ |

그래서 `SERVER_KNOWN_THEMES`가 `BuildConfig.DEBUG`로 분기한다.

- **debug 빌드에는 stub 이 뜨지 않는다** (`STUB_ONLY_THEMES`가 빈 Set) — 실데이터가 나온다
- release 빌드에서만 햇살/야경이 `[STUB]`로 대체된다

즉 이 계층에서 **먼저 사라질 부분은 stub**이고(운영 배포 시), 다중 필터 우회는
백엔드가 배열을 지원할 때까지 남는다.

## 1-C. ⚠️ 2026-09-11 갱신 — 개발 서버는 다중 필터를 지원한다 (QA 제보 원인)

전 페이지를 순회해 재실측한 결과다.

| | 개발(debug) | 운영(release) |
|---|---|---|
| `SUNLIGHT` / `NIGHT_VIEW` | 200 (실데이터) ✅ | 400 `C002` ❌ |
| `?theme=A&theme=B` | **200, A∪B 정상 필터** ✅ | 200 이지만 첫 값만 적용 ⚠️ |

근거:

- dev `?theme=SUNSET` → 29건, `?theme=SUNLIGHT` → 1건,
  `?theme=SUNSET&theme=SUNLIGHT` → **30건(SS 29 + SL 1)** = 정확한 합집합
- dev viewport 도 동일 (`SUNSET` 29 + `YUNSEUL` 33 → 둘 다 62)
- prod `?theme=SUNSET&theme=YUNSEUL` → 28건 전부 SS (첫 값만)

**그런데 다중 필터 폴백이 이미 정상화된 개발 서버에서도 계속 돌고 있었다.** 2개 이상 선택 시
`theme` 없이 전체를 받아 클라이언트에서 걸렀는데, 리스트는 페이지네이션(페이지당 6건)이라
**필터 안 걸린 첫 페이지만 걸러지고 끝난다.** 그 결과 QA 제보처럼

- 조합마다 "일부 스팟만 노출"
- 추천 순/가까운 순에서 전체 개수가 다름(정렬에 따라 첫 페이지 구성이 달라지므로)
- 햇살+야경은 아예 0건(전체 조회 첫 페이지에 노을/윤슬만 담겨 있어 전부 탈락)

이 나타났다.

그래서 `SERVER_SUPPORTS_MULTI_THEME`(= `BuildConfig.DEBUG`)를 두고, **다중을 지원하는
서버에는 선택된 무드 전부를 그대로 위임**한다. 클라이언트 필터 폴백은 운영 서버 전용으로만
남는다 — 운영에도 새 백엔드가 배포되면 이 계층은 통째로 삭제 대상이다(§3).

## 2. 무엇을 하고 있나

`SpotListService` / `SpotMapService` 앞에 데코레이터를 하나씩 끼웠다.

| 선택된 무드 | 서버 요청 | 결과 구성 |
|---|---|---|
| 없음 | `theme` 없이 전체 | 서버 응답 그대로 |
| 1개 (서버가 아는 값) | `?theme=SUNSET` | 서버 응답 그대로 |
| 2개 이상, **다중 지원 서버(dev)** | `?theme=A&theme=B` | 서버 응답 그대로 |
| 2개 이상, 다중 미지원 서버(prod) | `theme` 없이 전체 | **클라이언트 필터**(첫 페이지 한계 — §1-C) |
| 햇살 / 야경만 (prod) | **요청 안 함** | stub 스팟만 |
| 노을 + 야경 (prod) | `?theme=SUNSET` | 서버 노을 + stub 야경 |

- stub 스팟은 이름에 **`[STUB]` 접두사**가 붙어 실데이터와 구분된다.
- stub 은 리스트 **첫 페이지에만** 붙는다(페이지마다 붙이면 그리드 key 가 중복된다).
- `theme` 값이 400을 유발하는 조합은 **절대 서버로 나가지 않는다**.

### viewport 의 알려진 한계

지도 viewport 응답(`SpotSummaryDto`)에는 `theme` 필드가 없어 클라이언트 재필터가 불가능하다.
개발 서버는 다중 `theme` 를 처리하므로(§1-C) **debug 빌드에서는 이 한계가 없다.**
운영 서버에서만 노을 + 윤슬을 함께 고르면 지도에 필터가 걸리지 않은 전체 마커가 나오며,
이건 백엔드 배포 시 자동 해소된다.

## 3. 되돌리는 법

### 3-1. 제거 (유일한 경로)

**부분 비활성화 스위치는 두지 않는다.** 반쯤 켜진 상태가 생기면 지금 어떤 동작인지
헷갈리기 때문이다. 되돌리는 방법은 이 계층을 통째로 지우는 것 하나뿐이다.

이 폴백은 **독립 커밋**으로 분리돼 있다. 커밋 제목으로 찾아 되돌린다.

```bash
# 폴백 커밋 찾기
git log --oneline --grep="백엔드 미구현 임시 폴백"

# 되돌리기
git revert $(git log --format=%H --grep="백엔드 미구현 임시 폴백" -1)
```

> sha 를 문서에 박아두지 않는 이유: rebase/amend 로 sha 가 바뀌면 문서가 조용히 거짓이 된다.
> 커밋 제목은 그대로 남으므로 제목으로 찾는 편이 안전하다.

수동으로 지울 경우 대상은 다음 4개 파일과 2줄이다.

| 대상 | 조치 |
|---|---|
| `core/services/impl/compat/MoodBackendCompat.kt` | 삭제 |
| `core/services/impl/compat/MoodCompatSpotListService.kt` | 삭제 |
| `core/services/impl/compat/MoodCompatSpotMapService.kt` | 삭제 |
| `app/src/test/.../compat/MoodBackendCompatTest.kt` | 삭제 |
| `app/di/ServiceModule.kt` | `MoodCompatSpotListService` → `DefaultSpotListService` |
| `app/di/ServiceModule.kt` | `MoodCompatSpotMapService` → `DefaultSpotMapService` |

`compat/` 디렉터리가 통째로 사라지면 끝이다. 프로덕션 로직은 이 계층 밖으로 새지 않았다.

### 3-2. 함께 확인할 것 — 서버 enum 코드

**2026-08-14 API 문서로 확정됨**: 햇살 = `SUNLIGHT`, 야경 = **`NIGHT_VIEW`**.
초기 가정이던 `NIGHT` 는 정정 완료(`docs/PV-59/api-spec-mapping.md` §3).
남은 미확인 사항은 **응답의 2글자 코드**(`SL`/`NV`는 추정값)와 **다중 전달 지원 여부**다.
문서상 `theme` 는 단일 string 이라 **다중선택은 스펙에 없다** — 같은 문서 §4 참고.

서버가 또 다른 코드를 쓰면
아래 두 곳만 고치면 된다(다른 곳엔 문자열 리터럴이 없다).

| 파일 | 심볼 |
|---|---|
| `core/services/protocols/Spot.kt` | `enum class SpotTheme` |
| `core/network/mapper/SpotMapper.kt` | `parseTheme` |

두 곳 모두 `// PV-59 백엔드 확정시 변경 가능성 있음` 주석이 달려 있다.

## 4. 복원 후 검증 절차

1. 서버가 신규 값과 다중 파라미터를 받는지 먼저 확인한다.

   ```bash
   B="https://pickflow-api.us/api/v1/spots?page=0&sort=RECOMMENDED"
   curl -s "$B&theme=SUNLIGHT"            | head -c 200   # 200 이어야 한다
   curl -s "$B&theme=NIGHT"               | head -c 200   # 200 이어야 한다
   curl -s "$B&theme=SUNSET&theme=YUNSEUL" | python3 -c \
     'import sys,json,collections;d=json.load(sys.stdin);print(collections.Counter(s["theme"] for s in d["data"]["spots"]))'
   # SS 와 YS 가 **둘 다** 나와야 진짜 다중 필터다. SS만 나오면 아직 미구현이다.
   ```

2. 위가 전부 통과하면 §3-1 로 제거한다.
3. `./gradlew :app:testDebugUnitTest` 그린 확인.
4. 에뮬레이터에서 무드 2개 선택 후 OkHttp 로그에 `theme=A&theme=B` 가 나가고
   응답이 200인지, 결과에 두 무드가 섞여 있는지 확인한다.

## 5. 제거 후 기대 동작

- 햇살/야경에 **실데이터**가 나온다(`[STUB]` 접두사 사라짐).
- 무드를 2개 이상 골라도 서버가 필터하므로 클라이언트 필터링이 사라진다.
  → 페이지네이션이 정확해진다(현재는 클라 필터 때문에 페이지당 건수가 들쭉날쭉하다).
- 지도 viewport 도 다중 필터가 정상 적용된다(§2 한계 해소).

## 6. 관련 문서

- 구현 프롬프트: `docs/PV-59/mood-filter-expansion-implementation-prompt.md`
- 후속 논의: `docs/PV-59/mood-filter-expansion-discussion.md` (§1 서버 enum 코드)
- UI 테스트 케이스: `docs/PV-59/ui-test-cases.md`
