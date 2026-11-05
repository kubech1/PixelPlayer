package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.audio.AudioOutputSnapshot

@Composable
internal fun AudioRouteStatusCard(output: AudioOutputSnapshot) {
    val unknown = stringResource(R.string.audio_route_unknown)
    val yes = stringResource(R.string.audio_route_yes)
    val no = stringResource(R.string.audio_route_no)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.audio_route_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.audio_route_available, output.available.joinToString { it.label }.ifBlank { unknown }))
            Text(stringResource(R.string.audio_route_predicted, output.anticipated.joinToString { it.label }.ifBlank { unknown }))
            Text(stringResource(R.string.audio_route_prediction_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.audio_route_session, output.sessionId))
            val configuredSink = output.sink?.let {
                stringResource(R.string.audio_route_sink_value, it.sampleRate, sinkEncodingLabel(it.encoding), Integer.bitCount(it.channelMask))
            } ?: unknown
            Text(stringResource(R.string.audio_route_sink, configuredSink))
            Text(stringResource(R.string.audio_route_sink_capabilities, output.sinkCapabilities ?: unknown))
            Text(stringResource(R.string.audio_route_float_requested, if (output.floatRequested) yes else no))
            Text(stringResource(R.string.audio_route_offload_requested, if (output.offloadRequested) yes else no))
            Text(stringResource(R.string.audio_route_offload_configured, output.sink?.offload?.let { if (it) yes else no } ?: unknown))
            Text(stringResource(R.string.audio_route_offload_sleeping, output.sleepingForOffload?.let { if (it) yes else no } ?: unknown))
            Text(stringResource(R.string.audio_route_bluetooth_unknown), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.audio_route_format_query_note), style = MaterialTheme.typography.bodySmall)
            if (output.queryFailure != null) Text(stringResource(R.string.audio_route_query_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun sinkEncodingLabel(encoding: Int): String = when (encoding) {
    android.media.AudioFormat.ENCODING_PCM_8BIT -> "PCM 8-bit"
    android.media.AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
    android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
    android.media.AudioFormat.ENCODING_PCM_32BIT -> "PCM 32-bit"
    android.media.AudioFormat.ENCODING_PCM_FLOAT -> "PCM float"
    android.media.AudioFormat.ENCODING_MP3 -> "MP3"
    android.media.AudioFormat.ENCODING_AAC_LC -> "AAC LC"
    android.media.AudioFormat.ENCODING_AC3 -> "AC-3"
    android.media.AudioFormat.ENCODING_E_AC3 -> "E-AC-3"
    android.media.AudioFormat.ENCODING_DTS -> "DTS"
    android.media.AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
    android.media.AudioFormat.ENCODING_OPUS -> "Opus"
    else -> "Encoding $encoding"
}
