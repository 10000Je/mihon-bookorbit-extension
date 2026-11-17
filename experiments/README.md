# 캐시 기반 SourceFactory 실험

프로덕션 APK에는 이 폴더의 코드가 포함되지 않는다. 별도 임시 패키지 `app.bookorbit.mihonprobe`로 공식 Mihon v0.21.3 / Android 11 에뮬레이터에서 실행했다. 기존 확장 소스는 사용하지 않는다.

`python3 scripts/build-registry-probe.py`는 저장소의 독립 코드를 임시 폴더로 복사해 프로브를 빌드한다. SDK/JDK 설정은 본 프로젝트와 같다. 임시 APK를 에뮬레이터에 설치하고 Mihon에서 신뢰 승인한다.

캐시 파일은 테스트 호스트의 `noBackupFilesDir/bookorbit-cached-registry-probe.json`이다. 파일이 없으면 Guide 하나, 있으면 `[{"id":1,"name":"A"},…]`의 Library마다 하나를 반환한다. 네트워크 호출은 없다. 실제 BookOrbit 테스트 서버의 GET libraries에서 얻은 3개 id/name 배열을 사용했다. 키·서버 주소는 이 파일에 넣지 않는다.

실행 결과:

1. 캐시 없음 → `BookOrbit Probe · Guide` 한 소스 표시.
2. 실행 중 캐시를 3개 라이브러리로 갱신하고 Extensions/Sources 탭 왕복 → Guide 그대로 유지.
3. Mihon 프로세스 완전 종료 후 시작 → Guide 대신 Verification A/B/C 세 소스 표시.
4. 테스트 APK를 제거하고 프로브 캐시 파일을 삭제했다. 프로덕션 6슬롯과 별도 소스 ID를 사용하여 기록을 섞지 않았다.

증거 이미지: `artifacts/mihon-probe-before-cache.png`, `mihon-probe-cache-without-restart.png`, `mihon-probe-after-restart.png`.

결론: 캐시 동적 생성은 **완전 재시작 후 가능**하다. 설정 변경만으로 즉시 재등록되는 API는 없다. 이번 산출물은 재등록을 필요로 하지 않는 6개 슬롯 방식을 선택했다. 앱 완전 재시작이 허용되는 별도 동적 모드를 향후 구현할 수 있으나 현재 배포 APK의 기능으로 표시하지 않는다.
