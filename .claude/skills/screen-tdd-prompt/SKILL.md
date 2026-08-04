---
name: screen-tdd-prompt
description: Pickflow Android의 새 화면 티켓(Jira KAN-XX)에 대해 TDD A→B→C 3단계를 강제하는 단일 구현 프롬프트 MD를 생성한다. 사용자가 새 화면 티켓·Figma 노드·API 정보를 들고 와서 "구현 프롬프트 만들어줘" 류로 요청할 때 사용. 결과물은 `docs/KAN-XX/<screen>-implementation-prompt.md` 한 장이며, 이후 Claude가 이 한 장만 읽고도 화면을 구현할 수 있을 만큼 자기완결적이어야 한다.
---

# screen-tdd-prompt

Pickflow Android의 새 화면을 구현할 때 사용할 **단일 구현 프롬프트 MD**를 생성하는 스킬이다.
이전 화면 티켓 선례(`docs/KAN-76/login-implementation-prompt.md`)를 기준 골격으로 삼되, 다음 4개 게이트를 **반드시** 포함시킨다:

1. **TDD A→B→C 3단계 엄격 분리** — 단계 건너뛰기 금지, 각 단계의 진입/종료 조건 명시
2. **`docs/KAN-XX/ui-test-cases.md` 작성을 Phase B 산출물로 강제** — 뷰 코드 착수 전 통과 의무
3. **Paparazzi 스냅샷 게이트** — Phase C 진입 조건이자 머지 전 통과 필수
4. **에셋 입력 매트릭스** — 컬러/아이콘/폰트 토큰을 코딩 시작 전에 표로 확정

## 언제 이 스킬을 쓰는가

- 사용자가 새 화면 티켓(`KAN-XX`)을 가지고 와서 "구현 프롬프트", "통합 프롬프트", "implementation prompt", "TDD 프롬프트" 등을 요청할 때
- 사용자가 Figma 링크 + 화면 단위 작업을 던지면서 "어떻게 시작할지 계획부터 잡아줘" 류로 요청할 때

화면이 아닌 작업(core 인프라만, 디자인 시스템 토큰만, 빌드 설정 등)에는 사용하지 않는다.

## 산출물

```
docs/KAN-XX/
  ├── <screen-slug>-implementation-prompt.md   ← 메인 결과물 (화면별 사실만, ~150줄)
  └── ui-test-cases.md                          ← Phase B에서 채울 빈 셸 (헤더만 미리)
```

## 관련 디렉토리 구조

```
.claude/skills/screen-tdd-prompt/
  SKILL.md                          ← 본 문서 (게이트 정의 + 입력 수집 + 절차)
  prompt-template.md                ← 화면별 사실 위주의 슬림 템플릿

docs/phases/                        ← 모든 화면이 공유하는 방법론 (팀 가시성을 위해 docs 하위)
  phase-a-viewmodel-tdd.md          ← Gate 1A 운용 가이드
  phase-b-ui-tdd.md                 ← Gate 1B + Gate 2 운용 가이드
  phase-c-snapshot.md               ← Gate 1C + Gate 3 운용 가이드
```

**왜 분리했나**: 600+줄 단일 프롬프트는 (a) "lost in the middle" 효과로 중간 지시 약화, (b) Phase A 작업 중에 Phase C 디테일까지 워킹 메모리에 같이 들어와 단계 게이트 무력화. 방법론(모든 화면 공통)을 리프로 빼고 화면별 프롬프트는 사실만 담는다.

**읽는 시점**: 화면별 프롬프트는 항상 처음 읽되, `docs/phases/phase-a/b/c-*.md` 리프는 **해당 단계에 진입할 때만** read한다. 미리 다 읽어두지 않는다 — 단계 격리가 게이트의 본체다.

## 입력으로 사용자에게 받아야 하는 정보

프롬프트를 만들기 전에 아래 항목이 모두 모였는지 확인한다. 빠진 게 있으면 사용자에게 물어본다.

| # | 항목 | 비고 |
|---|---|---|
| 1 | Jira 티켓 번호 | 예: `KAN-76`. 티켓 본문(스코프/AC)은 자동 조회 수단이 없으므로 사용자에게 붙여달라고 요청 |
| 2 | 브랜치명 | `feature/KAN-XX`. `pickflow-worktree-setup` 스킬로 워크트리가 이미 준비돼 있다고 가정 |
| 3 | 화면 이름 / slug | slug는 파일명용 kebab-case (예: `spot-detail`) |
| 4 | Figma 파일 키 + 루트 노드 ID | URL에서 추출. node-id의 `-`는 `:`로 변환 |
| 5 | 컴포넌트별 Figma node-id 매핑 | nav/header/section/footer 등 — Phase C UI 검증 루프 표에 들어감 |
| 6 | API 엔드포인트 매핑 | UI 동작 ↔ HTTP method/path/body 표 |
| 7 | 스코프 안/밖 | "이번에 안 함" 항목까지 명시 |
| 8 | 정책 결정 | 일몰 윈도우, 거리 단위, 비로그인 처리 등 사용자 확정 내용 |
| 9 | 외부 앱 연동 | 네이버 지도/공유 Intent 등 스킴, fallback URL |
| 10 | 에셋 매트릭스 원천 | Figma dev mode hex/icon export — **코딩 시작 전에 확정** |

## 골격 — 프롬프트 템플릿 사용

같은 디렉토리의 `prompt-template.md`를 베이스로 채운다. 템플릿 안에 `{{TICKET}}`, `{{SCREEN_NAME}}` 등 플레이스홀더가 있고, 위에서 받은 정보를 치환한다.

치환 규칙:
- `{{TICKET}}` → `KAN-76` (디렉토리/브랜치/커밋에 쓰는 티켓 ID)
- `{{SCREEN_NAME}}` → 자연어 화면 이름, 예: `스팟 상세`
- `{{SCREEN_NAME_PASCAL}}` → PascalCase 심볼 조각, 예: `SpotDetail`
- `{{SCREEN_SLUG}}` → kebab-case 파일명 조각, 예: `spot-detail`
- `{{FEATURE_PKG}}` → `feature/` 하위 패키지명(소문자 무구분), 예: `spotdetail`
- `{{FIGMA_FILE_KEY}}` → `0oGEIr4oCzpvj4bkGtE5Oa`
- `{{FIGMA_ROOT_NODE_ID}}` → `686:18705`
- `{{BRANCH}}` → `feature/KAN-76`
- 표·리스트 자리들은 사용자 입력으로 채우거나, 정보가 없으면 `<!-- TODO: 합의 필요 -->` 코멘트와 함께 비워둔다

치환 후 빈 섹션이 남으면 그 섹션이 **현재 시점에 합의되지 않았다는 신호**다. 사용자에게 확인한다.

## 절대 빼지 말아야 할 항목 (강제 게이트 — what & why)

이 4개 게이트가 빠진 프롬프트는 머지 금지. **템플릿이 이미 포함**하고 있으므로 삭제하지 말 것. 운용 디테일(진입/종료 조건, 안티 패턴 등)은 `docs/phases/*.md` 리프 문서에 있음.

| Gate | 무엇 | 왜 | 운용 디테일 |
|---|---|---|---|
| **1** | TDD A→B→C 직렬 3단계 | 단계 격리 없으면 Phase A에서 Composable 손대고, B 없이 스냅샷 찍게 됨. 회귀 보호 무력화 | `docs/phases/phase-a-viewmodel-tdd.md`, `phase-b-ui-tdd.md`, `phase-c-snapshot.md` |
| **2** | `docs/KAN-XX/ui-test-cases.md` 작성 | ui-test-cases.md가 스냅샷 매트릭스의 단일 진실 소스. 표 없으면 Phase C는 임기응변 | `docs/phases/phase-b-ui-tdd.md` (8컬럼 정의, 최소 커버리지) |
| **3** | Paparazzi 스냅샷 | 비주얼 회귀를 사람 눈이 아니라 테스트가 잡는 마지막 방어선. record 블라인드 덮어쓰기 금지 | `docs/phases/phase-c-snapshot.md` (JUnit4 제약·고정값·record 운용) |
| **4** | 에셋 입력 매트릭스 (§9.1, §9.2) | 코딩 시작 후 Figma 재방문 비용 폭발. 컬러/아이콘 토큰을 표로 먼저 확정 | `prompt-template.md` §9 (Phase A 진입 차단 조건) |

각 게이트가 화면 프롬프트의 어디에 박혀 있는지:
- Gate 1 → §10 (오케스트레이션) + §15 (작업 순서)
- Gate 2 → §10 Phase B 라인 + §14 체크리스트
- Gate 3 → §10 Phase C 라인 + §14 체크리스트
- Gate 4 → §9 (매트릭스 본체) + 자가 점검 체크박스

## 절차 (스킬 호출 시 단계)

**프롬프트 생성 시점 (스킬이 호출된 직후)**

1. 사용자에게 위 "입력 정보 10항목" 중 누락된 것 확인. Jira 티켓 본문을 받아 스코프·정책 결정 초안을 채운다
2. `prompt-template.md` 읽어 골격 확보. **`docs/phases/*.md`는 이 시점에 읽지 않는다.**
3. `docs/KAN-XX/` 디렉토리 생성
4. 치환·작성하여 `docs/KAN-XX/<SCREEN_SLUG>-implementation-prompt.md` 저장
5. 빈 셸인 `docs/KAN-XX/ui-test-cases.md`도 같이 생성 (헤더만, 본문은 Phase B에서 채움)
6. 사용자에게 결과 경로와 "비어 있는 섹션"(합의 필요 항목) 목록을 알려줌

**실제 화면 구현 시점 (생성된 프롬프트로 작업할 때)**

1. `docs/KAN-XX/<SCREEN_SLUG>-implementation-prompt.md`를 read
2. §9 에셋 매트릭스 미채움이면 채우는 작업부터 (Gate 4)
3. Phase A 진입 직전 → `docs/phases/phase-a-viewmodel-tdd.md` read → 작업
4. Phase A 종료 → `docs/phases/phase-b-ui-tdd.md` read → 작업
5. Phase B 종료 → `docs/phases/phase-c-snapshot.md` read → 작업
6. §11~§14 마무리 후 PR (`gh pr create`, 제목·본문에 `KAN-XX` 명시)

> 단계 진입할 때만 해당 리프를 read. 미리 다 읽어두지 않는다.

## 같이 쓰면 좋은 도구

- Figma MCP(`get_design_context` / `get_screenshot`) — 컬러/아이콘 토큰 추출, 컴포넌트별 비교 스크린샷. **세션에 연결돼 있지 않을 수 있다.** 미연결이면 사용자에게 Figma dev mode의 hex/사이즈를 직접 받아 §9 매트릭스를 채운다
- `gh pr create` — 저장소는 `DDD-Community/DDD-13-iOeS-Android`. 이슈 트래킹은 GitHub이 아니라 **Jira**이므로 `Closes #NN` 자동 종료는 쓰지 않고, 제목·본문에 `KAN-XX`를 명시한다
- `pickflow-worktree-setup` 스킬 — 티켓 워크트리·브랜치 준비(선행 단계)
- 포맷은 ktlint 기본 규칙(Kotlin 표준 4-space) — `ARCHITECTURE.md §3.7`

## 안티 패턴

- 빈 섹션을 "추후"라고 적고 넘어가기 → 합의되지 않은 채 코딩 진입 → Phase A부터 흔들림
- TDD 단계 병렬화 → 단계 의미 사라짐. A→B→C는 직렬이다
- 스냅샷을 "있으면 좋고" 수준으로 약화 → Gate 3 무력화. **머지 차단 조건**으로 유지
- 스냅샷 깨질 때 분석 없이 `recordPaparazziDebug`로 덮어쓰기 → 회귀를 정상화시킴
- 에셋 매트릭스 없이 코드부터 시작 → Figma 재방문 비용 폭발. 매트릭스가 1차 산출물
- 티켓 스코프와 프롬프트 §1 스코프가 어긋난 채 진행 → PR 리뷰 단계에서 재작업
- Phase A(JUnit5)와 Phase B·C(JUnit4) 테스트 API 혼용 → `@get:Rule` 무시로 룰 미적용
