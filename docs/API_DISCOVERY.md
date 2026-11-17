# 공식 API 조사 및 지원 범위

조사일: 2026-11-18 (Asia/Seoul). 서버 코드를 가져오지 않고 컨트롤러·DTO·쿼리의 계약만 독립 구현했다. 기존 Keiyoushi 확장은 열람·복제·의존하지 않았다.

## 고정한 공식 소스

- **실제 연동 시험 서버: BookOrbit v3.8.0**, 커밋 [`450c18e2acf79ddd39b6dee022a523281448e5e6`](https://github.com/bookorbit/bookorbit/tree/450c18e2acf79ddd39b6dee022a523281448e5e6). 공식 이미지 `ghcr.io/bookorbit/bookorbit:3.8.0`, 이미지 digest `sha256:d318f7732f6f6330312e2c5f854f7899f858b54cc4b18a13cde1f43b5bea0515`. 이미지 revision 라벨과 태그 커밋을 대조했다.
- 추가 조사: BookOrbit main `c77966cc529423e993fae8a298aa77048b43bba2`. 아래 핵심 DTO/만화/쿼리 계약은 v3.8.0과 대조했다. book.controller.ts의 변경은 사용하지 않는 다른 동작이며 표지·상세 GET은 실서버로 확인했다.
- **컴파일 API: tachiyomix 1.6.0**, 커밋 [`4e1ff5957ae025dc8c9e900cb5d23f9ace5bbadb`](https://github.com/mihonapp/tachiyomix/tree/4e1ff5957ae025dc8c9e900cb5d23f9ace5bbadb). README의 `:1.6` 좌표는 실제 JitPack에서 404였고, 공식 태그 `:1.6.0`은 POM/AAR 다운로드 및 빌드 성공. Manifest의 API 레벨 값은 여전히 `1.6`이다.
- 호스트: 공식 Mihon v0.21.3 APK와 main `4823ba69ca65fa707fb89406f3a4c43c9e0053dd`. [v0.21.3 로더](https://github.com/mihonapp/mihon/blob/v0.21.3/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt)는 1.4/1.6을 허용한다. 최신 tachiyomix main은 1.7 언어 API 변경을 포함하므로 컴파일 계약으로 사용하지 않았다.

아래 서버 경로는 기본 URL 뒤 `api/v1/`에 붙인다. `/orbit/` 같은 역방향 프록시 하위 경로도 유지한다.

| 영역 | 실제 메서드·경로 | 요청 / 응답 계약 | 공식 근거 (v3.8.0 저장소 상대 경로) |
|---|---|---|---|
| 인증 확인 | `GET auth/me` | `Authorization: Bearer bok_…`, 응답 `{id, authenticationMethod:"api_key", …}` | `server/src/modules/auth/auth.controller.ts`, `auth.service.ts`, `docs/API_KEYS.md` |
| 접근 가능 라이브러리 | `GET libraries` | 배열 `[{id,name,…}]`; 키의 라이브러리 범위 반영 | `server/src/modules/library/library.controller.ts`, `packages/types/src/library.ts` |
| 목록·검색 | `POST libraries/{libraryId}/books` | `BookQuery`, 아래 예시. 읽기 전용 키에 허용된 검색 POST | library controller의 `@KeyReadSafe()`, `server/src/modules/book/pipes/book-query.pipe.ts` |
| 시리즈 묶음 | 같은 POST의 `collapseSeries:true` | 필터된 책들을 `(library_id, series_id)`로 묶음. 비시리즈는 book ID로 독립. `collapseLinked:false`로 연관 도서 추가 묶음 방지 | `server/src/modules/book/book.repository.ts`의 `findCardsCollapsed`, `book.service.ts` |
| 형식·파일 상태 필터 | `filter`의 `format/includesAny`, `fileAvailability/isPresent` | 모든 파일의 형식을 EXISTS로 검사. 기본 시리즈 목록 GET에는 형식 필터 없음 | `book-query-builder.service.ts`의 `formatRuleToSql`, `statusRuleToSql` |
| 목록 응답 | 위 POST | `{items:BookCard[],total,page,size}`. total은 묶인 카드 수. `BookCard.files:[{id,format,role,…}]`, `libraryId`, `seriesId`, `seriesName`, `seriesIndex`, `hasCover` | `packages/types/src/book.ts`, `utils/assemble-book-cards.ts` |
| 시리즈 상세·챕터 도서 | `GET series/{id}/books?libraryId=N&page=0&size=100&sort=seriesIndex&order=asc` | `{items:BookCard[],total,page,size,seriesInfo:{id,name,authors,…}}`. 도서 전 페이지를 읽고 만화 파일만 선택 | `server/src/modules/series/series.controller.ts`, `dto/list-series-books.dto.ts`, `series.service.ts` |
| 단독 도서 상세 | `GET books/{id}` | `BookDetail`: libraryId·files·description. authors는 문자열 배열이 아닌 `{id,name,…}` 배열. 라이브러리 ID를 클라이언트에서도 확인 | `book.controller.ts`, `packages/types/src/book.ts` |
| 표지 | `GET books/{id}/cover` | 인증된 이미지 응답, 없는 표지는 404. 변경 버전 쿼리는 사용하지 않음 | book controller의 `getCover` |
| 페이지 수 | `GET cbz/files/{fileId}/pages` | **`{pageCount:number}`**, 페이지 URL 배열이 아님 | `server/src/modules/reader/cbz/cbz.controller.ts` |
| 이미지 | `GET cbz/files/{fileId}/pages/{pageIndex}` | **0부터 시작**. CBZ/CBR/CB7 모두 동일 경로. 파일 권한 확인·실제 archive magic 확인 후 이미지 스트림 | `cbz.controller.ts`, `cbz.service.ts` |
| 오류·키 정책 | 401/403/404/422/429/5xx | 인증·권한·삭제/미지원·잘못된 파일·요청 제한·서버 오류. 응답 본문은 사용자 오류나 로그로 전달하지 않음 | API keys 문서, 전역 exception filter, comic service |

## 실제 사용하는 요청 예시

인증 정보와 서버 주소가 없는 예시다. Mihon 1페이지는 서버 0페이지에서 시작한다.

```json
{
  "collapseSeries": true,
  "collapseLinked": false,
  "q": "",
  "pagination": {"page": 0, "size": 40},
  "sort": [{"field": "title", "dir": "asc"}],
  "filter": {
    "type": "group", "join": "AND",
    "rules": [
      {"type": "rule", "field": "format", "operator": "includesAny", "value": ["cbz", "cbr", "cb7"]},
      {"type": "rule", "field": "fileAvailability", "operator": "isPresent"}
    ]
  }
}
```

응답 필드 예시(합성 예시이며 실제 응답을 가장하지 않음):

```json
{"items":[{"id":11,"libraryId":7,"status":"present","title":"Volume 1","seriesId":4,"seriesName":"Series","seriesIndex":"1","authors":["Author"],"hasCover":true,"files":[{"id":22,"format":"cbz","role":"primary"},{"id":23,"format":"epub","role":"content"}]}],"total":1,"page":0,"size":40}
```

`primary`는 카드 조립 과정에서 부여될 수 있다. 파일 역할 `primary` 또는 `content`만 읽고, 첨부 파일 역할 및 알 수 없는 역할은 제외한다. 형식은 클라이언트에서 대소문자·앞쪽 점·공백을 정규화한다. 서버의 형식 필터는 DB에 정규화된 소문자 형식을 사용한다. 비정상 DB에 임의의 대문자/확장자를 직접 저장한 경우까지 서버 필터가 보장하지는 않는다.

## 인증·이미지·재시도

- 비밀번호 로그인/JWT/쿠키 갱신 대신 **서버에서 발급한 읽기 전용 API 키**를 사용한다. 키의 수명은 서버가 관리한다. 401은 만료·폐기·계정/전역 설정 문제이므로 같은 키로 반복 재시도하지 않고 교체를 안내한다. 이 모드에는 refresh token이 없다.
- `auth/me`와 `libraries`로 연결을 확인한 다음 선택한 라이브러리를 적용한다. 설정 초안과 실제 읽기에 사용하는 연결을 분리한다.
- HttpSource의 `client`를 표지/페이지/API에 공통 사용한다. 정적인 `headers`에 키를 넣지 않고 인터셉터에서 현재 연결 범위를 검사한 뒤 넣는다. 리다이렉트·쿠키·다른 origin/기본 경로·이전 연결 이미지를 거부한다.
- 이미지 URL의 `#bo=불변연결ID`는 로컬 범위 확인용이다. 서버에 보내기 전에 제거하며 비밀이나 쿼리스트링이 아니다. 재매핑 후 오래된 표지 URL로 새 자격 증명을 보내지 않는다.
- 429/502/503/504만 최대 2회 재시도. Retry-After 초/HTTP 날짜를 최대 5초까지 존중한다. 더 긴 서버 대기는 재요청하지 않고 오류를 안내한다. 연결/읽기/전체 호출 제한은 10/30/60초이다. JSON 본문을 읽는 중에도 취소하면 Call을 취소한다.

## 실서버 검증 및 API 안정성

`LiveServerTest`가 별도 컨테이너의 공식 **v3.8.0**에서 실행되어 성공했다. 직접 생성한 EPUB·ZIP·RAR·7z 파일, 라이브러리 3개, 표시 작품 7개, 이미지 21장 및 표지·검색을 사용했다. 기존 서버나 사용자 데이터는 사용하지 않았다. 합성 응답 기반 ProtocolTest와 구별한다. 상세 결과는 `VERIFICATION.md`에 기록한다.

BookOrbit는 [공식 README의 API Compatibility](https://github.com/bookorbit/bookorbit/blob/450c18e2acf79ddd39b6dee022a523281448e5e6/README.md#api-compatibility)에서 외부 클라이언트의 API 호환성을 보장하지 않는다. 검증 범위는 위 커밋/버전이다. 이전 서버·향후 서버·OIDC/JWT 모드·HTTP 서버는 검증된 지원 범위에 포함하지 않는다. 계약이 맞지 않으면 빈 목록으로 숨기지 않고 명시적으로 실패한다.
