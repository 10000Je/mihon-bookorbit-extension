# 빌드·검증 기록

## 현재 변경: BookOrbit 1.6.3 (versionCode 3)

2026-11-18. 기본 라이브러리 선택/적용 설정과 선택 로직을 제거했다. **연결 확인 / 라이브러리 재검색** 성공 시 서버·계정·접근 가능한 전체 라이브러리를 자동 적용한다. 인기는 모든 라이브러리의 만화를 통합하며 필터는 전체 또는 특정 라이브러리로 제한한다. 앱/확장 표시 이름은 **BookOrbit**, 소스 이름은 **BookOrbit I / II / III**다. 기존 패키지·서명·소스 ID·작품 binding과 암호화 저장 경로는 보존했다.

구현 파일: `settings/SettingsScreen.kt`, `settings/Connection.kt`, `BookOrbitSource.kt`, `LibraryFilter.kt`, `repository/LibraryCataloguePager.kt`, `model/Identity.kt`, `AndroidManifest.xml`. 통합 순서는 라이브러리 ID순 + 각 라이브러리의 제목순이며 페이지당 최대 40작품이다. 다른 라이브러리의 동일 seriesId도 각각 유지한다. 경계 페이지·빈 라이브러리·권한 오류·캐시 제한 정책은 [ARCHITECTURE.md](ARCHITECTURE.md)에 기록했다.

실행한 기능 테스트는 로컬 JVM 단위 테스트뿐이며 **48개 통과, 실패/오류/skip 0**이다. ProtocolTest 16, ComicFormatTest 3, FilteredPagerTest 6, LibraryCataloguePagerTest 8, IdentityAndSettingsTest 5, LibrarySelectionTest 10개다. MockWebServer는 합성 응답만 사용했다. `LiveServerTest`는 기본 작업에서 제외되어 실행되지 않았다. 에뮬레이터·기기·실서버 호출, 설치 및 공개 배포는 하지 않았다.

앞선 APK 빌드 요청에 따라 수정된 소스도 Debug/Release로 다시 빌드했다. 작업 시작 전부터 있던 wrapper의 Gradle 9.7.1 URL/8.14.4 체크섬 변경은 그대로 두고 캐시된 `/home/ubuntu/.gradle/wrapper/dists/gradle-8.14.4-bin/92wwslzcyst3phie3o264zltu/gradle-8.14.4/bin/gradle`을 사용했다. `--offline :app:testDebugUnitTest`와 기존 비공개 signing properties를 지정한 `--offline :app:assembleDebug :app:assembleRelease` 모두 BUILD SUCCESSFUL이다. Release 필수 Lint도 통과했다.

| 산출물 | 상대 경로 | SHA-256 |
|---|---|---|
| 설치용 Release (97,702 bytes) | `artifacts/bookorbit-1.6.3-release.apk` | `fb1d4dd7fbe812461db6205747f2f0ac076c933e8f417a5e39a8c7f7de5f9c83` |
| 개발용 Debug (141,589 bytes) | `artifacts/bookorbit-1.6.3-debug.apk` | `c1a2f14ce740bd022a98470350cebf59e3a20ee2d7f5b2fc1d61c95ba6b43a90` |

Release 절대 경로: `/home/ubuntu/workspace/mihon-bookorbit-extension/artifacts/bookorbit-1.6.3-release.apk`. 체크섬 파일: `artifacts/SHA256SUMS-1.6.3`. 검사 기록: `artifacts/apk-verification-1.6.3.txt`, `artifacts/manifest-1.6.3.txt`. SDK 36.0.0의 apksigner와 aapt2로 APK v2 서명, 패키지 `app.bookorbit.mihonext`, 표시명 BookOrbit, 버전 1.6.3 (3), extensionLib float 1.6을 확인했다. Release 인증서는 기존 핀 `bf3c650b87f278df0f01ecb458345f7a1c231a5f7634a29a2274da63cf0b1b10`과 같다.

Release APK로 기존 Release를 업데이트하고 Mihon에서 요청하면 신뢰 승인한다. 연결 확인을 실행한 뒤 소스 화면에 다시 진입하면 전체 라이브러리와 새 필터가 적용된다. 실제 UI·읽기 동작과 BookOrbit 3.8.0 이외 버전은 이번에 검증하지 않았다. 공개 스토어 피드는 기존 배포본을 유지하며 이번 명칭 변경을 아직 포함하지 않는다. `git diff --check`는 통과했다.

## 과거 1.6.2 (versionCode 2)

검증일: 2026-11-18 (Asia/Seoul). 고정 3소스, 라이브러리 필터, 기본 라이브러리와 저장된 작품의 연결 분리, 라이브러리별 이미지 인증을 구현했다. 기존 슬롯 1~3의 ID와 binding은 유지하며 4~6번 슬롯은 생성하지 않는다. 변경 구현은 `BookOrbitSourceFactory.kt`, `BookOrbitSource.kt`, `LibraryFilter.kt`, `settings/Connection.kt`, `SettingsStore.kt`, `SettingsScreen.kt`, `api/BookOrbitHttp.kt`, `model/Identity.kt`에 있다.

기능 테스트는 로컬 JVM 단위 테스트만 실행했고 `LiveServerTest`는 Gradle 기본 테스트 작업에서 제외했다. 이후 사용자의 명시적 APK 빌드 요청에 따라 Debug/Release APK 빌드 및 정적 Manifest·서명 검사를 추가로 실행했다. 에뮬레이터·물리 기기·실서버를 호출하지 않았고 APK 설치나 공개 배포는 하지 않았다. 아래 과거 1.6.1 배포 APK와 체크섬은 **이번 변경의 산출물이 아니다**.

로컬 wrapper properties에 작업 시작 전부터 Gradle 9.7.1 URL과 기존 8.14.4 SHA-256이 함께 설정되어 있었다. 이 사용자 변경은 수정하지 않고, 캐시된 기존 고정 버전 Gradle 8.14.4로 `--offline :app:testDebugUnitTest`를 실행했다. 정확한 실행 파일은 `/home/ubuntu/.gradle/wrapper/dists/gradle-8.14.4-bin/92wwslzcyst3phie3o264zltu/gradle-8.14.4/bin/gradle`이다. 샌드박스의 네이티브 초기화 제한으로 승인 후 실행했다.

새 회귀 검증은 `LibrarySelectionTest`와 `ProtocolTest`에 있다. 필터/기본값 전환, 기존 작품·챕터·이미지, 이름·순서·키 회전, 다른 서버·계정 차단, 라이브러리 제거/복구, 구형 설정 호환, 12개를 넘는 현재 라이브러리의 식별자 유지, 두 라이브러리의 동일 검색어 요청 경로를 확인한다. ProtocolTest는 로컬 MockWebServer의 **합성 응답**만 사용한다.

결과: **BUILD SUCCESSFUL, JUnit 38개 통과, 실패 0, 오류 0, skip 0**. ComicFormatTest 3개, IdentityAndSettingsTest 5개, LibrarySelectionTest 8개, FilteredPagerTest 6개, ProtocolTest 16개다. `LiveServerTest`는 제외되어 실행 보고서 자체가 없다. 보고서는 `app/build/reports/tests/testDebugUnitTest/index.html`과 `app/build/test-results/testDebugUnitTest/`에 있다. `git diff --check`도 통과했다. SDK XML 형식 버전 안내와 기존 테스트의 불필요한 `!!` 경고가 있었으며 단위 테스트 실행에는 영향을 주지 않았다.

스토어 생성 스크립트는 현재의 `Identity.SOURCE_COUNT=3` 및 과거 릴리스의 리터럴 6슬롯 소스를 모두 읽도록 수정했다. 피드 생성은 실행하지 않았으며 기존 `repo/` 피드는 그대로 유지했다.

### 후속 요청으로 실행한 APK 빌드

같은 캐시된 Gradle 8.14.4에 기존 비공개 서명 properties 경로를 `BOOKORBIT_SIGNING_PROPERTIES`로 지정하여 `--offline :app:assembleDebug :app:assembleRelease`를 실행했다. **BUILD SUCCESSFUL**, Release 필수 Lint 검사도 통과했다. 단위 테스트는 앞서 통과한 동일 소스로 재실행하지 않았다.

| 산출물 | 상대 경로 | SHA-256 |
|---|---|---|
| 설치용 Release (92,442 bytes) | `artifacts/bookorbit-independent-1.6.2-release.apk` | `80071189f2d671970517ef7aac8de000dde8df1f3792ba25ebf3bfda382d1641` |
| 개발용 Debug (132,145 bytes) | `artifacts/bookorbit-independent-1.6.2-debug.apk` | `871816039517e5802e76667e850dca5247f3e7fbdddae7c7111ec68e05d2658d` |

Release 절대 경로: `/home/ubuntu/workspace/mihon-bookorbit-extension/artifacts/bookorbit-independent-1.6.2-release.apk`. 체크섬 파일: `artifacts/SHA256SUMS-1.6.2`. 검사 기록: `artifacts/apk-verification-1.6.2.txt`.

`scripts/verify-apk.sh`와 SDK Build Tools 36.0.0의 `aapt2 dump xmltree`로 패키지 `app.bookorbit.mihonext`, versionName 1.6.2 / versionCode 2, minSdk 26 / targetSdk 35, backup 비활성화, Mihon 확장 기능·팩토리 및 extensionLib의 실제 float 1.6을 확인했다. Release와 Debug 모두 APK v2 서명 검증에 성공했다. Release 인증서 SHA-256은 기존 핀과 같은 `bf3c650b87f278df0f01ecb458345f7a1c231a5f7634a29a2274da63cf0b1b10`이다. 개인 키는 APK나 Git에 포함하지 않았다.

설치 시 Release APK를 Android에서 열어 기존 Release를 업데이트하고, Mihon에서 요청하면 신뢰 승인한다. 변경된 필터 목록은 연결 확인 → 기본 라이브러리 적용 → 소스 재진입으로 갱신한다. 실제 설치·신뢰·탐색·읽기 동작은 이번에 검증하지 않았다.

## 아래는 과거 1.6.1 빌드·서버·기기 검증 기록

이하 내용은 이번 변경 이전의 기록이다. 과거 검증일: 2026-11-18 (Asia/Seoul). 모의 응답, 실제 서버, 실제 Mihon 앱의 검증을 구별한다. 물리 Android 단말을 사용한 시험은 하지 않았다.

## 산출물과 빌드

- 패키지 `app.bookorbit.mihonext`, versionName `1.6.1`, versionCode `1`, minSdk 26 / targetSdk 35.
- 설치용 APK: `artifacts/bookorbit-independent-1.6.1-release.apk`.
- 개발용 APK: `artifacts/bookorbit-independent-1.6.1-debug.apk`.
- 공개 서명 인증서 SHA-256: `bf3c650b87f278df0f01ecb458345f7a1c231a5f7634a29a2274da63cf0b1b10`.
- APK 파일 SHA-256: `artifacts/SHA256SUMS`에 기록. 전체 Manifest/서명/정의된 패키지 검사: `artifacts/apk-verification.txt`.
- release는 로컬 RSA 3072-bit 키로 서명했고 APK v2 서명 검증에 성공했다. 개인 키와 비밀번호는 Git 밖에 보관했다. release/debug를 혼용하면 서명이 달라 덮어쓰기가 안 되므로 release를 유지한다.

실제 실행한 주요 명령:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
BOOKORBIT_SIGNING_PROPERTIES=/absolute/private/release-signing.properties \
BOOKORBIT_LIVE_CONFIG=/absolute/private/live-config.json \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
```

결과: **BUILD SUCCESSFUL. JUnit 30개, 실패 0, skip 0**(실서버 설정을 제공한 실행). 일반 환경에서 선택적 LiveServerTest는 설정이 없으면 1개 skip한다. Android Lint는 오류 0, 경고 12개이다. 경고는 고정한 호스트 의존성/도구 버전의 새 버전 권고와 targetSdk/플랫폼 관련 안내이며 실제 HTML/XML 보고서를 보관했다. 최신 버전으로 임의 교체하지 않았다.

APK에서 Manifest의 `tachiyomi.extension` 기능, `tachiyomix.extensionLib=1.6`, SourceFactory 클래스, 독립 패키지, backup 설정을 검사했다. dex의 정의된 클래스는 독립 구현 및 컴파일러/플랫폼 보조 클래스이며 호스트 API 스텁과 Kotlin/OkHttp를 패키징하지 않았다. `git diff --check`도 실행했다.

## 자동 테스트 범위

| 테스트 | 실행 수 | 검증 |
|---|---:|---|
| ComicFormatTest | 3 | CBZ/CBR/CB7 정규화, EPUB/PDF/알 수 없는 형식, 혼합 형식 |
| IdentityAndSettingsTest | 5 | 이름·정렬·키 회전, 서버/계정/라이브러리 전환 보호, 이전 연결 복원, 잘못된 URL/레지스트리 |
| FilteredPagerTest | 6 | EPUB뿐인 첫 페이지, 빈 구간 look-ahead, 중복, 실패 후 재시도 커서, TTL/쿼리 격리, 1,000작품/25요청 |
| ProtocolTest | 15 | 실제 TLS 통신을 하는 모의 서버의 요청/DTO/인증, 이미지 헤더, 외부·과거 연결 거부, 401/403/404/429/5xx, 타임아웃, 503 복구, 취소(헤더·본문), 100권 이상 시리즈, 첨부 대표 카드, 빈·삭제 파일 |
| LiveServerTest | 1 | 공식 실제 서버의 라이브러리 격리, 형식 필터, 상세/챕터, 페이지 및 표지 바이트 |

모의 서버 응답은 공식 DTO를 따르는 **합성 fixture**이며 실서버 실행을 대신했다고 표시하지 않았다. 따로 수행한 LiveServerTest는 아래의 실제 컨테이너를 호출했다.

## 공식 BookOrbit 실제 서버

공식 v3.8.0 이미지/커밋/digest는 API_DISCOVERY.md에 고정했다. 별도의 테스트 PostgreSQL과 서버를 로컬 컨테이너로 실행했으며 사용자 서버나 실제 계정 데이터를 사용하지 않았다. 서버 코드는 패치하지 않았다. 테스트용 TLS 프록시를 앞에 두고 신뢰한 테스트 CA로 HTTPS를 사용했다. 프로덕션 확장에는 TLS 우회 코드를 넣지 않았다.

직접 생성한 원본 PNG 3장씩을 포함하는 실제 ZIP(CBZ), RAR(CBR), 7z(CB7), 원본 EPUB을 준비해 서버의 정상 스캐너로 등록했다.

- 라이브러리 A: ZIP/RAR/7z 시리즈 각 1개, EPUB+CBZ 혼합 작품 1개, 비시리즈 CBZ 1개, EPUB 전용 작품 1개.
- 라이브러리 B/C: 각각 독립 CBZ 시리즈 1개.
- 만화 목록: A 5개, B/C 각각 1개, 합계 7개. EPUB 전용 작품은 목록·검색 모두 제외.
- 혼합 작품 챕터에는 CBZ만 노출. EPUB 챕터를 만들지 않음.
- 읽기 전용 API 키, 추가 권한 없음으로 계정/라이브러리/목록/상세/챕터/표지/페이지 접근 성공.
- 3개 아카이브 형식, 총 21장 페이지의 `image/png` 및 실제 PNG signature 확인. 실제 표지도 인증하여 수신.

테스트용 프록시의 HTTP 연결 종료 처리가 초기 실서버 시험에서 EOF를 만들었으며, 프록시를 수정한 뒤 동일 프로덕션 클라이언트로 통과했다. 이는 서버 엔드포인트를 추정하거나 성공 응답을 대체한 것이 아니다.

## 공식 Mihon 실제 앱 / Android 에뮬레이터

공식 릴리스 **Mihon v0.21.3 x86_64 APK**, Android 11/API 30 AOSP 에뮬레이터에 서명된 릴리스 확장 APK를 설치했다. Android/Mihon 앱 자체를 수정하지 않았다. 초기 소프트웨어 에뮬레이션이 느려 별도 KVM 에뮬레이터 컨테이너를 사용했다. 자체 테스트 CA는 이 시험용 Android의 사용자 인증서 저장소에만 설치했다.

확인 완료:

- Android 설치 성공, 독립 패키지 인식, Mihon의 Untrusted 표시 → 사용자 신뢰 승인 → Installed/Multi 상태.
- `탐색 → 소스`의 슬롯 1~6 독립 표시.
- 미설정 소스 진입 시 설정 안내, `인기` 탭만 표시. 최신 탭 없음.
- 설정 화면, 서버/비밀번호 형식 API 키 입력, Keystore 저장, 연결 확인, 실제 3개 라이브러리 선택창.
- 슬롯 1에 A 연결: 실제 표지와 A의 작품 목록 표시. 혼합 시리즈 상세는 만화 챕터 1개.
- **CBZ·CBR·CB7 각각 Mihon 독자 화면에서 실제 페이지와 1/3 표시 확인.**
- 슬롯 2/3에 B/C를 각각 UI로 연결하여 해당 라이브러리 만화만 표시되고 A 작품은 나타나지 않음을 확인.
- 호스트 no_backup의 암호화 파일 존재 및 실제 API 키/서버 주소의 평문 바이트가 없음을 검사.
- 테스트 라이브러리 A의 이름을 서버에서 바꾸고 소스 설정에서 재검색했다. Mihon에 바뀐 라이브러리명이 표시됐으며, 변경 전후 DB를 비교해 기존 3작품의 manga/chapter ID, Source.id, 작품/챕터 URL, 읽기 상태·페이지 필드가 정확히 유지됨을 확인했다. 이때 기존 페이지 값은 0(첫 페이지)이었으며 다른 진행률 값을 전수 시험했다는 의미는 아니다.

발견·수정한 호스트 호환성 문제: Mihon이 목록 요청 전 `getHomeUrl/baseUrl`을 읽는다. 미설정 baseUrl이 예외를 던져 소스 화면이 크래시하던 것을 수정했다. 재빌드/재설치 후 미설정 화면부터 정상 진입하고 설정/독자 화면까지 재검증했다. 최신 API의 suspend 구현만 컴파일하는 것으로 이 문제를 검출했다고 주장하지 않는다.

증거 이미지(모두 테스트용 원본 이미지·테스트 라이브러리): `artifacts/mihon-six-sources.png`, `mihon-cbz-reader.png`, `mihon-cbr-reader.png`, `mihon-cb7-reader.png`, `mihon-library-b.png`, `mihon-library-c.png`.

## 동적 팩토리 실험

공식 로더를 먼저 분석한 뒤 독립된 임시 캐시 팩토리 APK도 실제 Mihon에서 시험했다. Guide 1소스 → 캐시를 실제 라이브러리 3개로 변경 → 탭 왕복 시 1개 유지 → Mihon 프로세스 재시작 후 3개 표시를 확인했다. 동적 생성은 재시작 후 가능하며, 자동 재등록이 불가능하다는 것이 확인한 제약이다. 이번 배포는 **고정 6슬롯**으로 연결 변경을 팩토리 재등록과 분리했다. 실험 APK는 제거했고 프로덕션 APK에 프로브 클래스를 포함하지 않는다. 코드/절차는 experiments/README.md에 있다.

## 남은 제한

- **물리 단말 시험 미실행.** 에뮬레이터에서의 실제 Mihon 앱 시험과 혼동하지 않는다. 다른 Android 버전/제조사/향후 Mihon은 추가 확인이 필요하다.
- 검증 서버는 v3.8.0이며 그 외 API 버전 호환성을 보장하지 않는다. HTTP, 비밀번호/JWT/OIDC 인증, 서버 진행률 쓰기 동기화는 미지원이다.
- 동시에 노출할 수 있는 라이브러리는 6개이다. 동적 모드는 현재 설치용 APK의 기능이 아니다.
- 이름 변경은 ID를 유지하지만 호스트 화면별 이름 캐시에는 프로세스 재시작이 필요할 수 있다. 자동 소스 UI 새로고침 API를 만들지 않았다.
- 서버 URL/계정 ID를 그대로 둔 채 서버 DB를 교체해 도서 ID를 재사용하는 상황은 자동 식별할 서버 UUID 계약이 없어 별도 슬롯/초기화가 필요하다.
- 최근 12개 연결 밖의 복원, 설정 초기화 뒤 기존 binding 복원, 라이브러리를 옮기는 자동 기록 이관은 지원하지 않는다. 재매핑은 이전 작품의 잘못된 연결을 거부한다.
- 라이브러리 조회 중 도서가 삽입/삭제되면 서버 offset 페이지에 snapshot 보장이 없으므로 새로고침이 필요할 수 있다. 1,000작품은 자동 부하 시나리오로 확인했으며 실제 서버에 1,000작품을 스캔한 시험은 아니다.
- 손상된 실제 아카이브/디스크 장애/권한 회수 전 조합을 물리 단말에서 전수 시험하지 않았다. 클라이언트는 오류를 빈 성공 결과로 삼키지 않는다.

## 구현 파일과 Git 관리

- 루트 `AGENTS.md`를 구현 전에 새로 작성하고 준수했다. `CODEX_PROMPT.md`를 핵심 명세로 사용했다.
- `BookOrbitSourceFactory.kt`, `BookOrbitSource.kt`: 6소스 생성, host suspend API/설정/인기/최신/표지/독자 어댑터.
- `api/BookOrbitApi.kt`, `api/BookOrbitHttp.kt`, `api/ApiFailure.kt`: 공식 경로·DTO, 인증·취소·재시도·오류.
- `model/ComicFormat.kt`, `model/Models.kt`, `model/Identity.kt`: 형식·계약 모델·불변 식별자.
- `repository/BookOrbitRepository.kt`, `repository/FilteredPager.kt`: 라이브러리/시리즈 범위, 챕터·페이지, 유계 캐시와 보충 읽기.
- `settings/Connection.kt`, `settings/SettingsStore.kt`, `settings/SettingsScreen.kt`: 초안/활성 연결, 암호화, 연결 확인·선택·재매핑 UI.
- `AndroidManifest.xml`, `res/`, Gradle 스크립트·wrapper: 독립 APK, 백업/아이콘/메타데이터, 고정 도구체인·선택적 비공개 release 서명.
- `app/src/test/`, `experiments/`, `scripts/`: 30개 시험, 캐시 팩토리 프로브, APK 검증 도구.
- README와 docs를 실제 구현·지원 버전·시험 결과로 갱신했다. LICENSE/NOTICE에 독립 코드와 외부 출처를 명시했다.

Git을 새로 초기화하여 main 브랜치에서 초기 규칙/스타터, 구현, 호스트 호환성 수정·검증 문서를 단계별로 커밋했다. APK·SDK·캐시·실제 자격 증명·개인 서명 키는 추적하지 않는다. 초기 구현 검증 당시 원격 저장소 추가나 외부 푸시는 하지 않았다. 후속 GitHub 배포 내용은 [배포 기록](GITHUB_PUBLISHING.md)에 별도로 기록한다.
