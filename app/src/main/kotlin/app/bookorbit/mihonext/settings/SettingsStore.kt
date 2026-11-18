package app.bookorbit.mihonext.settings

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import app.bookorbit.mihonext.api.ApiFailure
import app.bookorbit.mihonext.api.BookOrbitApi
import app.bookorbit.mihonext.api.FailureKind
import app.bookorbit.mihonext.model.Identity
import kotlinx.serialization.encodeToString
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Runs under the host UID: noBackupFilesDir is essential; extension allowBackup alone is insufficient. */
object SettingsStore {
    private const val KEY_ALIAS = "app.bookorbit.mihonext.settings.v1"
    private val app: Application by lazy { Injekt.get<Application>() }
    private val memory = HashMap<Int, SlotState>()

    @Synchronized fun read(slot: Int): SlotState {
        require(slot in 1..Identity.SOURCE_COUNT)
        return memory.getOrPut(slot) {
            val file = file(slot)
            if (!file.baseFile.exists()) SlotState() else try {
                val bytes = file.readFully()
                require(bytes.size in 29..1_048_576)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.updateAAD("$KEY_ALIAS/$slot".toByteArray())
                BookOrbitApi.json.decodeFromString<SlotState>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
            } catch (_: Exception) {
                throw storageFailure()
            }
        }
    }

    @Synchronized fun update(slot: Int, change: (SlotState) -> SlotState): SlotState {
        val updated = change(read(slot))
        val file = file(slot)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.updateAAD("$KEY_ALIAS/$slot".toByteArray())
            val bytes = cipher.iv + cipher.doFinal(BookOrbitApi.json.encodeToString(updated).toByteArray())
            val stream = file.startWrite()
            try { stream.write(bytes); file.finishWrite(stream) } catch (e: Exception) { file.failWrite(stream); throw e }
        } catch (_: Exception) { throw storageFailure() }
        memory[slot] = updated
        return updated
    }

    @Synchronized fun erase(slot: Int) {
        require(slot in 1..Identity.SOURCE_COUNT)
        file(slot).delete()
        memory.remove(slot)
    }

    private fun file(slot: Int): AtomicFile = AtomicFile(File(app.noBackupFilesDir, "bookorbit-independent-slot-$slot.bin"))

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private fun storageFailure() = ApiFailure(FailureKind.CONFIGURATION,
        "보안 설정을 읽거나 저장할 수 없습니다. 설정에서 이 슬롯의 연결을 초기화한 후 다시 연결하세요.")
}
