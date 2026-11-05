package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.utils.HapticIntensity

@Composable
internal fun HapticIntensitySettings(
    intensity: HapticIntensity,
    enabled: Boolean,
    onSelect: (HapticIntensity) -> Unit
) {
    val feedback = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.settings_haptic_intensity), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            HapticIntensity.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = intensity == option,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, HapticIntensity.entries.size),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(when (option) {
                        HapticIntensity.LOW -> R.string.settings_haptic_low
                        HapticIntensity.MEDIUM -> R.string.settings_haptic_medium
                        HapticIntensity.HIGH -> R.string.settings_haptic_high
                    }))
                }
            }
        }
        TextButton(
            onClick = { feedback.performHapticFeedback(HapticFeedbackType.Confirm) },
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.settings_haptic_preview)) }
    }
}
