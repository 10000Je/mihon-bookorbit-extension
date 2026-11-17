# BookOrbit × Mihon — 독립 확장 스타터

Keiyoushi의 BookOrbit 확장을 포크하거나 가져오지 않고, **Mihon의 공식 확장 인터페이스(tachiyomix)** 및 **BookOrbit 서버의 공식 소스/API**를 참고하여 독립적인 Android 확장을 만드는 프로젝트입니다.

> **상태: 개발 스타터 / 구현 미완성.** 여기 포함된 Android 소스는 구조를 확인하기 위한 슬롯 6개의 뼈대입니다. 실제 서버 통신·인증·도서 로딩·페이지 읽기는 Codex가 구현해야 합니다. 빌드·실기기 동작은 아직 검증되지 않았습니다.

## 목표

- Mihon 앱 수정 없이 **BookOrbit 라이브러리별 독립 소스** 진입 (Mihon `탐색 > 소스`).
- EPUB만 포함하는 시리즈는 **목록과 검색에서 숨김**.
- 혼합 시리즈의 경우 CBZ/CBR/CB7 챕터만 노출.
- 기존 Mihon의 `인기` 진입점을 해당 소스의 전체 목록으로 사용; `최신` 비활성화.
- 소스 이름과 라이브러리 매핑의 안정성(읽기 기록 보존)을 우선 고려.

## 시작하기

1. 이 폴더를 Codex에서 열고 **`CODEX_PROMPT.md` 전체 내용을 실행하도록 요청**하세요.
2. Codex가 프로젝트 **루트에 `AGENTS.md`를 새로 작성**해야 합니다. 스타터 ZIP에는 포함되지 않습니다.
3. Codex가 Mihon/tachiyomix 공식 API 및 BookOrbit 서버의 실제 API 스키마를 조사하고 확장을 완성합니다.
4. Android SDK, JDK, Gradle이 있는 환경에서 빌드하고 실제 Mihon에서 신뢰 승인 및 소스 인식을 테스트합니다.

## 현재 스캐폴드 설명

- `app/src/main/.../BookOrbitSourceFactory.kt`: Mihon `SourceFactory`를 구현하고 6개 고정 슬롯 객체를 반환하는 **임시 기본형**.
- `BookOrbitSource.kt`: 각 슬롯을 다른 `Source.id`로 제공하며, 서버 API 작업은 의도적으로 미구현 예외를 던집니다.
- `model/ComicFormat.kt`: EPUB/CBZ/CBR/CB7 형식 구별을 위한 순수 Kotlin 로직.
- `docs/`: 동적 소스 및 폴백 설계, API 검증 과제, 완료 기준.
- **빌드 스크립트의 Android/Kotlin 버전은 스타터용 예시**입니다. Codex가 실제 SDK/호환성 확인 후 버전과 Gradle Wrapper를 고정해야 합니다.

## 중요한 UI 제약

Mihon 자체의 `인기` UI 표시명이나 버튼 배치는 확장에서 원하는 형태로 바꿀 수 있다고 가정하지 않습니다. `supportsLatest = false`는 `최신` 진입 지원을 비활성화합니다. 소스를 라이브러리 단위로 분리하여 `탐색 > 소스`에서 바로 선택하는 방식입니다.

## 공식 참고

- Mihon: https://github.com/mihonapp/mihon
- Mihon 확장 API (`tachiyomix`): https://github.com/mihonapp/tachiyomix
- BookOrbit: https://github.com/bookorbit/bookorbit

저작권 고지/라이선스는 `docs/REFERENCES.md` 참고. 복사한 BookOrbit/Keiyoushi 구현 소스는 이 ZIP에 포함되지 않습니다.
