package com.theveloper.pixelplay.data.audio

import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Debug
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.provider.MediaStore
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theveloper.pixelplay.data.database.SourceType
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Test
import org.junit.runner.RunWith

private data class BlindCandidate(
    val uri: Uri,
    val modified: Long,
    val duration: Long,
    val size: Long,
    val artist: String,
    val albumId: Long,
    val genres: Set<String>
)

private data class ProfiledBlindCandidate(val candidate: BlindCandidate, val profile: AudioSimilarityProfile)

/** Synthetic DSP checks plus an explicitly gated, anonymized local listening study. */
@RunWith(AndroidJUnit4::class)
class LocalAudioAnalyzerDiagnosticTest {
    @Test
    fun analyzeGeneratedWavAndOptionalGeneratedCodecFixtures() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = File(context.cacheDir, "audio-similarity-fixture.wav")
        val related = File(context.cacheDir, "audio-similarity-related.wav")
        val adversarial = File(context.cacheDir, "audio-similarity-adversarial.wav")
        try {
            writeSyntheticWav(fixture, bpm = 120, amplitude = 0.85, tones = doubleArrayOf(110.0, 440.0, 2_200.0))
            writeSyntheticWav(related, bpm = 120, amplitude = 0.34, tones = doubleArrayOf(110.0, 440.0, 2_200.0))
            writeSyntheticWav(adversarial, bpm = 91, amplitude = 0.75, tones = doubleArrayOf(200.0, 800.0, 5_000.0))
            val analyzer = LocalAudioAnalyzer(context.contentResolver)
            val profile = analyzer.analyze(fixture)
            check(profile.version == AudioSimilarity.PROFILE_VERSION)
            check(profile.energy.size == AudioSimilarity.ENERGY_BINS)
            check(profile.timbre.size == AudioSimilarity.TIMBRE_BANDS)
            check(profile.energy.all { it.isFinite() } && profile.timbre.all { it.isFinite() })
            val repeat = analyzer.analyze(fixture)
            check((AudioSimilarity.compare(profile, repeat)?.score ?: 0f) > 0.999f)
            val relatedScore = AudioSimilarity.compare(profile, analyzer.analyze(related))?.score ?: 0f
            val adversarialScore = AudioSimilarity.compare(profile, analyzer.analyze(adversarial))?.score ?: 0f
            check(relatedScore > adversarialScore + 0.1f) { "Synthetic related track did not outrank adversary" }

            val args = InstrumentationRegistry.getArguments()
            val fixtureUris = args.getString("audioSimilarityFixtureUris").orEmpty()
                .split('|').map(String::trim).filter(String::isNotEmpty)
            val codecOutcomes = mutableListOf<String>()
            for (rawUri in fixtureUris) {
                val uri = Uri.parse(rawUri)
                val outcome = try {
                    val decoded = analyzer.analyze(uri)
                    if (decoded.energy.all { it.isFinite() }) "decoded" else "invalid_features"
                } catch (error: Exception) {
                    "unsupported_${error.javaClass.simpleName}"
                }
                codecOutcomes += outcome
            }
            Log.i(TAG, "synthetic_algorithm_fixture=wav profile=ok order_check=passed human_evaluation=false duration_ms=${profile.durationMs} optional_fixture_count=${fixtureUris.size} optional_outcomes=${codecOutcomes.joinToString(",")}")
            if (args.getString("audioSimilarityScanLocal").equals("true", ignoreCase = true)) {
                scanLocalMusic(context, args.getString("audioSimilarityMaxTracks")?.toIntOrNull())
            }
            if (args.getString("audioSimilarityBlindEval").equals("true", ignoreCase = true)) {
                prepareBlindEvaluation(
                    context,
                    args.getString("audioSimilarityMaxTracks")?.toIntOrNull() ?: DEFAULT_BLIND_SAMPLE_SIZE,
                    args.getString("audioSimilarityBlindSeed")?.toLongOrNull() ?: System.nanoTime()
                )
            }
        } finally {
            fixture.delete()
            related.delete()
            adversarial.delete()
        }
    }

    private fun scanLocalMusic(context: Context, requestedMaxTracks: Int?) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "local_scan=skipped reason=audio_permission_missing")
            return
        }
        val index = LocalAudioSimilarityIndex(LocalAudioAnalyzer(context.contentResolver))
        val sampling = AtomicBoolean(true)
        val initialPssKb = Debug.getPss().toLong()
        val peakPssKb = java.util.concurrent.atomic.AtomicLong(initialPssKb)
        val sampler = Thread {
            while (sampling.get()) {
                val pss = Debug.getPss().toLong()
                peakPssKb.accumulateAndGet(pss) { current, sample -> maxOf(current, sample) }
                try { Thread.sleep(MEMORY_SAMPLE_INTERVAL_MS) } catch (_: InterruptedException) { break }
            }
        }.apply { name = "AudioSimilarityPssSampler"; isDaemon = true; start() }
        val cpuStartNs = Debug.threadCpuTimeNanos()
        val wallStartNs = System.nanoTime()
        data class MimeStats(
            var attempted: Int = 0,
            var decoded: Int = 0,
            var failures: Int = 0,
            var cacheHits: Int = 0,
            val latencyMs: MutableList<Long> = mutableListOf()
        )
        val stats = sortedMapOf<String, MimeStats>()
        var total = 0
        val maxTracks = requestedMaxTracks?.takeIf { it > 0 } ?: Int.MAX_VALUE
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ?"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf("1"),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            while (total < maxTracks && cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn))
                val mime = cursor.getString(mimeColumn)?.takeIf { it.isNotBlank() } ?: "unknown"
                val mimeStats = stats.getOrPut(mime) { MimeStats() }
                mimeStats.attempted++
                total++
                val modified = cursor.getLong(modifiedColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val result = index.profile(uri, modified, duration, size)
                if (result == null) {
                    mimeStats.failures++
                } else {
                    mimeStats.decoded++
                    mimeStats.latencyMs += result.analysisDurationMs
                    val cached = index.profile(uri, modified, duration, size)
                    if (cached?.cacheHit == true) mimeStats.cacheHits++ else mimeStats.failures++
                }
                if (total % PROGRESS_INTERVAL_TRACKS == 0) {
                    Log.i(TAG, "local_scan_progress processed=$total")
                }
            }
        }
        sampling.set(false)
        sampler.interrupt()
        try { sampler.join(1_500L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        val wallMs = (System.nanoTime() - wallStartNs) / 1_000_000L
        val cpuMs = (Debug.threadCpuTimeNanos() - cpuStartNs) / 1_000_000L
        val finalPssKb = Debug.getPss().toLong()
        peakPssKb.accumulateAndGet(finalPssKb) { current, sample -> maxOf(current, sample) }
        val summaries = stats.map { (mime, value) ->
            val sorted = value.latencyMs.sorted()
            val median = sorted.getOrNull(sorted.size / 2) ?: 0L
            val p95 = sorted.getOrNull(((sorted.size - 1) * 95 / 100).coerceAtLeast(0)) ?: 0L
            "$mime:${value.decoded}/${value.attempted},fail=${value.failures},cache=${value.cacheHits},p50_ms=$median,p95_ms=$p95"
        }
        Log.i(TAG, "local_scan=complete total=$total wall_ms=$wallMs caller_cpu_ms=$cpuMs pss_initial_kb=$initialPssKb pss_peak_sampled_kb=${peakPssKb.get()} pss_final_kb=$finalPssKb mime_coverage=${summaries.joinToString(";")}")
    }

    /** Explicitly opt-in real-file paired listening experiment. No per-track result reaches logs. */
    private fun prepareBlindEvaluation(context: Context, requestedMaxTracks: Int, seed: Long) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "audio_blind=skipped reason=audio_permission_missing")
            return
        }
        val sessionFile = java.io.File(context.cacheDir, BlindAudioEvaluationActivity.SESSION_FILE_NAME)
        sessionFile.delete()
        val maximum = if (requestedMaxTracks <= 0) Int.MAX_VALUE else requestedMaxTracks.coerceAtLeast(MIN_BLIND_SAMPLE_SIZE)
        val allCandidates = mutableListOf<BlindCandidate>()
        val genresBySong = readGenreMap(context)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} = ?",
            arrayOf("1"),
            "${MediaStore.Audio.Media._ID} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durationColumn)
                if (duration <= 0L) continue
                allCandidates += BlindCandidate(
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)),
                    modified = cursor.getLong(modifiedColumn),
                    duration = duration,
                    size = cursor.getLong(sizeColumn),
                    artist = cursor.getString(artistColumn)?.trim().orEmpty(),
                    albumId = cursor.getLong(albumIdColumn),
                    genres = genresBySong[cursor.getLong(idColumn)].orEmpty()
                )
            }
        }
        val random = Random((seed xor (seed ushr 32)).toInt())
        val favorites = readFavoriteUris(context)
        val favoritesFirst = allCandidates.sortedByDescending { it.uri.toString() in favorites }
        val selected = if (maximum >= favoritesFirst.size) favoritesFirst else {
            val favoriteCandidates = favoritesFirst.filter { it.uri.toString() in favorites }.shuffled(random).take(maximum)
            val remainingCount = (maximum - favoriteCandidates.size).coerceAtLeast(0)
            favoriteCandidates + favoritesFirst.filterNot { it.uri.toString() in favorites }.shuffled(random).take(remainingCount)
        }
        val index = LocalAudioSimilarityIndex(LocalAudioAnalyzer(context.contentResolver))
        val profiled = mutableListOf<ProfiledBlindCandidate>()
        val latencies = mutableListOf<Long>()
        val initialPssKb = Debug.getPss().toLong()
        val peakPssKb = java.util.concurrent.atomic.AtomicLong(initialPssKb)
        val sampling = AtomicBoolean(true)
        val sampler = Thread {
            while (sampling.get()) {
                val pss = Debug.getPss().toLong()
                peakPssKb.accumulateAndGet(pss) { current, sample -> maxOf(current, sample) }
                try { Thread.sleep(MEMORY_SAMPLE_INTERVAL_MS) } catch (_: InterruptedException) { break }
            }
        }.apply { name = "AudioBlindPssSampler"; isDaemon = true; start() }
        val startNs = System.nanoTime()
        val cpuStartNs = Debug.threadCpuTimeNanos()
        for ((position, candidate) in selected.withIndex()) {
            val profile = index.profile(candidate.uri, candidate.modified, candidate.duration, candidate.size)
            if (profile != null) {
                profiled += ProfiledBlindCandidate(candidate, profile.profile)
                latencies += profile.analysisDurationMs
            }
            if ((position + 1) % PROGRESS_INTERVAL_TRACKS == 0) {
                Log.i(TAG, "audio_blind_progress processed=${position + 1}")
            }
        }

        data class Trial(val seed: Uri, val panelA: List<String>, val panelB: List<String>, val acousticOnA: Boolean)
        val trials = mutableListOf<Trial>()
        var weakMetadata = 0
        var overlappingCandidateCount = 0
        val referencePool = profiled.shuffled(random).sortedByDescending { it.candidate.uri.toString() in favorites }
        for (seedTrack in referencePool) {
            if (trials.size == BlindAudioEvaluationActivity.REQUIRED_REFERENCES) break
            val candidates = profiled.filter { it.candidate.uri != seedTrack.candidate.uri }
            val rankedAcoustic = candidates.mapNotNull { candidate ->
                AudioSimilarity.compare(seedTrack.profile, candidate.profile)?.let { candidate to it.score }
            }.sortedByDescending { it.second }.take(BlindAudioEvaluationActivity.CANDIDATES_PER_PANEL).map { it.first }
            val rankedMetadata = candidates.sortedWith(
                compareByDescending<ProfiledBlindCandidate> { metadataSignalCount(seedTrack.candidate, it.candidate) }
                    .thenByDescending { seedTrack.candidate.albumId > 0 && seedTrack.candidate.albumId == it.candidate.albumId }
            ).take(BlindAudioEvaluationActivity.CANDIDATES_PER_PANEL)
            val bestMetadataSignal = rankedMetadata.maxOfOrNull { metadataSignalCount(seedTrack.candidate, it.candidate) } ?: 0
            if (bestMetadataSignal == 0 || rankedAcoustic.size < BlindAudioEvaluationActivity.CANDIDATES_PER_PANEL ||
                rankedMetadata.size < BlindAudioEvaluationActivity.CANDIDATES_PER_PANEL) {
                weakMetadata++
                continue
            }
            overlappingCandidateCount += rankedAcoustic.count { acoustic -> rankedMetadata.any { it.candidate.uri == acoustic.candidate.uri } }
            val acousticOnA = random.nextBoolean()
            val shuffledAcoustic = rankedAcoustic.shuffled(random)
            val shuffledMetadata = rankedMetadata.shuffled(random)
            trials += Trial(
                seed = seedTrack.candidate.uri,
                panelA = (if (acousticOnA) shuffledAcoustic else shuffledMetadata).map { it.candidate.uri.toString() },
                panelB = (if (acousticOnA) shuffledMetadata else shuffledAcoustic).map { it.candidate.uri.toString() },
                acousticOnA = acousticOnA
            )
        }
        val randomizedIndices = trials.indices.shuffled(random)
        sampling.set(false)
        sampler.interrupt()
        try { sampler.join(1_500L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        val sortedLatencies = latencies.sorted()
        val medianMs = sortedLatencies.getOrNull(sortedLatencies.size / 2) ?: 0L
        val p95Ms = sortedLatencies.getOrNull(((sortedLatencies.size - 1) * 95 / 100).coerceAtLeast(0)) ?: 0L
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
        val cpuMs = (Debug.threadCpuTimeNanos() - cpuStartNs) / 1_000_000L
        val finalPssKb = Debug.getPss().toLong()
        peakPssKb.accumulateAndGet(finalPssKb) { current, sample -> maxOf(current, sample) }
        val genreCoverage = profiled.count { it.candidate.genres.isNotEmpty() }
        val favoriteCoverage = trials.count { it.seed.toString() in favorites }
        val referenceSelection = if (favoriteCoverage > 0) "favorites_prioritized" else "seeded_random_approximation"
        Log.i(TAG, "audio_blind_plan prepared=${trials.size} reference_pool=${referencePool.size} profiled=${profiled.size}/${selected.size} baseline=artist_genre_album_tiebreak skipped_weak_metadata=$weakMetadata overlapping_top5=$overlappingCandidateCount genre_coverage=$genreCoverage/${profiled.size} favorite_references=$favoriteCoverage/${trials.size} reference_selection=$referenceSelection sample_seed=$seed elapsed_ms=$elapsedMs caller_cpu_ms=$cpuMs pss_initial_kb=$initialPssKb pss_peak_sampled_kb=${peakPssKb.get()} pss_final_kb=$finalPssKb p50_ms=$medianMs p95_ms=$p95Ms")
        if (trials.size != BlindAudioEvaluationActivity.REQUIRED_REFERENCES) {
            sessionFile.delete()
            return
        }

        val orderedTrials = randomizedIndices.map { trials[it] }
        val session = JSONObject()
            .put(BlindAudioEvaluationActivity.KEY_SEEDS, JSONArray().apply { orderedTrials.forEach { put(it.seed.toString()) } })
            .put(BlindAudioEvaluationActivity.KEY_A, JSONArray().apply { orderedTrials.forEach { trial -> trial.panelA.forEach { put(it) } } })
            .put(BlindAudioEvaluationActivity.KEY_B, JSONArray().apply { orderedTrials.forEach { trial -> trial.panelB.forEach { put(it) } } })
            .put(BlindAudioEvaluationActivity.KEY_ACOUSTIC_ON_A, JSONArray().apply { orderedTrials.forEach { put(it.acousticOnA) } })
        sessionFile.writeText(session.toString())
    }

    private fun metadataSignalCount(seedTrack: BlindCandidate, candidateTrack: BlindCandidate): Int {
        val artistMatch = isKnownArtist(seedTrack.artist) && isKnownArtist(candidateTrack.artist) &&
            seedTrack.artist.equals(candidateTrack.artist, ignoreCase = true)
        val genreMatch = seedTrack.genres.any { it in candidateTrack.genres }
        return (if (artistMatch) 1 else 0) + (if (genreMatch) 1 else 0)
    }

    private fun isKnownArtist(artist: String): Boolean {
        if (artist.isBlank()) return false
        return when (artist.trim().lowercase()) {
            "unknown", "<unknown>", "unknown artist", "artista desconocido" -> false
            else -> true
        }
    }

    private fun readGenreMap(context: Context): Map<Long, Set<String>> {
        val bySong = mutableMapOf<Long, MutableSet<String>>()
        try {
            val genresUri = MediaStore.Audio.Genres.getContentUri("external")
            context.contentResolver.query(
                genresUri,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null, null
            )?.use { genres ->
                val idColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameColumn = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genres.moveToNext()) {
                    val genreId = genres.getLong(idColumn)
                    val genreName = genres.getString(nameColumn)?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null, null, null
                    )?.use { members ->
                        val audioIdColumn = members.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (members.moveToNext()) {
                            bySong.getOrPut(members.getLong(audioIdColumn)) { mutableSetOf() }.add(genreName)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return bySong.mapValues { it.value.toSet() }
    }

    private fun readFavoriteUris(context: Context): Set<String> {
        val databaseFile = context.getDatabasePath("pixelplay_database")
        if (!databaseFile.isFile) return emptySet()
        return try {
            SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery(
                    """
                    SELECT songs.content_uri_string FROM songs
                    INNER JOIN favorites ON favorites.songId = songs.id
                    WHERE songs.source_type = ? AND favorites.isFavorite = 1
                    """.trimIndent(),
                    arrayOf(SourceType.LOCAL.toString())
                ).use { cursor ->
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun writeSyntheticWav(file: File, bpm: Int, amplitude: Double, tones: DoubleArray) {
        val sampleRate = 22_050
        val seconds = 20
        val sampleCount = sampleRate * seconds
        val pcmBytes = sampleCount * 2
        FileOutputStream(file).use { stream ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII)); header.putInt(36 + pcmBytes)
            header.put("WAVE".toByteArray(Charsets.US_ASCII)); header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16); header.putShort(1); header.putShort(1); header.putInt(sampleRate)
            header.putInt(sampleRate * 2); header.putShort(2); header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII)); header.putInt(pcmBytes)
            stream.write(header.array())
            val buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until sampleCount) {
                val time = i.toDouble() / sampleRate
                val beatPhase = time % (60.0 / bpm)
                val envelope = if (beatPhase < 0.035) 0.85 else 0.15
                var toneSum = 0.0
                for (toneIndex in tones.indices) {
                    toneSum += (0.55 / (toneIndex + 1)) * sin(2 * PI * tones[toneIndex] * time)
                }
                val sample = (amplitude * envelope * toneSum)
                    .coerceIn(-1.0, 1.0)
                if (buffer.remaining() < 2) { stream.write(buffer.array(), 0, buffer.position()); buffer.clear() }
                buffer.putShort((sample * Short.MAX_VALUE).toInt().toShort())
            }
            if (buffer.position() > 0) stream.write(buffer.array(), 0, buffer.position())
        }
    }

    companion object {
        private const val TAG = "AudioSimilarityDiag"
        private const val DEFAULT_BLIND_SAMPLE_SIZE = 12
        private const val MIN_BLIND_SAMPLE_SIZE = 10
        private const val MEMORY_SAMPLE_INTERVAL_MS = 1_000L
        private const val PROGRESS_INTERVAL_TRACKS = 4
    }
}
