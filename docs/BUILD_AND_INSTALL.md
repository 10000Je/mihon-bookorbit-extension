# 재현 빌드와 설치

## 도구체인

| 항목 | 고정 버전 / 범위 |
|---|---|
| Gradle Wrapper | 8.14.4, distribution SHA-256는 wrapper properties에 고정 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin / serialization compiler | 2.4.0 |
| SDK / Build Tools | Platform 36 / 36.0.0 |
| JDK | 최소 17, 실행 검증은 OpenJDK 21.0.13 |
| Android | minSdk 26, targetSdk 35 |
| 호스트 API | tachiyomix 1.6.0 (Manifest 1.6), AndroidX Preference 1.2.1 |
| 호스트 제공 라이브러리 | OkHttp 5.4.0, coroutines 1.10.2, serialization 1.7.3, Injekt 91edab2317 |

호스트가 제공하는 의존성은 모두 compileOnly입니다. APK에 스텁·Kotlin·OkHttp를 함께 넣어 호스트 클래스 로더와 충돌시키지 않습니다. 최신 버전을 무조건 사용하지 않고 공식 1.6.0 인터페이스와 호스트에 맞는 조합을 고정했습니다. AndroidX 실제 API는 공식 스텁의 간략화된 Preference 생성자 대신 호스트와 같은 1.2.1 계약을 컴파일할 때 필요합니다.

공식 호환성 근거: [AGP 8.13](https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes), [Kotlin Gradle 호환성](https://kotlinlang.org/docs/gradle-configure-project.html), [tachiyomix 1.6.0](https://github.com/mihonapp/tachiyomix/tree/1.6.0). AGP/호스트 의존성은 최신 유행 버전 대신 실제 빌드·호스트 ABI 검증을 우선했습니다.

## 명령

```sh
# Android SDK 경로는 각 환경에 맞게 설정
export ANDROID_HOME=/absolute/path/android-sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleRelease :app:lintDebug
```

SDK 설치 예: Android Studio SDK Manager 또는 공식 command-line tools에서 `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`를 설치합니다. `local.properties`는 로컬에서만 사용합니다. Gradle Wrapper는 distribution checksum을 확인하며 별도 시스템 Gradle 설치가 필요 없습니다.

`app/build/outputs/apk/debug/app-debug.apk`는 개발 키로 서명되어 즉시 설치 가능합니다. release는 아래 키 설정이 있을 때만 서명합니다.

## 릴리스 서명

이번 작업의 개인 서명 자료는 저장소 밖 **`/home/ubuntu/.local/share/bookorbit-mihon-extension/`**에 있습니다. 디렉터리 0700, 키·properties 0600입니다. 같은 키를 보관해야 앞으로 동일 패키지의 APK를 업데이트할 수 있습니다. 키와 비밀번호 파일은 별도 안전한 장소에 백업하세요. APK의 공개 인증서 지문과 private key는 다릅니다.

`release-signing.properties` 형식:

```properties
storeFile=/absolute/private/release.p12
storePassword=YOUR_LOCAL_PASSWORD
keyAlias=bookorbit-independent
keyPassword=YOUR_LOCAL_PASSWORD
```

실행:

```sh
BOOKORBIT_SIGNING_PROPERTIES=/home/ubuntu/.local/share/bookorbit-mihon-extension/release-signing.properties \
  ./gradlew :app:assembleRelease
```

생성 APK는 v2 서명을 검증했습니다. release/debug는 서명이 다르므로 같은 패키지로 상호 덮어쓸 수 없습니다. 테스트 환경의 debug를 제거한 다음 release를 설치했으며, 일반 사용자는 release 한 종류를 유지하면 됩니다.

```sh
adb install -r artifacts/bookorbit-independent-1.6.1-release.apk
ANDROID_HOME=/absolute/path/android-sdk ./scripts/verify-apk.sh \
  artifacts/bookorbit-independent-1.6.1-release.apk
```

확장에는 Activity가 없으므로 Android 앱 서랍에서 실행하지 않습니다. Mihon에서 확장을 신뢰 승인한 후 소스 설정을 엽니다. APK 직접 설치가 가능하며, 스토어에서 설치·업데이트하려면 `https://10000je.github.io/mihon-bookorbit-extension/repo.json`을 Mihon 확장 스토어에 추가합니다. BookOrbit 서버 설정은 변경하지 않습니다. [GitHub 배포 절차](GITHUB_PUBLISHING.md)를 참고하세요.

## 선택적 실서버 테스트

`LiveServerTest`는 기본 실행에서 서버 설정이 없으면 **명시적으로 skip**합니다. 이 작업에서는 별도 로컬 BookOrbit 3.8.0에 실제로 실행하여 통과했습니다. 이 테스트는 읽기 전용이지만 아래의 원본 fixture 구성을 전제로 하므로 운영 서버를 테스트 대상으로 쓰지 마세요.

저장소 밖 0600 JSON 파일에 다음 필드를 준비합니다. caCertificate는 테스트 TLS 서버의 PEM 인증서이고, libraryIds는 격리 서버의 3개 라이브러리입니다. 로그나 Git에 키를 넣지 않습니다.

```json
{"server":"https://localhost:31443/","apiKey":"REDACTED","caCertificate":"PEM","principalId":1,"libraryIds":[1,2,3]}
```

```sh
BOOKORBIT_LIVE_CONFIG=/absolute/private/live-config.json \
  ./gradlew :app:testDebugUnitTest --tests '*LiveServerTest'
```

fixture 구성: 첫 라이브러리에 EPUB 전용 작품, CBZ 시리즈, 실제 RAR 기반 CBR 시리즈, 실제 7z 기반 CB7 시리즈, EPUB+CBZ 혼합 작품, 비시리즈 CBZ 도서. 다른 두 라이브러리에 각각 CBZ 작품 하나. 각 만화는 원본 생성 PNG 3장을 포함합니다. 테스트는 EPUB 검색 제외, 총 7작품, 각 챕터와 페이지 수, 21개 PNG 실제 바이트, 인증된 표지를 확인합니다. 잘못된 TLS 구현으로 시험을 통과시키지 않으며 신뢰 인증서는 테스트 클라이언트에만 추가합니다. 프로덕션 소스는 기본 TLS 검증을 유지합니다.

## 산출물

- 설치용 release: `artifacts/bookorbit-independent-1.6.1-release.apk`
- 개발용 debug: `artifacts/bookorbit-independent-1.6.1-debug.apk`
- 체크섬: `artifacts/SHA256SUMS`
- JUnit: `app/build/reports/tests/testDebugUnitTest/index.html`
- Android Lint: `app/build/reports/lint-results-debug.html`
- 완료/기기 기록: `docs/VERIFICATION.md`

SDK·캐시·테스트 전용 서버·생성 파일·APK는 Git에 넣지 않습니다. 사용자 서버 URL, 쿠키, 키 및 서명 비밀번호도 커밋하지 않습니다.
