# 아키텍처 및 의사결정

```
Mihon (host)
    ↓ SourceFactory — 소스 목록 (호스트가 재로딩 시 생성)
BookOrbitSource(libId 또는 slotId) ──→ LibraryRegistry (캐시 + 안전한 불변 id)
    ↓                               └─→ Settings (server URL / auth / 매핑)
BookOrbitRepository → BookOrbitApi → BookOrbit server (실제 API 계약은 조사)
    ↓
ComicEligibility (CBZ, CBR, CB7만) → Manga / Chapter / Page 변환
```

## 두 단계 소스 UX

1. 가능성 검증: 서버 라이브러리를 한 번 동기화해 캐시한 뒤 SourceFactory가 캐시된 라이브러리 리스트로 소스 객체를 생성. 라이브러리 이름 변화 및 신규 추가에 Mihon 재시작/앱 리로드가 필요한지 실기기 테스트.
2. 동적 소스 생성이 안정적이지 않을 때: 6개 고정 슬롯 폴백. 사용자는 각 슬롯의 설정에서 라이브러리를 선택해 별도 소스로 탐색. 확장 초기 스캐폴드는 6개 슬롯을 노출함.

### ID 설계

- `Source.id`는 반드시 불변: 이름/순번/페이지 번호에서 계산 금지.
- 동적 소스: 서버 인스턴스 식별과 서버 libraryId의 결합을 안정적으로 매핑. 충돌 검증 및 여러 서버 전환 정책 필요.
- 슬롯 방식: 슬롯 index 기반 id를 **지속**해서 유지. 연결 라이브러리를 재지정하면 기존 저장 시리즈가 새 라이브러리로 엉키지 않게 사용자 안내/마이그레이션 필요.
- 작품과 챕터 URL도 표시 이름이 아니라 안정적인 서버 식별자를 사용하고, URL/파라미터 인코딩을 검증.

### 목록+형식 필터

- 서버가 만화 형식으로 직접 필터링 가능하면 서버 측에서 수행.
- 불가능하면 한 페이지의 시리즈를 가져와 각각의 book/file format을 검사. 6개가 전부 EPUB인 경우 다음 페이지까지 읽어 Mihon의 유효 결과를 채우는 **bounded look-ahead**.
- 쿼리+필터+라이브러리 ID 단위로 페이지 커서를 관리; 캐시 TTL/오류 복구/요청 폭주 방지 필요.
- 실패한 검사는 읽을 수 있는지 모르는 상태: 허위로 CBZ로 판단하거나 묵묵히 전체 목록에 섞지 않음.

### UI 한계

`supportsLatest=false`는 Mihon의 '최신' 제공을 끄는 것이지, 인기/탐색 UI 전체를 재구성하는 것이 아님. `인기`는 실제로 해당 라이브러리의 전체 도서를 불러오는 진입점으로 사용.
