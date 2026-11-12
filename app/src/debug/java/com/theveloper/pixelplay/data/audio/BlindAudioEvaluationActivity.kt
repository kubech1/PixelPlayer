package com.theveloper.pixelplay.data.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.json.JSONObject

/** Debug-only, single-listener blind listening harness. It never displays or logs source metadata. */
class BlindAudioEvaluationActivity : ComponentActivity() {
    private lateinit var seedUris: ArrayList<String>
    private lateinit var panelA: ArrayList<String>
    private lateinit var panelB: ArrayList<String>
    private lateinit var acousticOnA: BooleanArray
    private lateinit var acousticYesCounts: IntArray
    private lateinit var metadataYesCounts: IntArray
    private lateinit var skippedReferences: BooleanArray
    private var referenceIndex = 0
    private var panelIndex = 0
    private var candidateIndex = 0
    private var heardReferenceIndex = -1
    private var candidateHeard = false
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var heading: TextView
    private lateinit var status: TextView
    private lateinit var candidateButton: Button
    private lateinit var referenceButton: Button
    private lateinit var stopButton: Button
    private lateinit var skipButton: Button
    private lateinit var answerButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionFile = java.io.File(cacheDir, SESSION_FILE_NAME)
        val session = try { JSONObject(sessionFile.readText()) } catch (_: Exception) { null }
        sessionFile.delete()
        if (session == null) { finish(); return }
        seedUris = session.getJSONArray(KEY_SEEDS).toStringArray()
        panelA = session.getJSONArray(KEY_A).toStringArray()
        panelB = session.getJSONArray(KEY_B).toStringArray()
        val acousticArray = session.getJSONArray(KEY_ACOUSTIC_ON_A)
        acousticOnA = BooleanArray(acousticArray.length()) { acousticArray.getBoolean(it) }
        val expected = seedUris.size * CANDIDATES_PER_PANEL
        if (seedUris.size != REQUIRED_REFERENCES || panelA.size != expected || panelB.size != expected || acousticOnA.size != seedUris.size) {
            finish()
            return
        }
        acousticYesCounts = IntArray(seedUris.size)
        metadataYesCounts = IntArray(seedUris.size)
        skippedReferences = BooleanArray(seedUris.size)
        buildContent()
        showCurrentCandidate()
    }

    private fun buildContent() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }
        heading = TextView(this).apply { textSize = 21f; gravity = Gravity.CENTER }
        content.addView(heading, matchWidth())
        val instructions = TextView(this).apply {
            text = "Cada referencia tiene dos listas ciegas de cinco candidatos: una se ordenó por audio y otra por artista/género (álbum desempata). El orden de las listas y candidatos se mezcla. Escucha la referencia una vez y cada candidato; puedes repetirlos. Marca si se parece. No se muestran datos ni método."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(18))
        }
        content.addView(instructions, matchWidth())
        candidateButton = button("Candidato 1") { play(currentCandidateUri(), candidateClip = true) }
        val playbackRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        referenceButton = button("Referencia") { play(seedUris[referenceIndex], candidateClip = false) }
        stopButton = button("Detener") { stopPlayer() }
        playbackRow.addView(referenceButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        playbackRow.addView(stopButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        content.addView(playbackRow, matchWidth())
        content.addView(candidateButton, matchWidth())
        status = TextView(this).apply { textSize = 14f; gravity = Gravity.CENTER; setPadding(0, dp(14), 0, dp(8)) }
        content.addView(status, matchWidth())
        answerButtons = listOf(
            button("Sí, se parece") { recordAnswer(similar = true) },
            button("No se parece") { recordAnswer(similar = false) }
        )
        answerButtons.forEach { content.addView(it, matchWidth()) }
        skipButton = button("Omitir referencia") { skipReference() }
        content.addView(skipButton, matchWidth())
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    private fun showCurrentCandidate() {
        stopPlayer()
        if (referenceIndex >= REQUIRED_REFERENCES) {
            showSummary()
            return
        }
        candidateHeard = false
        val panelLabel = if (panelIndex == 0) "A" else "B"
        heading.text = "Referencia ${referenceIndex + 1} de $REQUIRED_REFERENCES · Lista $panelLabel"
        candidateButton.text = "Candidato ${candidateIndex + 1} de $CANDIDATES_PER_PANEL"
        status.text = if (heardReferenceIndex == referenceIndex) {
            "Referencia escuchada. Escucha el candidato completo antes de responder."
        } else {
            "Escucha la referencia y el candidato completo antes de responder."
        }
        answerButtons.forEach { it.isEnabled = false }
        referenceButton.isEnabled = true
        candidateButton.isEnabled = true
        stopButton.isEnabled = true
        skipButton.isEnabled = true
    }

    private fun currentCandidateUri(): String =
        (if (panelIndex == 0) panelA else panelB)[referenceIndex * CANDIDATES_PER_PANEL + candidateIndex]

    private fun recordAnswer(similar: Boolean) {
        if (heardReferenceIndex != referenceIndex || !candidateHeard) return
        val acousticPanel = if (acousticOnA[referenceIndex]) 0 else 1
        val isAcousticPanel = panelIndex == acousticPanel
        if (similar && isAcousticPanel) acousticYesCounts[referenceIndex]++
        if (similar && !isAcousticPanel) metadataYesCounts[referenceIndex]++
        candidateIndex++
        if (candidateIndex == CANDIDATES_PER_PANEL) {
            candidateIndex = 0
            panelIndex++
            if (panelIndex == PANELS_PER_REFERENCE) {
                panelIndex = 0
                referenceIndex++
            }
        }
        showCurrentCandidate()
    }

    private fun skipReference() {
        skippedReferences[referenceIndex] = true
        referenceIndex++
        panelIndex = 0
        candidateIndex = 0
        showCurrentCandidate()
    }

    private fun showSummary() {
        stopPlayer()
        val validCount = skippedReferences.count { !it }
        val acousticPasses = skippedReferences.indices.count { !skippedReferences[it] && acousticYesCounts[it] >= MIN_SIMILAR_PER_PANEL }
        val metadataPasses = skippedReferences.indices.count { !skippedReferences[it] && metadataYesCounts[it] >= MIN_SIMILAR_PER_PANEL }
        val acousticTotal = acousticYesCounts.sum()
        val metadataTotal = metadataYesCounts.sum()
        val acousticMeetsThreshold = acousticPasses >= REQUIRED_PASS_REFERENCES
        val metadataMeetsThreshold = metadataPasses >= REQUIRED_PASS_REFERENCES
        heading.text = "Sesión terminada"
        status.text = if (validCount != REQUIRED_REFERENCES) {
            "Muestra incompleta: $validCount de $REQUIRED_REFERENCES referencias válidas. Repite la sesión para evaluar el umbral."
        } else {
            "Umbral: al menos $MIN_SIMILAR_PER_PANEL de $CANDIDATES_PER_PANEL parecidas en $REQUIRED_PASS_REFERENCES de $REQUIRED_REFERENCES referencias. Audio: $acousticPasses/$REQUIRED_REFERENCES (${if (acousticMeetsThreshold) "cumple" else "no cumple"}); metadatos: $metadataPasses/$REQUIRED_REFERENCES (${if (metadataMeetsThreshold) "cumple" else "no cumple"}). Marcas positivas: audio $acousticTotal/50; metadatos $metadataTotal/50. Una sola sesión no valida el resultado de forma general."
        }
        answerButtons.forEach { it.isEnabled = false }
        referenceButton.isEnabled = false
        candidateButton.isEnabled = false
        stopButton.isEnabled = false
        skipButton.isEnabled = false
    }

    private fun play(uriString: String, candidateClip: Boolean) {
        stopPlayer()
        val next = MediaPlayer()
        player = next
        next.setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build())
        next.setOnPreparedListener { prepared ->
            if (player !== prepared) { prepared.release(); return@setOnPreparedListener }
            val durationMs = prepared.duration.coerceAtLeast(0)
            if (durationMs > CLIP_LENGTH_MS) prepared.seekTo((durationMs * CLIP_START_FRACTION).toInt())
            prepared.start()
            status.text = "Fragmento reproduciéndose; termina automáticamente."
            handler.postDelayed({
                if (player === prepared) {
                    stopPlayer()
                    markHeard(candidateClip)
                }
            }, CLIP_LENGTH_MS.toLong())
        }
        next.setOnCompletionListener {
            if (player === it) {
                stopPlayer()
                markHeard(candidateClip)
            }
        }
        next.setOnErrorListener { failed, _, _ ->
            if (player === failed) {
                stopPlayer()
                status.text = "No se pudo reproducir este fragmento. Omite esta referencia si continúa fallando."
            }
            true
        }
        try {
            next.setDataSource(this, Uri.parse(uriString))
            next.prepareAsync()
            status.text = "Preparando fragmento…"
        } catch (_: Exception) {
            stopPlayer()
            status.text = "No se pudo abrir este fragmento. Omite esta referencia si continúa fallando."
        }
    }

    private fun markHeard(candidateClip: Boolean) {
        if (candidateClip) candidateHeard = true else heardReferenceIndex = referenceIndex
        if (heardReferenceIndex == referenceIndex && candidateHeard) {
            status.text = "Escuchados referencia y candidato. Marca si se parecen."
            answerButtons.forEach { it.isEnabled = true }
        } else {
            status.text = "Escucha la referencia y el candidato antes de responder."
        }
    }

    private fun stopPlayer() {
        handler.removeCallbacksAndMessages(null)
        player?.let { current ->
            try { if (current.isPlaying) current.stop() } catch (_: Exception) { }
            try { current.reset() } catch (_: Exception) { }
            current.release()
        }
        player = null
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun org.json.JSONArray.toStringArray(): ArrayList<String> =
        ArrayList<String>(length()).also { values -> for (index in 0 until length()) values += getString(index) }

    override fun onStop() {
        stopPlayer()
        super.onStop()
    }

    override fun onDestroy() {
        stopPlayer()
        super.onDestroy()
    }

    companion object {
        const val SESSION_FILE_NAME = "audio-blind-session.json"
        const val KEY_SEEDS = "seeds"
        const val KEY_A = "panel_a"
        const val KEY_B = "panel_b"
        const val KEY_ACOUSTIC_ON_A = "acoustic_on_a"
        const val CANDIDATES_PER_PANEL = 5
        const val REQUIRED_REFERENCES = 10
        private const val PANELS_PER_REFERENCE = 2
        private const val MIN_SIMILAR_PER_PANEL = 3
        private const val REQUIRED_PASS_REFERENCES = 8
        private const val CLIP_LENGTH_MS = 10_000
        private const val CLIP_START_FRACTION = 0.35f
    }
}
