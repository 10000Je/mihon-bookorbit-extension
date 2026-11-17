package app.bookorbit.mihonext.api

import java.io.IOException

enum class FailureKind { AUTHENTICATION, PERMISSION, NOT_FOUND, RATE_LIMIT, SERVER, PROTOCOL, CONNECTION, TIMEOUT, CONFIGURATION }

class ApiFailure(val kind: FailureKind, message: String) : IOException(message) {
    companion object {
        fun http(code: Int): ApiFailure = when (code) {
            401 -> ApiFailure(FailureKind.AUTHENTICATION, "인증 실패(401): API 키의 만료·폐기·서버 설정을 확인하세요.")
            403 -> ApiFailure(FailureKind.PERMISSION, "접근 거부(403): 키의 라이브러리 및 읽기 권한을 확인하세요.")
            404 -> ApiFailure(FailureKind.NOT_FOUND, "대상을 찾을 수 없습니다(404). 삭제·접근 권한 또는 서버 API 버전을 확인하세요.")
            429 -> ApiFailure(FailureKind.RATE_LIMIT, "요청 제한(429): 잠시 후 다시 시도하세요.")
            in 500..599 -> ApiFailure(FailureKind.SERVER, "BookOrbit 서버 오류($code). 잠시 후 다시 시도하세요.")
            else -> ApiFailure(FailureKind.PROTOCOL, "지원하지 않는 서버 응답($code). API 호환성과 리다이렉트를 확인하세요.")
        }
        fun protocol() = ApiFailure(FailureKind.PROTOCOL, "BookOrbit API 응답 형식이 호환되지 않습니다.")
        fun configuration() = ApiFailure(FailureKind.CONFIGURATION, "소스 설정에서 서버와 API 키를 연결하고 라이브러리를 선택하세요.")
    }
}
