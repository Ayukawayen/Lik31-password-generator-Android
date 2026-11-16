package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ayukawayen.lik31.R
import net.ayukawayen.lik31.core.Lik31
import kotlin.math.roundToInt

/**
 * Password length, 6..75. The slider is for coarse changes; the − / + buttons
 * step by one, since 70 values are too fine for a phone-width slider.
 */
@Composable
fun LengthSlider(
    length: Int,
    weak: Boolean,
    onLengthChange: (Int) -> Unit,
) {
    val decreaseLabel = stringResource(R.string.decrease_length)
    val increaseLabel = stringResource(R.string.increase_length)
    // Short lengths are flagged by colour only; the layout does not change.
    val tone = if (weak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_width),
            contentDescription = stringResource(if (weak) R.string.length_weak else R.string.length),
            tint = tone,
        )
        IconButton(
            onClick = { onLengthChange(length - 1) },
            enabled = length > Lik31.MIN_LENGTH,
            modifier = Modifier.semantics { contentDescription = decreaseLabel },
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Slider(
            value = length.toFloat(),
            onValueChange = { onLengthChange(it.roundToInt()) },
            modifier = Modifier.weight(1f),
            valueRange = Lik31.MIN_LENGTH.toFloat()..Lik31.MAX_LENGTH.toFloat(),
        )
        IconButton(
            onClick = { onLengthChange(length + 1) },
            enabled = length < Lik31.MAX_LENGTH,
            modifier = Modifier.semantics { contentDescription = increaseLabel },
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = length.toString(),
            modifier = Modifier.widthIn(min = 28.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
            color = if (weak) tone else Color.Unspecified,
            textAlign = TextAlign.End,
        )
    }
}
