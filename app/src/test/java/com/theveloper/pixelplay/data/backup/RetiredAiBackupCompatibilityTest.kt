package com.theveloper.pixelplay.data.backup

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.theveloper.pixelplay.data.backup.format.BackupReader
import com.theveloper.pixelplay.data.backup.model.BackupSection
import com.theveloper.pixelplay.data.backup.model.BackupManifest
import com.theveloper.pixelplay.data.backup.model.BackupModuleInfo
import com.theveloper.pixelplay.data.backup.model.BackupValidationResult
import com.theveloper.pixelplay.data.backup.model.DeviceInfo
import com.theveloper.pixelplay.data.backup.model.Severity
import com.theveloper.pixelplay.data.backup.restore.RestorePlanner
import com.theveloper.pixelplay.data.backup.validation.ManifestValidator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class RetiredAiBackupCompatibilityTest {
    @Test fun `legacy activity module is warned and excluded while music modules stay selectable`() = runBlocking {
        val reader = mockk<BackupReader>()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://synthetic/legacy-backup"
        val info = BackupModuleInfo(checksum = "sha256:" + "0".repeat(64), entryCount = 1, sizeBytes = 32)
        val manifest = BackupManifest(schemaVersion = 3, appVersion = "fixture", appVersionCode = 1,
            createdAt = 1_700_000_000_000, deviceInfo = DeviceInfo(),
            modules = mapOf("ai_usage_logs" to info, "playlists" to info, "lyrics" to info, "global_settings" to info))
        coEvery { reader.readManifest(uri) } returns Result.success(manifest)
        val plan = RestorePlanner(reader, emptyMap()).buildRestorePlan(uri).getOrThrow()
        assertThat(plan.availableModules).containsExactly(BackupSection.PLAYLISTS, BackupSection.LYRICS, BackupSection.GLOBAL_SETTINGS)
        assertThat(plan.selectedModules).isEqualTo(plan.availableModules)
        val validation = ManifestValidator().validate(manifest) as BackupValidationResult.Invalid
        assertThat(validation.errors.any { it.module == "ai_usage_logs" && it.severity == Severity.WARNING }).isTrue()
        assertThat(validation.errors.any { it.severity == Severity.ERROR }).isFalse()
        assertThat(BackupManifest.CURRENT_SCHEMA_VERSION).isEqualTo(3)
    }
}
