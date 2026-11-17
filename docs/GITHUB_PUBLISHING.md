# GitHub Release와 Pages 배포

소스 저장소: <https://github.com/10000Je/mihon-bookorbit-extension>

APK Release: <https://github.com/10000Je/mihon-bookorbit-extension/releases>

Mihon에 추가할 주소: <https://10000je.github.io/mihon-bookorbit-extension/repo.json>

## 공식 스토어 계약

기준은 공식 `mihonapp/mihon` **v0.21.3**, 정확한 커밋 **`feadad5e1e2bf638be6e2ca317d11657b09d4596`**이다. 기존 확장 저장소나 Keiyoushi 확장의 코드는 참조하지 않았다.

- [NetworkExtensionStore.kt](https://github.com/mihonapp/mihon/blob/feadad5e1e2bf638be6e2ca317d11657b09d4596/data/src/main/java/mihon/data/extension/model/NetworkExtensionStore.kt): `name`, `badgeLabel`, `signingKey`, `contact`, `extensionList`, `extensionListUrl` 및 APK/소스 정보의 실제 필드.
- [ExtensionStoreService.kt](https://github.com/mihonapp/mihon/blob/feadad5e1e2bf638be6e2ca317d11657b09d4596/data/src/main/java/mihon/data/extension/service/ExtensionStoreService.kt): `{`로 시작하는 JSON 스토어를 해석하고, `extensionListUrl=null`이면 같은 파일의 `extensionList.extensions`를 사용한다.
- [NetworkLegacyExtensionRepo.kt](https://github.com/mihonapp/mihon/blob/feadad5e1e2bf638be6e2ca317d11657b09d4596/data/src/main/java/mihon/data/extension/model/NetworkLegacyExtensionRepo.kt): 구형 `meta` 형식은 별도 계약이다. 이 프로젝트는 현대 JSON 형식을 사용하며 구형 `index.min.json` 호환성을 주장하지 않는다.

`repo/repo.json`은 최초 릴리스의 사람이 확인 가능한 스냅샷이다. 실제 Pages 파일은 Release마다 서명 APK와 해당 태그의 코드에서 재생성된다. `index.json`은 동일한 `extensions` 객체의 독립 사본이다. APK는 Pages에 복제하지 않고 버전별 GitHub Release 자산의 고정 URL로 연결한다. 소스의 홈 URL에는 개인 서버를 포함하지 않는다.

## 서명과 배포 흐름

`scripts/generate-repo.py`는 실제 APK의 v2 서명과 `repo/signing-key.sha256`의 공개 인증서 지문을 비교한다. 다른 키·debug 키, 잘못된 패키지, 태그/버전 불일치는 거부한다. versionCode/versionName/API 레벨은 APK에서, 슬롯 ID/개수/언어는 해당 태그의 코드에서 읽는다. 실패하면 새 JSON을 작성하지 않는다.

`.github/workflows/pages.yml`은 Release 발행 또는 수동 실행 시 APK를 내려받아 다시 검증하고 `_site/`를 Pages에 배포한다. 서명 개인 키·비밀번호·BookOrbit 접속 정보는 GitHub에 업로드하지 않는다. Actions에는 기본 읽기 토큰과 Pages 배포 권한만 사용한다. 사용한 공식 GitHub Actions는 커밋 SHA로 고정했다.

GitHub 저장소의 Pages source는 **GitHub Actions** (`build_type=workflow`)로 설정한다. 최초 배포 후에는 GitHub Release 발행이 자동 갱신을 시작한다. Pages 작업이 실패하면 기존 배포가 유지되므로 Actions 결과를 확인한다.

## 다음 버전 발행

1. `app/build.gradle.kts`의 `versionCode`를 증가시키고 `versionName`을 변경한다. 호스트 API와 슬롯 ID를 보존한다.
2. 같은 개인 서명 키로 테스트와 release 빌드를 실행하고, APK를 새 버전명으로 `artifacts/`에 복사한다.
3. 아래 명령으로 JSON과 체크섬을 생성한다. 태그는 APK versionName 앞에 `v`를 붙인다.

```sh
ANDROID_HOME=/absolute/android-sdk \
  python3 scripts/generate-repo.py \
    --apk artifacts/bookorbit-independent-1.6.1-release.apk \
    --repository 10000Je/mihon-bookorbit-extension --tag v1.6.1
```

4. 릴리스 노트·검증 기록·생성한 `repo/` 스냅샷을 갱신하고 커밋·푸시한다. 새 태그를 해당 커밋에 만들고 푸시한다. 기존 태그나 릴리스 파일은 덮어쓰지 않는다.
5. GitHub Release에 **서명된 release APK 하나**, `repo/SHA256SUMS`, `repo/repo.json`을 첨부하여 발행한다. 다음은 최초 버전의 명령 예시다.

```sh
gh release create v1.6.1 --verify-tag \
  --title 'BookOrbit Independent 1.6.1' --notes-file docs/RELEASE_NOTES.md \
  artifacts/bookorbit-independent-1.6.1-release.apk repo/SHA256SUMS repo/repo.json
```

6. Pages Actions 성공과 공개 JSON/APK의 HTTP 응답·서명·체크섬을 확인한다. 수동 재배포는 `gh workflow run pages.yml -f release_tag=v1.6.1`이다. 오래된 태그로 수동 실행하면 스토어도 그 버전으로 돌아가므로 최신 발행 버전을 지정한다.

## 배포 전 재검증 (2026-11-18)

- 기존 4개 커밋, 고유 blob 62개에서 키/토큰/개인 주소 패턴과 민감 파일 경로를 검사했다. 발견한 URL 자격 증명은 거부 동작을 시험하는 `.example`의 합성 `user:pass` 한 건이다. 검사 패턴에서 실제 비밀은 발견하지 않았다. 모든 유형의 비밀 부재를 보장하는 전수 감사는 아니다.
- `:app:testDebugUnitTest --rerun :app:assembleDebug :app:assembleRelease :app:lintDebug`: 성공. 테스트 30개 중 29개 통과·실서버 설정 없는 선택적 시험 1개 skip. Lint 기존 결과는 Warning 11개·Hint 1개·Error 0개이다.
- 릴리스 APK는 기존 에뮬레이터 검증 산출물과 SHA-256이 일치한다: `2238cca74aa5e9b5ec01756ac08842a1ac1088a2e20fad8b272b9e3d38b9fc3d`.
- APK Manifest와 인증서·v2 서명을 재확인했다. 현재 `adb devices`에 연결된 기기는 없으며 이번 배포 과정에서는 Mihon 스토어 등록 UI를 직접 실행하지 않았다. 기존 독자/라이브러리 시험과 구분한다.
- 실제 release APK로 피드를 재생성해 결과의 동일성과 6개 불변 소스 ID를 확인했다. 실제 debug APK는 서명 불일치로, release APK에 잘못된 태그를 주면 버전 불일치로 거부됐으며 두 실패 모두 출력 파일을 만들지 않았다. Workflow YAML 파싱과 `git diff --check`도 통과했다.
