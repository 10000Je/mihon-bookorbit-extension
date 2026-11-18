package app.bookorbit.mihonext.settings

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.BookOrbitApi
import app.bookorbit.mihonext.api.BookOrbitHttp
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.Identity
import kotlinx.coroutines.*

class SettingsScreen(private val slot: Int, private val invalidate: () -> Unit) {
    fun setup(screen: PreferenceScreen) {
        val context = screen.context
        fun action(title: String, summary: String, onClick: () -> Unit) {
            // Handle our own dialogs; no credentials ever enter the host PreferenceDataStore.
            screen.addPreference(Preference(context).apply {
                this.title = title
                this.summary = summary
                setOnPreferenceClickListener { protect(context, onClick); true }
            })
        }
        action("서버 URL", "HTTPS 주소. 설정 후 연결 확인을 실행하세요.") {
            edit(context, "서버 URL", SettingsStore.read(slot).draft.server, false) { raw ->
                val normalized = ServerUrl.parse(raw).toString()
                SettingsStore.update(slot) { it.copy(draft = it.draft.copy(server = normalized), discovery = null) }
            }
        }
        action("읽기 전용 API 키", "BookOrbit 설정 → API keys에서 생성. 입력값은 암호화하여 보관합니다.") {
            edit(context, "API 키 교체", "", true) { raw ->
                val key = raw.trim()
                if (!key.startsWith("bok_") || key.length != 53 || key.any { !it.isLetterOrDigit() && it != '_' && it != '-' }) {
                    throw ApiFailure(FailureKind.CONFIGURATION, "BookOrbit의 bok_로 시작하는 53자 API 키를 입력하세요.")
                }
                SettingsStore.update(slot) { it.copy(draft = it.draft.copy(apiKey = key), discovery = null) }
            }
        }
        action("연결 확인 / 라이브러리 재검색", "연결에 성공하면 접근 가능한 모든 라이브러리를 자동으로 적용합니다.") {
            connect(context)
        }
        action("현재 연결 정보", "소스는 3개입니다. 라이브러리를 갱신한 뒤에는 소스 화면에 다시 진입하세요.") {
            val state = SettingsStore.read(slot)
            AlertDialog.Builder(context).setTitle(Identity.sourceName(slot)).setMessage(
                state.active?.let { "접근 가능한 라이브러리: ${state.connections().size}개\n소스 ID: ${Identity.sourceId(slot)}\n\n인기: 모든 라이브러리의 만화\n필터: 라이브러리별 탐색\n최신: 지원 안 함\n\n필터를 바꿔도 저장한 작품은 원래 라이브러리에서 읽습니다. 서버나 계정을 바꾸면 기존 작품은 새 연결로 이관되지 않습니다. 원래 연결의 식별자는 최근 12개까지 복원합니다." }
                    ?: "서버 URL과 API 키를 입력한 뒤 연결 확인을 실행하세요.",
            ).setPositiveButton("확인", null).show()
        }
        action("이 소스 연결 초기화", "암호화 설정과 이전 연결 식별자를 삭제합니다. 기존 읽기 기록과의 연결도 해제됩니다.") {
            AlertDialog.Builder(context).setTitle("소스 연결 초기화")
                .setMessage("이 소스의 설정을 삭제합니다. Mihon의 기존 읽기 기록은 남지만 새 연결에서 자동 복원되지 않습니다. 계속할까요?")
                .setNegativeButton("취소", null).setPositiveButton("초기화") { _, _ ->
                    protect(context) { SettingsStore.erase(slot); invalidate(); toast(context, "연결 설정을 초기화했습니다.") }
                }.show()
        }
        action("정보 / 공식 참고", "BookOrbit 서버에 연결하는 비공식 Mihon 클라이언트입니다. 실제 서버 및 기기 검증 상태는 배포 문서를 확인하세요.") {
            AlertDialog.Builder(context).setTitle("BookOrbit")
                .setMessage("Mihon tachiyomix 1.6 API\n\n서버 API 참고:\nhttps://github.com/bookorbit/bookorbit\n\nMihon / TachiyomiX:\nhttps://github.com/mihonapp/mihon\nhttps://github.com/mihonapp/tachiyomix\n\nBookOrbit 서버 소스와 기존 확장 구현 코드는 포함하지 않습니다. API 호환성은 서버 버전에 따라 달라질 수 있습니다.")
                .setPositiveButton("확인", null).show()
        }
    }

    private fun connect(context: Context) {
        val draft = SettingsStore.read(slot).draft
        if (draft.server.isBlank() || draft.apiKey.isBlank()) throw ApiFailure.configuration()
        val progress = AlertDialog.Builder(context).setTitle("서버 연결 확인 중")
            .setMessage("계정과 라이브러리를 확인하고 있습니다.").setNegativeButton("취소", null).create()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val job = scope.launch {
            try {
                val discovery = withContext(Dispatchers.IO) {
                    val temporary = Connection(draft.server, draft.apiKey, 0, 1, "", "0".repeat(32))
                    val client = BookOrbitHttp.client { temporary }
                    val api = BookOrbitApi(temporary, client)
                    val principal = api.principal()
                    if (principal.id <= 0 || principal.authenticationMethod != "api_key") throw ApiFailure.protocol()
                    Discovery(draft, principal.id, api.libraries(), System.currentTimeMillis())
                }
                SettingsStore.update(slot) { state ->
                    if (state.draft != draft) throw ApiFailure(FailureKind.CONFIGURATION, "연결 설정이 변경되었습니다. 다시 확인하세요.")
                    state.applyDiscovery(discovery)
                }
                invalidate()
                toast(context, "연결 성공: 라이브러리 ${discovery.libraries.size}개를 적용했습니다. 소스 화면에 다시 진입하세요.")
            } catch (e: CancellationException) { throw e }
            catch (e: ApiFailure) { toast(context, e.message ?: "연결 실패") }
            catch (_: Exception) { toast(context, "연결 확인을 완료하지 못했습니다.") }
            finally { progress.dismiss(); scope.cancel() }
        }
        progress.setOnCancelListener { job.cancel() }
        progress.setButton(AlertDialog.BUTTON_NEGATIVE, "취소") { _, _ -> job.cancel() }
        progress.show()
    }

    private fun edit(context: Context, title: String, initial: String, secret: Boolean, save: (String) -> Unit) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or if (secret) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true); setText(initial)
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO
            isSaveEnabled = false
        }
        val dialog = AlertDialog.Builder(context).setTitle(title).setView(input)
            .setNegativeButton("취소", null).setPositiveButton("저장", null).create()
        if (secret) dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try { save(input.text.toString()); input.text.clear(); dialog.dismiss(); toast(context, "저장했습니다. 연결 확인을 실행하세요.") }
                catch (e: ApiFailure) { toast(context, e.message ?: "설정 오류") }
                catch (_: Exception) { toast(context, "설정을 저장하지 못했습니다.") }
            }
        }
        dialog.show()
    }

    private fun protect(context: Context, action: () -> Unit) {
        try { action() } catch (e: ApiFailure) { toast(context, e.message ?: "설정 오류") }
        catch (_: Exception) { toast(context, "설정 작업을 완료하지 못했습니다.") }
    }
    private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
