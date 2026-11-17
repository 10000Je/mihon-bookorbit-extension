# BookOrbit Independent 1.6.1

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
