# BookOrbit 1.6.3

- 앱/확장 이름을 **BookOrbit**, 소스 이름을 **BookOrbit I / II / III**로 변경했습니다.
- **기본 라이브러리 선택 / 적용**을 제거했습니다. 서버 URL과 API 키를 입력하고 **연결 확인 / 라이브러리 재검색**에 성공하면 전체 라이브러리가 바로 적용됩니다.
- **인기**는 접근 가능한 모든 라이브러리의 만화를 표시합니다. **필터 → 라이브러리**에서 전체 또는 특정 라이브러리를 선택합니다.
- 라이브러리 경계를 넘는 페이지에서 작품을 누락하거나 다른 라이브러리의 같은 시리즈 ID를 합치지 않습니다. 저장 작품·챕터·이미지는 원래 연결을 유지합니다.
- 로컬 JVM 단위 테스트 **48개**, Debug/Release 빌드 및 정적 서명 검사를 통과했습니다. 사용자가 동작을 확인했습니다. 에뮬레이터·기기·실서버 자동 재검증은 수행하지 않았습니다.
- [1.6.3 Release](https://github.com/10000Je/mihon-bookorbit-extension/releases/tag/v1.6.3)와 기존 Mihon 스토어 주소로 배포했습니다. 로컬 설치용 파일: `artifacts/bookorbit-1.6.3-release.apk`, 체크섬: `artifacts/SHA256SUMS-1.6.3`. 기존 Release와 같은 인증서입니다.

아래는 이전 버전의 기록입니다.

# BookOrbit Independent 1.6.2 — 로컬 APK 빌드, 공개 미배포

- 고정 소스를 6개에서 3개로 줄이며 슬롯 1~3의 기존 ID를 유지합니다.
- 소스의 인기 / 필터 버튼을 제공하고 필터에서 라이브러리를 선택합니다. 인기는 설정에서 적용한 기본 라이브러리를 표시합니다.
- 필터·기본 라이브러리 변경 후에도 저장한 작품·챕터·이미지를 원래 라이브러리로 연결합니다.
- 소스 설정에서 연결 확인 후 기본 라이브러리를 적용하고 소스 화면에 재진입하면 필터 목록이 갱신됩니다.
- 슬롯 4~6의 기존 작품은 자동 이관하지 않습니다. 서버·계정 전환과 초기화의 기록 보호 정책은 유지합니다.
- 기능 테스트는 로컬 JVM 단위 테스트 38개를 통과했습니다. 후속 요청으로 Debug/Release 빌드 및 정적 Manifest·서명 검사도 통과했습니다. 기기·실서버 재검증 및 공개 배포는 수행하지 않았습니다.
- 설치용 파일: `artifacts/bookorbit-independent-1.6.2-release.apk`. 체크섬: `artifacts/SHA256SUMS-1.6.2`. 기존 Release와 같은 인증서로 서명했습니다.

# BookOrbit Independent 1.6.1 — 기존 배포 기록

공식 BookOrbit 서버 API와 Mihon/tachiyomix 계약을 바탕으로 독립 구현한 비공식 확장입니다. Android 패키지는 `app.bookorbit.mihonext`입니다.

- 6개 고정 슬롯을 각각 다른 BookOrbit 라이브러리에 연결합니다.
- CBZ·CBR·CB7만 읽고 EPUB/PDF 전용·빈 작품은 목록과 검색에서 제외합니다.
- 읽기 전용 API 키, 인증된 표지와 페이지, 암호화한 설정 저장소를 사용합니다.
- 라이브러리 이름 변경 시 작품 식별자를 보존하고, 다른 서버·계정·라이브러리로 재매핑할 때 잘못된 기록 연결을 거부합니다.

## 설치

Android 8.0 이상, tachiyomix 1.6을 지원하는 Mihon이 필요합니다. 공식 Mihon v0.21.3 / Android 11 에뮬레이터 / BookOrbit 3.8.0에서 검증했습니다.

Mihon 확장 스토어에 다음 주소를 추가하거나 아래 APK를 직접 설치하세요.

```text
https://10000je.github.io/mihon-bookorbit-extension/repo.json
```

설치 파일은 `bookorbit-independent-1.6.1-release.apk`입니다. `SHA256SUMS`를 함께 제공합니다. 서명 인증서 SHA-256:

```text
bf3c650b87f278df0f01ecb458345f7a1c231a5f7634a29a2274da63cf0b1b10
```

확장 설정에서 HTTPS 서버 URL과 BookOrbit 읽기 전용 API 키를 입력하고, 연결 확인 후 라이브러리를 선택하세요. 슬롯마다 반복할 수 있습니다. 기존 debug APK와 서명이 다르므로 서로 덮어쓸 수 없습니다.

## 검증과 제한

기존 실제 서버 시험은 30개 테스트 모두 통과했고, 실제 Mihon 에뮬레이터에서 세 형식의 페이지 읽기와 라이브러리 격리를 확인했습니다. 이번 배포 전 재실행에서는 29개 통과, 실서버 설정이 없어 선택적 테스트 1개 skip입니다. debug/release 빌드와 lint 작업이 성공했습니다. 자세한 범위는 `docs/VERIFICATION.md`를 참고하세요.

물리 단말과 BookOrbit 3.8.0 이외 버전은 검증하지 않았습니다. HTTP, 비밀번호/JWT/OIDC 로그인, 서버 읽기 진행률 동기화, 7개 이상 동시 라이브러리는 지원하지 않습니다. 라이브러리명 표시 갱신에 Mihon 재시작이 필요할 수 있습니다. Pages 스토어의 실제 앱 등록 시험은 별도 검증 대상입니다.
