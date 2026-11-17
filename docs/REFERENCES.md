# 공식 출처와 귀속

## 분석한 공식 소스

- Mihon [v0.21.3](https://github.com/mihonapp/mihon/tree/v0.21.3), [Source API](https://github.com/mihonapp/mihon/tree/v0.21.3/source-api/src/main/kotlin/eu/kanade/tachiyomi/source), [ExtensionLoader](https://github.com/mihonapp/mihon/blob/v0.21.3/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt), [ExtensionManager](https://github.com/mihonapp/mihon/blob/v0.21.3/app/src/main/java/eu/kanade/tachiyomi/extension/ExtensionManager.kt).
- 이미지 경로: [MangaCoverFetcher](https://github.com/mihonapp/mihon/blob/v0.21.3/app/src/main/java/eu/kanade/tachiyomi/data/coil/MangaCoverFetcher.kt), [HttpPageLoader](https://github.com/mihonapp/mihon/blob/v0.21.3/app/src/main/java/eu/kanade/tachiyomi/ui/reader/loader/HttpPageLoader.kt), 호스트 HttpSource의 client/headers.
- tachiyomix [1.6.0](https://github.com/mihonapp/tachiyomix/tree/1.6.0): README/Manifest 요구사항, Source/CatalogueSource/HttpSource/SourceFactory/ConfigurableSource 및 모델 스텁.
- BookOrbit [v3.8.0의 정확한 커밋](https://github.com/bookorbit/bookorbit/tree/450c18e2acf79ddd39b6dee022a523281448e5e6), [API keys](https://github.com/bookorbit/bookorbit/blob/450c18e2acf79ddd39b6dee022a523281448e5e6/docs/API_KEYS.md), [추가 조건](https://github.com/bookorbit/bookorbit/blob/450c18e2acf79ddd39b6dee022a523281448e5e6/ADDITIONAL_TERMS.md), [AGENTS](https://github.com/bookorbit/bookorbit/blob/450c18e2acf79ddd39b6dee022a523281448e5e6/AGENTS.md). 서버 코드를 수정하거나 복사하지 않았다.
- [Gradle Wrapper](https://docs.gradle.org/8.14.4/userguide/gradle_wrapper.html), [AGP 8.13](https://developer.android.com/build/releases/past-releases/agp-8-13-0-release-notes), [Kotlin Gradle 호환성](https://kotlinlang.org/docs/gradle-configure-project.html).
- Android 플랫폼의 [Keystore](https://developer.android.com/privacy-and-security/keystore), [Context.noBackupFilesDir](https://developer.android.com/reference/android/content/Context#getNoBackupFilesDir()), [Preference](https://developer.android.com/reference/androidx/preference/Preference).

과거 이슈의 후보 경로를 그대로 추정하지 않고 실제 컨트롤러와 실서버로 페이지 수·0-based 이미지 경로를 확인했다. 자세한 버전·경로 표는 API_DISCOVERY.md에 있다.

## 라이선스 정책

독립 구현 코드는 루트 LICENSE의 Apache-2.0으로 제공한다. Gradle이 생성한 wrapper도 Apache-2.0 고지를 포함한다. NOTICE에 공식 프로젝트 출처와 호스트 제공 의존성을 기록했다. 공식 BookOrbit 로고를 사용하지 않았으며 아이콘은 새로 만든 벡터다. 프로젝트는 비공식 클라이언트이며 승인/후원을 주장하지 않는다.

BookOrbit 서버는 AGPL-3.0-only 및 저장소의 별도 추가 조건을 고지한다. 그 AGENTS.md·ADDITIONAL_TERMS.md를 읽고 참조했으나 프로젝트 지침이나 서버 구현을 가져오지 않았다. 서버 코드가 포함된 파생물을 배포하는 방식은 사용하지 않았다. 서버 구현을 앞으로 복사/수정/배포할 경우 그 시점에 원문 라이선스와 추가 조건, Corresponding Source 및 UI 귀속 요구를 다시 검토해야 한다.

Mihon은 Apache-2.0, 실제 사용한 tachiyomix 1.6.0 태그도 Apache-2.0이다. 최신 main의 라이선스·언어 API 변경과 태그 버전을 혼동하지 않는다. 호스트 라이브러리는 compileOnly라서 확장 APK에 재배포하지 않는다. 실험에 사용한 BookOrbit 컨테이너와 RAR 생성 도구도 APK/저장소에 포함하지 않는다.

이 프로젝트에는 기존 Keiyoushi BookOrbit 확장을 복사하거나 포크한 파일 또는 그 모듈 의존성이 없다.
