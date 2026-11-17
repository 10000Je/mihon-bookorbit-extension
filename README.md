# BookOrbit Independent for Mihon

Mihon에서 개인 BookOrbit 서버의 만화를 읽기 위한 비공식 Android 확장입니다. 최대 6개 라이브러리를 각각 별도 소스로 연결하며, CBZ·CBR·CB7 형식을 지원합니다.

Android 8.0 이상이 필요하며, Mihon 0.21.3과 BookOrbit 3.8.0에서 검증했습니다.

## 설치

1. Mihon의 **더보기 → 설정 → 탐색 → 확장 스토어**에 아래 주소를 추가합니다.

   ```text
   https://10000je.github.io/mihon-bookorbit-extension/repo.json
   ```

2. **탐색 → 확장**에서 `BookOrbit Independent`를 설치하고, 신뢰 승인이 표시되면 승인합니다. [Releases](https://github.com/10000Je/mihon-bookorbit-extension/releases/latest)에서 APK를 직접 설치할 수도 있습니다.

## 사용법

1. BookOrbit의 **Settings → You → API keys**에서 원하는 라이브러리에 **Read** 권한을 가진 API 키를 발급합니다.
2. Mihon의 **탐색 → 소스 → BookOrbit · 슬롯 1** 설정을 엽니다.
3. **HTTPS 서버 URL**과 **API 키**를 입력합니다. URL에 `/api/v1`은 붙이지 않습니다.
4. **연결 확인 / 라이브러리 재검색 → 라이브러리 선택 / 적용**을 진행합니다.
5. 소스의 **인기** 탭이나 검색으로 만화를 찾아 읽습니다. 다른 라이브러리는 슬롯 2~6에 연결합니다.

`인기`는 선택한 라이브러리의 전체 목록이며, `최신`은 지원하지 않습니다. 읽기 진행률은 Mihon에 저장됩니다.
