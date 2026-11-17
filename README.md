# BookOrbit Independent for Mihon

공식 Mihon/tachiyomix 및 BookOrbit 서버 API를 분석해 독립적으로 구현한 **비공식 Android 확장 APK**입니다. Mihon 앱이나 BookOrbit 서버를 수정하지 않습니다. 기존 Keiyoushi BookOrbit 확장은 열람·복제·의존하지 않았습니다.

**상태:** 단위 테스트, 공식 BookOrbit **3.8.0** 실서버 연동, 디버그·서명된 릴리스 APK 빌드 완료. Mihon/기기 검증의 정확한 범위와 제한은 [검증 기록](docs/VERIFICATION.md)을 확인하세요.

## 설치와 연결

1. 생성된 **`artifacts/bookorbit-independent-1.6.1-release.apk`**를 Android 8.0 이상에 설치합니다. 패키지는 `app.bookorbit.mihonext`입니다. 확장 자체는 실행 아이콘이 없는 APK입니다.
2. **tachiyomix 1.6 API를 지원하는 Mihon**에서 `탐색 → 확장`으로 이동해 `BookOrbit Independent`를 신뢰 승인합니다. 서명 지문은 검증 기록에 있습니다. 구형 1.4 전용 Mihon은 지원하지 않습니다.
3. `탐색 → 소스`에 표시되는 `BookOrbit · 슬롯 1`의 설정을 엽니다.
4. **서버 URL → 읽기 전용 API 키 → 연결 확인 / 라이브러리 재검색 → 라이브러리 선택 / 적용** 순서로 설정합니다. 서버 주소는 HTTPS 기본 주소이며 `/api/v1`은 붙이지 않습니다. 예: `https://books.example.com/` 또는 `https://books.example.com/orbit/`.
5. 다른 라이브러리는 슬롯 2~6에 각각 연결합니다. 각 슬롯은 별도의 Mihon 소스입니다. 소스가 보이지 않으면 언어 필터의 전체 언어 항목을 확인하세요.
6. `인기`에서 선택한 라이브러리의 전체 만화 목록을 탐색합니다. `최신`은 지원하지 않습니다. 검색에도 동일한 만화/라이브러리 범위를 적용합니다.

키는 BookOrbit `Settings → You → API keys`에서 **Read**, 필요한 라이브러리 범위만 선택하여 발급하세요. 실제 시험에서는 추가 권한 없이 목록·만화 페이지·표지를 읽었습니다. 인증은 API 키 방식이며 비밀번호/JWT/OIDC 로그인은 구현하지 않았습니다. 자체 CA를 사용하면 Mihon/Android가 신뢰하는 인증서를 설치해야 합니다. 인증서 검증을 우회하지 않습니다.

## 동작과 기록 보존

- 동적 팩토리와 호스트 재로딩 제약을 조사한 뒤 **6개 고정 슬롯**을 채택했습니다. 자세한 [근거와 설계](docs/ARCHITECTURE.md)가 있습니다.
- EPUB/PDF/오디오/알 수 없는 형식 전용 작품은 목록과 검색에서 제외합니다. 혼합 작품은 CBZ·CBR·CB7의 content/primary 파일만 챕터로 제공합니다. 비시리즈 만화도 포함합니다.
- 라이브러리 이름이 바뀌어도 슬롯 Source.id와 작품/파일 ID가 유지됩니다. 재검색으로 이름을 갱신한 후 화면에 따라 Mihon 재시작이 필요할 수 있습니다.
- 다른 서버·계정·라이브러리로 재매핑하면 기존 읽기 기록을 다른 도서에 연결하지 않습니다. 새 대상을 위한 빈 슬롯 사용을 권장합니다. 이전 연결을 복원하면 최근 12개 연결까지 원래 작품 식별자를 복원할 수 있습니다.
- HTTP, 동시 7개 이상 라이브러리, 서버 읽기 진행률 동기화는 지원하지 않습니다. Mihon 읽기 기록을 사용합니다. API 변경·서버 DB 교체·대규모 단일 시리즈의 제한도 설계 문서를 확인하세요.

## 빌드와 테스트

JDK 17 또는 21, Android SDK Platform 36 / Build Tools 36.0.0을 설치하고 `ANDROID_HOME` 또는 로컬 `local.properties`의 sdk.dir을 설정합니다. Gradle 8.14.4 / AGP 8.13.2 / Kotlin 2.4.0 / tachiyomix 1.6.0을 고정했습니다.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleRelease :app:lintDebug
```

기본 release는 개인 키가 없으면 unsigned입니다. 설치 가능한 release를 만들려면 저장소 밖의 signing properties 파일을 지정합니다. **이 작업에서 생성한 로컬 키로 서명된 release APK는 이미 artifacts에 있습니다.** 키를 Git에 넣지 않습니다.

```sh
BOOKORBIT_SIGNING_PROPERTIES=/absolute/private/release-signing.properties \
  ./gradlew :app:assembleRelease
ANDROID_HOME=/absolute/android-sdk ./scripts/verify-apk.sh
```

서명 파일 형식과 실제 생성한 키 위치, 선택적 실서버 테스트는 [빌드/설치 절차](docs/BUILD_AND_INSTALL.md)를 참고하세요. 빌드 산출물·서명 키·SDK·인증 정보는 Git에서 제외합니다.

## 구조와 개발 규칙

루트 [AGENTS.md](AGENTS.md)를 새로 작성했으며 [CODEX_PROMPT.md](CODEX_PROMPT.md)를 핵심 명세로 사용합니다.

- `app/src/main/kotlin/app/bookorbit/mihonext/`: SourceFactory, HttpSource 어댑터, API/저장소/보안 설정/순수 모델.
- `app/src/test/`: 형식·기록 식별자·페이지네이션·HTTPS 프로토콜·선택적 실제 서버 테스트.
- [API 계약과 버전](docs/API_DISCOVERY.md), [완료 기준](docs/ACCEPTANCE_TESTS.md), [검증 결과와 제한](docs/VERIFICATION.md).
- [공식 출처와 라이선스](docs/REFERENCES.md), `NOTICE`, `LICENSE`.

BookOrbit는 외부 클라이언트의 공개 API 호환성을 보장하지 않습니다. 검증한 서버 버전 밖에서는 엔드포인트·DTO 차이가 있을 수 있으며 명시적 오류로 안내합니다. 이 저장소에는 BookOrbit 서버 구현이나 기존 확장 소스가 포함되지 않습니다.
